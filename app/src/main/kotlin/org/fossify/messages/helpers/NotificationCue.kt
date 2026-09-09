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
import org.fossify.messages.services.DeviceNotificationPostService

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
    private const val KEY_CARD_STATUS = "cardStatus"
    private const val KEY_REMINDER_STARTED_AT = "reminderStartedAt"
    private const val KEY_CARD_SHOWN_AT = "cardShownAt"
    private const val KEY_QUIET_UNTIL_INCOMING = "quietUntilIncoming"
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
    @Volatile private var cardStatus = DeviceNotificationApi.CardStatus.IDLE
    @Volatile private var reminderStartedAt: String? = null
    @Volatile private var cardShownAt: String? = null
    @Volatile private var quietUntilIncoming = false
    @Volatile private var lookGeneration = 0
    @Volatile private var networkWatching = false
    @Volatile private var pileDebouncePending = false

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

    fun onIncomingUnread(context: Context) {
        val app = context.applicationContext
        if (isLooking()) {
            // Texts that arrive while Messages is in front must not arm a
            // reminder on Home — including after the inbox was emptied in
            // this same visit. Re-assert hold; do not release it.
            quietUntilIncoming = true
            persist(app)
            Log.i(TAG, "incoming; Messages in front, hold")
            onInboxChanged(app)
            return
        }
        quietUntilIncoming = false
        persist(app)
        Log.i(TAG, "incoming; hold released")
        onInboxChanged(app)
    }

    fun onInboxChanged(context: Context) {
        try {
            val app = context.applicationContext
            if (!hasUnread(app)) {
                quietUntilIncoming = false
                cancelPileDebounce()
                postClear(app)
                return
            }
            if (isLooking()) {
                return
            }
            if (quietUntilIncoming) {
                Log.i(TAG, "inbox tick; hold")
                return
            }
            schedulePile(app)
        } catch (t: Throwable) {
            Log.w(TAG, "onInboxChanged failed", t)
        }
    }

    fun onPhoneLooked() {
        Log.i(TAG, "Phone LOOKED ignored; Recents is not the inbox")
    }

    fun onPhoneLeft() {
        Log.i(TAG, "Phone LEFT ignored")
    }

    private fun onMessagesForeground(context: Context) {
        messagesForeground = true
        quietUntilIncoming = true
        lookGeneration += 1
        cancelPileDebounce()
        persist(context)
        notifyPhone(context, ACTION_LOOKED)
        ensureBackgroundThread { postLooked(context) }
    }

    private fun onMessagesBackground(context: Context) {
        messagesForeground = false
        notifyPhone(context, ACTION_LEFT)
        ensureBackgroundThread {
            if (!hasUnread(context)) {
                quietUntilIncoming = false
                postClear(context)
            } else if (quietUntilIncoming) {
                Log.i(TAG, "leave; hold, no pile")
                persist(context)
            } else {
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
            if (unread && !isLooking() && !quietUntilIncoming) {
                schedulePile(context)
            } else if (!unread) {
                postClear(context)
            } else if (quietUntilIncoming) {
                Log.i(TAG, "boot: looked, wait for incoming")
            }
            return
        }
        applyServer(context, result.state)
        if (!unread) {
            quietUntilIncoming = false
            postClear(context)
            return
        }
        if (cardStatus == DeviceNotificationApi.CardStatus.SHOWN) {
            Log.i(TAG, "boot: shown; do nothing")
            return
        }
        if (cardStatus == DeviceNotificationApi.CardStatus.WAITING) {
            Log.i(TAG, "boot: already waiting")
            persist(context)
            return
        }
        if (quietUntilIncoming) {
            Log.i(TAG, "boot: looked, wait for incoming")
            persist(context)
            return
        }
        schedulePile(context)
    }

    private fun isLooking(): Boolean {
        if (messagesForeground) return true
        return try {
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        } catch (_: Throwable) {
            false
        }
    }

    private fun schedulePile(context: Context) {
        val app = context.applicationContext
        synchronized(lock) {
            if (isLooking()) return
            if (quietUntilIncoming) {
                Log.i(TAG, "pile skipped; hold")
                return
            }
            if (!hasUnread(context)) return
            appContext = app
            if (pileDebouncePending) {
                Log.i(TAG, "pile debounce kept")
                return
            }
            pileDebouncePending = true
            mainHandler.postDelayed(pileRunnable, PILE_DEBOUNCE_MS)
        }
        startPostForeground(app)
        Log.i(TAG, "pile debounced ${PILE_DEBOUNCE_MS}ms")
    }

    private fun cancelPileDebounce() {
        synchronized(lock) {
            pileDebouncePending = false
            mainHandler.removeCallbacks(pileRunnable)
            appContext?.let { app ->
                unwatchNetwork(app)
                stopPostForeground(app)
            }
        }
    }

    private fun onNetworkMaybeUp() {
        val app = appContext ?: return
        if (isLooking() || quietUntilIncoming || !hasUnread(app)) {
            return
        }
        synchronized(lock) {
            if (pileDebouncePending) {
                Log.i(TAG, "network up; pile wait kept")
                return
            }
        }
        Log.i(TAG, "network up; pile debounce")
        schedulePile(app)
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
        val app = context.applicationContext
        synchronized(lock) {
            if (isLooking() || quietUntilIncoming || !hasUnread(context)) {
                unwatchNetwork(app)
                stopPostForeground(app)
                return
            }
            appContext = app
            watchNetwork(app)
            pileDebouncePending = true
            mainHandler.removeCallbacks(pileRunnable)
            mainHandler.postDelayed(pileRunnable, PILE_RETRY_MS)
        }
        startPostForeground(app)
        Log.i(TAG, "pile retry ${PILE_RETRY_MS}ms (until looked or POST)")
    }

    private fun flushPile(context: Context) {
        synchronized(lock) {
            pileDebouncePending = false
            if (isLooking() || quietUntilIncoming) {
                stopPostForeground(context)
                return
            }
            if (!hasUnread(context)) {
                stopPostForeground(context)
                return
            }
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
            } else {
                stopPostForeground(context)
            }
        } else {
            Log.i(TAG, "POST pile")
            unwatchNetwork(context)
            stopPostForeground(context)
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
        applyIdle()
        if (result.state != null) {
            applyServer(context, result.state)
        } else {
            persist(context)
        }
    }

    private fun postClear(context: Context) {
        quietUntilIncoming = false
        if (cardStatus == DeviceNotificationApi.CardStatus.IDLE) {
            persist(context)
            return
        }
        cancelPileDebounce()
        val result = DeviceNotificationApi.clear(context)
        if (result.unavailable) {
            Log.w(TAG, "WOULD_CLEAR")
        } else {
            Log.i(TAG, "POST clear")
        }
        applyIdle()
        persist(context)
    }

    private fun applyIdle() {
        cardStatus = DeviceNotificationApi.CardStatus.IDLE
        reminderStartedAt = null
    }

    private fun applyServer(context: Context, state: DeviceNotificationApi.NotificationState?) {
        if (state == null) {
            applyIdle()
            cardShownAt = null
        } else {
            cardStatus = state.cardStatus
            reminderStartedAt = if (state.cardStatus == DeviceNotificationApi.CardStatus.WAITING) {
                state.reminderStartedAt
            } else {
                null
            }
            cardShownAt = state.cardShownAt
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

    private fun startPostForeground(context: Context) {
        val app = context.applicationContext
        mainHandler.post { DeviceNotificationPostService.start(app) }
    }

    private fun stopPostForeground(context: Context) {
        val app = context.applicationContext
        mainHandler.post { DeviceNotificationPostService.stop(app) }
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
        cardStatus = DeviceNotificationApi.CardStatus.fromWire(prefs.getString(KEY_CARD_STATUS, null))
        reminderStartedAt = prefs.getString(KEY_REMINDER_STARTED_AT, null)
        cardShownAt = prefs.getString(KEY_CARD_SHOWN_AT, null)
        if (cardStatus != DeviceNotificationApi.CardStatus.WAITING) {
            reminderStartedAt = null
        }
        quietUntilIncoming = prefs.getBoolean(KEY_QUIET_UNTIL_INCOMING, false)
    }

    private fun persist(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_CARD_STATUS, cardStatus.wire)
            .putString(KEY_REMINDER_STARTED_AT, reminderStartedAt)
            .putString(KEY_CARD_SHOWN_AT, cardShownAt)
            .putBoolean(KEY_QUIET_UNTIL_INCOMING, quietUntilIncoming)
            .remove("alreadySent")
            .remove("waitingSince")
            .remove("piledThisCycle")
            .apply()
    }
}
