package org.fossify.messages.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.RestrictionsManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import org.fossify.messages.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.nio.charset.StandardCharsets

/**
 * Reports pile / looked / clear to PhonlyAPI. Never holds the Esper key.
 * Body is only `{ imei, source }` — no SMS text, no thread id.
 */
object DeviceNotificationApi {
    const val TAG = "PhonlyCue"
    const val SOURCE = "messages"

    enum class CardStatus {
        IDLE, WAITING, SHOWN;

        val wire: String
            get() = when (this) {
                IDLE -> "idle"
                WAITING -> "waiting"
                SHOWN -> "shown"
            }

        companion object {
            fun fromWire(raw: String?): CardStatus = when (raw?.trim()) {
                "waiting" -> WAITING
                "shown" -> SHOWN
                else -> IDLE
            }
        }
    }

    data class NotificationState(
        val cardStatus: CardStatus,
        val reminderStartedAt: String?,
        val cardShownAt: String?,
    ) {
        companion object {
            fun idle(cardShownAt: String? = null) = NotificationState(
                cardStatus = CardStatus.IDLE,
                reminderStartedAt = null,
                cardShownAt = cardShownAt,
            )
        }
    }

    data class CallResult(
        val unavailable: Boolean,
        val httpCode: Int,
        val state: NotificationState?,
        val retryable: Boolean = false,
    )

    fun get(context: Context): CallResult = request(context, "GET", "/api/device/notification")

    fun pile(context: Context): CallResult = request(context, "POST", "/api/device/notification/pile")

    fun looked(context: Context): CallResult = request(context, "POST", "/api/device/notification/looked")

    fun clear(context: Context): CallResult = request(context, "POST", "/api/device/notification/clear")

    @SuppressLint("HardwareIds", "MissingPermission")
    fun resolveImei(context: Context): String? {
        val override = BuildConfig.DEVICE_IMEI_OVERRIDE.trim()
        if (isImei(override)) return override

        val fromConfig = context.getSystemService(RestrictionsManager::class.java)
            ?.applicationRestrictions
            ?.getString("device_imei")
            ?.trim()
            .orEmpty()
        if (isImei(fromConfig)) return fromConfig

        val prefs = context.getSharedPreferences("phonly_device_notification", Context.MODE_PRIVATE)
        val cached = prefs.getString("imei", "").orEmpty()
        if (isImei(cached)) return cached

        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return null
        val candidates = ArrayList<String>()
        fun add(read: () -> String?) {
            try {
                val value = read()?.trim().orEmpty()
                if (value.isNotEmpty()) candidates.add(value)
            } catch (_: Exception) {
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add { telephony.imei }
            add { telephony.getImei(0) }
            add { telephony.getImei(1) }
        } else {
            @Suppress("DEPRECATION")
            add { telephony.deviceId }
        }
        val found = candidates.firstOrNull { isImei(it) }
        if (found != null) {
            prefs.edit().putString("imei", found).apply()
        }
        return found
    }

    private fun isImei(value: String): Boolean = value.matches(Regex("^\\d{14,16}$"))

    private fun request(context: Context, method: String, path: String): CallResult {
        val token = BuildConfig.NOTIFICATION_SECRET.trim()
        if (token.isEmpty()) {
            Log.w(TAG, "NOTIFICATION_SECRET empty; skip $method $path")
            return CallResult(unavailable = true, httpCode = 0, state = null, retryable = false)
        }
        val imei = resolveImei(context)
        if (imei == null) {
            Log.w(TAG, "No IMEI; skip $method $path")
            return CallResult(unavailable = true, httpCode = 0, state = null, retryable = false)
        }

        val encodedImei = URLEncoder.encode(imei, StandardCharsets.UTF_8.name())
        val encodedSource = URLEncoder.encode(SOURCE, StandardCharsets.UTF_8.name())
        val url = URL(
            BuildConfig.NOTIFICATION_API_BASE.trimEnd('/') + path +
                "?imei=$encodedImei&source=$encodedSource"
        )
        val body = if (method == "POST") {
            JSONObject()
                .put("imei", imei)
                .put("source", SOURCE)
                .toString()
        } else {
            null
        }

        // After a voice call Android may leave this process on the call path,
        // which cannot look up hosts. POST on WiFi / mobile data instead.
        // Do not bindProcessToNetwork. Do not go through Phone.
        var lastRetryable: CallResult? = null
        for (route in dataRoutes(context)) {
            val attempt = execute(url, method, token, body, route)
            if (!attempt.unavailable || !attempt.retryable) {
                return attempt
            }
            lastRetryable = attempt
        }
        return lastRetryable
            ?: CallResult(unavailable = true, httpCode = 0, state = null, retryable = true)
    }

    private data class Route(val label: String, val network: Network?)

    private fun dataRoutes(context: Context): List<Route> {
        val fallback = Route("default", null)
        val cm = context.getSystemService(ConnectivityManager::class.java)
            ?: return listOf(fallback)
        val ranked = try {
            cm.allNetworks.mapNotNull { network ->
                val caps = cm.getNetworkCapabilities(network) ?: return@mapNotNull null
                if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    return@mapNotNull null
                }
                Route(routeLabel(caps), network) to routeScore(caps)
            }.sortedByDescending { it.second }.map { it.first }.distinctBy { it.network }
        } catch (t: Throwable) {
            Log.w(TAG, "routes ${t.message}")
            emptyList()
        }
        return ranked + fallback
    }

