package org.fossify.messages.helpers

import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.getUnreadCountsByThread

/**
 * Kid unread cue: report facts to PhonlyAPI. API + cron send Esper NOTIFY_DEVICE.
 * This process never draws an overlay, never uses AlarmManager, never holds the Esper key.
 */
object NotificationCue {
    const val PERMISSION = "co.phonly.permission.NOTIFICATION"
    const val ACTION_LOOKED = "co.phonly.intent.NOTIFICATION_LOOKED"
    const val ACTION_LEFT = "co.phonly.intent.NOTIFICATION_LEFT"
    const val PHONE_PACKAGE = "co.phonly.phone"

    private const val TAG = DeviceNotificationApi.TAG
    private const val PREFS = "phonly_notification_cue"
    private const val KEY_ALREADY_SENT = "alreadySent"
    private const val KEY_WAITING_SINCE = "waitingSince"
    private const val KEY_PILED_THIS_CYCLE = "piledThisCycle"
    private const val PILE_DEBOUNCE_MS = 8_000L
    private const val PILE_RETRY_MS = 15_000L

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pileRunnable = Runnable {
        val app = appContext ?: return@Runnable
        ensureBackgroundThread { flushPile(app) }
    }

    @Volatile private var started = false
    @Volatile private var appContext: Context? = null
    @Volatile private var messagesForeground = false
    @Volatile private var phoneLooking = false
    @Volatile private var alreadySent = false
    @Volatile private var waitingSince: String? = null
    @Volatile private var piledThisCycle = false
    @Volatile private var lookGeneration = 0
    @Volatile private var networkWatching = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onNetworkMaybeUp()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                onNetworkMaybeUp()
            }
        }
    }

    private val inboxObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            val app = appContext ?: return
            ensureBackgroundThread { onInboxChanged(app) }
        }
    }

    fun start(context: Context) {
        val app = context.applicationContext
        synchronized(lock) {
            if (started) return
            started = true
            appContext = app
            loadPrefs(app)
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> onMessagesForeground(app)
                    Lifecycle.Event.ON_STOP -> onMessagesBackground(app)
                    else -> Unit
                }
            }
        )
        try {
            app.contentResolver.registerContentObserver(
                Telephony.MmsSms.CONTENT_URI,
                true,
                inboxObserver
            )
        } catch (t: Throwable) {
            Log.w(TAG, "inbox observer not registered", t)
        }
        ensureBackgroundThread { onProcessStart(app) }
    }

    fun onInboxChanged(context: Context) {
        try {
            val app = context.applicationContext
            if (!hasUnread(app)) {
                cancelPileDebounce()
                postClear(app)
                return
            }
            if (isLooking()) {
                return
            }
            schedulePile(app)
        } catch (t: Throwable) {
            Log.w(TAG, "onInboxChanged failed", t)
        }
    }

    fun onPhoneLooked() {
        phoneLooking = true
        cancelPileDebounce()
        Log.i(TAG, "Phone LOOKED; hold pile")
    }

    fun onPhoneLeft(context: Context) {
        phoneLooking = false
        Log.i(TAG, "Phone LEFT")
        val app = context.applicationContext
        ensureBackgroundThread {
            if (hasUnread(app) && !isLooking()) {
                schedulePile(app)
            }
        }
    }

    private fun onMessagesForeground(context: Context) {
        messagesForeground = true
        phoneLooking = false
        lookGeneration += 1
        cancelPileDebounce()
        notifyPhone(context, ACTION_LOOKED)
        ensureBackgroundThread { postLooked(context) }
    }

    private fun onMessagesBackground(context: Context) {
        messagesForeground = false
        notifyPhone(context, ACTION_LEFT)
        ensureBackgroundThread {
            if (!hasUnread(context)) {
                postClear(context)
            } else if (!phoneLooking) {
                schedulePile(context)
            }
        }
    }

    private fun onProcessStart(context: Context) {
        val gen = lookGeneration
        val unread = hasUnread(context)
        val result = DeviceNotificationApi.get(context)
        if (gen != lookGeneration) {
            Log.i(TAG, "boot GET stale; looked won")
            return
        }
        if (result.unavailable) {
            Log.w(TAG, "GET unavailable; unread=$unread")
            if (unread && !isLooking()) {
                schedulePile(context)
            } else if (!unread) {
                postClear(context)
            }
            return
        }
        applyServer(context, result.state)
        if (!unread) {
            postClear(context)
            return
        }
        if (alreadySent) {
            Log.i(TAG, "boot: alreadySent; do nothing")
            return
        }
        if (waitingSince != null) {
            Log.i(TAG, "boot: already waiting")
            persist(context)
            return
        }
        schedulePile(context)
    }

    private fun isLooking(): Boolean {
        if (phoneLooking) return true
        if (messagesForeground) return true
        return try {
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        } catch (_: Throwable) {
            false
        }
    }

    private fun schedulePile(context: Context) {
        synchronized(lock) {
            if (isLooking()) return
            if (!hasUnread(context)) return
            // Do not skip on local alreadySent / piledThisCycle / waitingSince.
            // Those flags go stale after LOOKED (boot GET can re-apply the old
            // card). Extra POSTs are harmless: the API does not restart a wait.
            // Home is enough; Recents swipe-kill is not required.
            appContext = context.applicationContext
            mainHandler.removeCallbacks(pileRunnable)
            mainHandler.postDelayed(pileRunnable, PILE_DEBOUNCE_MS)
            Log.i(TAG, "pile debounced ${PILE_DEBOUNCE_MS}ms")
        }
    }

    private fun cancelPileDebounce() {
        mainHandler.removeCallbacks(pileRunnable)
        appContext?.let { unwatchNetwork(it) }
    }

    private fun onNetworkMaybeUp() {
        val app = appContext ?: return
        if (isLooking() || !hasUnread(app)) {
            return
        }
        Log.i(TAG, "network up; pile now")
        mainHandler.removeCallbacks(pileRunnable)
        mainHandler.post(pileRunnable)
    }

    private fun watchNetwork(context: Context) {
        if (networkWatching) {
            return
        }
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, networkCallback)
            networkWatching = true
        } catch (t: Throwable) {
            Log.w(TAG, "network watch ${t.message}")
        }
    }

    private fun unwatchNetwork(context: Context) {
        if (!networkWatching) {
            return
        }
        try {
            context.getSystemService(ConnectivityManager::class.java)
                ?.unregisterNetworkCallback(networkCallback)
        } catch (_: Throwable) {
        }
        networkWatching = false
    }

    private fun schedulePileRetry(context: Context) {
        synchronized(lock) {
            if (isLooking() || !hasUnread(context)) {
                unwatchNetwork(context)
                return
            }
            appContext = context.applicationContext
            watchNetwork(context)
            mainHandler.removeCallbacks(pileRunnable)
            mainHandler.postDelayed(pileRunnable, PILE_RETRY_MS)
            Log.i(TAG, "pile retry ${PILE_RETRY_MS}ms (until looked or POST)")
        }
    }

    private fun flushPile(context: Context) {
        synchronized(lock) {
            if (isLooking()) return
            if (!hasUnread(context)) return
        }
        val gen = lookGeneration
        val result = DeviceNotificationApi.pile(context)
        if (gen != lookGeneration) {
            Log.i(TAG, "PILE stale; looked won")
            cancelPileDebounce()
            if (!result.unavailable) {
                postLooked(context)
            }
            return
        }
        if (result.unavailable) {
            Log.w(TAG, "WOULD_PILE")
            if (result.retryable) {
                schedulePileRetry(context)
            }
        } else {
            Log.i(TAG, "POST pile")
            unwatchNetwork(context)
        }
        if (result.state != null) {
            applyServer(context, result.state)
        } else {
            persist(context)
        }
    }

    private fun postLooked(context: Context) {
        cancelPileDebounce()
        val result = DeviceNotificationApi.looked(context)
        if (result.unavailable) {
            Log.w(TAG, "WOULD_LOOKED")
        } else {
            Log.i(TAG, "POST looked")
        }
        alreadySent = false
        waitingSince = null
        piledThisCycle = false
        if (result.state != null) {
            applyServer(context, result.state)
        } else {
            persist(context)
        }
    }

    private fun postClear(context: Context) {
        if (!alreadySent && waitingSince == null && !piledThisCycle) {
            return
        }
        cancelPileDebounce()
        val result = DeviceNotificationApi.clear(context)
        if (result.unavailable) {
            Log.w(TAG, "WOULD_CLEAR")
        } else {
            Log.i(TAG, "POST clear")
        }
        alreadySent = false
        waitingSince = null
        piledThisCycle = false
        persist(context)
    }

    private fun applyServer(context: Context, state: DeviceNotificationApi.NotificationState?) {
        if (state == null) {
            alreadySent = false
            waitingSince = null
            piledThisCycle = false
        } else {
            alreadySent = state.alreadySent
            waitingSince = state.waitingSince
            piledThisCycle = state.alreadySent || state.waitingSince != null
        }
        persist(context)
    }

    private fun hasUnread(context: Context): Boolean {
        return try {
            context.getUnreadCountsByThread().values.any { it > 0 }
        } catch (t: Throwable) {
            Log.w(TAG, "unread query failed", t)
            false
        }
    }

    private fun notifyPhone(context: Context, action: String) {
        try {
            val intent = Intent(action).setPackage(PHONE_PACKAGE)
            context.sendBroadcast(intent, PERMISSION)
        } catch (t: Throwable) {
            Log.w(TAG, "broadcast $action failed", t)
        }
    }

    private fun loadPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        alreadySent = prefs.getBoolean(KEY_ALREADY_SENT, false)
        waitingSince = prefs.getString(KEY_WAITING_SINCE, null)
        piledThisCycle = prefs.getBoolean(KEY_PILED_THIS_CYCLE, false)
    }

    private fun persist(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ALREADY_SENT, alreadySent)
            .putString(KEY_WAITING_SINCE, waitingSince)
            .putBoolean(KEY_PILED_THIS_CYCLE, piledThisCycle)
            .apply()
    }
}