    private fun routeScore(caps: NetworkCapabilities): Int {
        var score = 100
        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            score += 5
        }
        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)) {
            score += 10
        }
        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> score += 40
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> score += 30
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> score += 10
        }
        return score
    }

    private fun routeLabel(caps: NetworkCapabilities): String {
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cell"
            else -> "net"
        }
        val validated = if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            "ok"
        } else {
            "novalid"
        }
        return "$transport/$validated"
    }

    private fun execute(
        url: URL,
        method: String,
        token: String,
        body: String?,
        route: Route,
    ): CallResult {
        var connection: HttpURLConnection? = null
        return try {
            val raw = if (route.network != null) {
                route.network.openConnection(url)
            } else {
                url.openConnection()
            }
            connection = (raw as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 20_000
                useCaches = false
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
                }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            if (code == 429 || code == 503 || code >= 500) {
                Log.w(TAG, "$method via=${route.label} HTTP $code")
                return CallResult(unavailable = true, httpCode = code, state = null, retryable = true)
            }
            if (code !in 200..299) {
                Log.w(TAG, "$method via=${route.label} HTTP $code")
                return CallResult(unavailable = true, httpCode = code, state = null, retryable = false)
            }
            Log.i(TAG, "via=${route.label} HTTP $code")
            CallResult(unavailable = false, httpCode = code, state = parseState(text), retryable = false)
        } catch (t: Throwable) {
            Log.w(TAG, "$method via=${route.label} ${t.javaClass.simpleName}: ${t.message}")
            CallResult(unavailable = true, httpCode = 0, state = null, retryable = isRetryable(t))
        } finally {
            connection?.disconnect()
        }
    }

    private fun isRetryable(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            when (current) {
                is UnknownHostException,
                is SocketTimeoutException,
                is ConnectException,
                is NoRouteToHostException,
                is SocketException,
                is InterruptedIOException -> return true
            }
            current = current.cause
        }
        return false
    }

    private fun parseState(json: String): NotificationState? {
        if (json.isBlank()) {
            return NotificationState.idle()
        }
        return try {
            val root = JSONObject(json)
            if (root.isNull("notification")) {
                return NotificationState.idle()
            }
            val notification = root.optJSONObject("notification")
                ?: return NotificationState.idle()
            val slot = notification.optJSONObject(SOURCE) ?: notification
            parseSlot(slot)
        } catch (_: Exception) {
            null
        }
    }

    /** PhonlyAPI slot: cardStatus / reminderStartedAt / cardShownAt only. */
    private fun parseSlot(slot: JSONObject): NotificationState {
        val status = CardStatus.fromWire(jsonStringOrNull(slot, "cardStatus"))
        val cardShownAt = jsonStringOrNull(slot, "cardShownAt")
        val reminderStartedAt = jsonStringOrNull(slot, "reminderStartedAt")
        return when (status) {
            CardStatus.WAITING -> {
                if (reminderStartedAt == null) {
                    NotificationState.idle(cardShownAt)
                } else {
                    NotificationState(CardStatus.WAITING, reminderStartedAt, cardShownAt)
                }
            }
            CardStatus.SHOWN -> NotificationState(CardStatus.SHOWN, null, cardShownAt)
            CardStatus.IDLE -> NotificationState.idle(cardShownAt)
        }
    }

    private fun jsonStringOrNull(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optString(key).trim().ifBlank { null }?.takeIf { it != "null" }
    }
}
