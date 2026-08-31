package org.fossify.messages.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Telephony.Threads
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getLongValue
import org.fossify.commons.extensions.getStringValue
import org.fossify.commons.extensions.queryCursor
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.activities.MainActivity
import org.fossify.messages.activities.ThreadActivity
import org.fossify.messages.databinding.UnreadOverlayBubbleBinding
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.conversationsDB
import org.fossify.messages.extensions.getNotificationBitmap
import org.fossify.messages.extensions.getThreadParticipants
import org.fossify.messages.extensions.getUnreadCountsByThread
import org.fossify.messages.services.UnreadOverlayService
import kotlin.math.abs

/**
 * Kid-visible unread cue: small bubbles over Home until that thread is read.
 * No Settings snackbar if the overlay grant is missing.
 */
object UnreadOverlay {
    const val MORE_THREAD_ID = -1L
    private const val MAX_BUBBLES = 3
    private const val MAX_THREAD_BUBBLES_WITH_OVERFLOW = 2

    @Volatile
    var openThreadId: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var refreshSeq = 0
    private var container: LinearLayout? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var attached = false

    fun refresh(context: Context) {
        val app = context.applicationContext
        val seq = ++refreshSeq
        if (!Settings.canDrawOverlays(app)) {
            mainHandler.post {
                if (seq == refreshSeq) {
                    clear(app)
                    app.stopService(Intent(app, UnreadOverlayService::class.java))
                }
            }
            return
        }

        ensureBackgroundThread {
            val items = loadItems(app)
            mainHandler.post {
                if (seq != refreshSeq) {
                    return@post
                }
                render(app, items)
                val serviceIntent = Intent(app, UnreadOverlayService::class.java)
                if (items.isEmpty()) {
                    app.stopService(serviceIntent)
                } else {
                    try {
                        app.startService(serviceIntent)
                    } catch (_: IllegalStateException) {
                    }
                }
            }
        }
    }

    private data class OverlayItem(
        val threadId: Long,
        val title: String,
        val phoneNumber: String,
        val unreadCount: Int,
        val photo: Bitmap?,
        val isMore: Boolean,
    )

    private data class Candidate(
        val item: OverlayItem,
        val isGroup: Boolean,
        val date: Int,
        val recipientIds: Set<Int>,
    )

    private data class InboxThread(
        val threadId: Long,
        val recipientIds: Set<Int>,
    )

    private fun loadItems(context: Context): List<OverlayItem> {
        val counts = try {
            context.getUnreadCountsByThread()
        } catch (_: Exception) {
            emptyMap()
        }
        val inboxThreads = unreadInboxThreads(context)
        val openId = openThreadId
        val contacts = SimpleContactsHelper(context)
        val bubbleSize = context.resources.getDimensionPixelSize(R.dimen.unread_overlay_bubble_size)

        val candidates = inboxThreads
            .filter { it.threadId != openId && it.threadId > 0L }
            .mapNotNull { inboxThread ->
                val threadId = inboxThread.threadId
                val count = counts[threadId] ?: 0
                val unreadCount = if (count > 0) count else 1
                val conversation = try {
                    context.conversationsDB.getConversationWithThreadId(threadId)
                } catch (_: Exception) {
                    null
                }
                val numbers = participantNumbers(context, threadId, conversation?.phoneNumber)
                val isGroup = conversation?.isGroupConversation == true ||
                    inboxThread.recipientIds.size > 1 ||
                    numbers.size > 1
                val title = conversation?.title?.ifBlank { null }
                    ?: conversation?.phoneNumber?.ifBlank { null }
                    ?: numbers.firstOrNull()
                if (title.isNullOrBlank()) {
                    return@mapNotNull null
                }
                val phoneNumber = conversation?.phoneNumber.orEmpty()
                val photoUri = conversation?.photoUri.orEmpty()
                val photo = when {
                    photoUri.isNotEmpty() -> context.getNotificationBitmap(photoUri)
                    isGroup -> try {
                        contacts.getColoredGroupIcon(title).toBitmap(bubbleSize, bubbleSize)
                    } catch (_: Exception) {
                        contacts.getContactLetterIcon(title)
                    }
                    else -> contacts.getContactLetterIcon(title)
                }
                Candidate(
                    item = OverlayItem(
                        threadId = threadId,
                        title = title,
                        phoneNumber = phoneNumber,
                        unreadCount = unreadCount,
                        photo = photo,
                        isMore = false,
                    ),
                    isGroup = isGroup,
                    date = conversation?.date ?: Int.MAX_VALUE,
                    recipientIds = inboxThread.recipientIds,
                )
            }

        val threads = collapseIdenticalThreads(candidates)
            .sortedByDescending { it.date }
            .map { it.item }

        if (threads.size <= MAX_BUBBLES) {
            return threads
        }

        val shown = threads.take(MAX_THREAD_BUBBLES_WITH_OVERFLOW)
        val hidden = threads.drop(MAX_THREAD_BUBBLES_WITH_OVERFLOW)
        return shown + OverlayItem(
            threadId = MORE_THREAD_ID,
            title = context.getString(R.string.app_launcher_name),
            phoneNumber = "",
            unreadCount = hidden.size,
            photo = null,
            isMore = true,
        )
    }

    private fun unreadInboxThreads(context: Context): List<InboxThread> {
        val threads = queryInboxThreads(context, unreadOnly = true)
        if (threads.isNotEmpty()) {
            return threads
        }
        val existingIds = queryInboxThreads(context, unreadOnly = false)
            .map { it.threadId }
            .toSet()
        if (existingIds.isEmpty()) {
            return emptyList()
        }
        return try {
            context.conversationsDB.getUnreadConversations()
                .filter { it.threadId > 0L && it.threadId in existingIds }
                .map { InboxThread(it.threadId, emptySet()) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun queryInboxThreads(context: Context, unreadOnly: Boolean): List<InboxThread> {
        val threads = ArrayList<InboxThread>()
        try {
            context.queryCursor(
                uri = "${Threads.CONTENT_URI}?simple=true".toUri(),
                projection = arrayOf(Threads._ID, Threads.RECIPIENT_IDS),
                selection = if (unreadOnly) "${Threads.READ} = 0" else null,
                selectionArgs = null,
                showErrors = false,
            ) { cursor ->
                val id = cursor.getLongValue(Threads._ID)
                if (id <= 0L) {
                    return@queryCursor
                }
                val recipientIds = cursor.getStringValue(Threads.RECIPIENT_IDS)
                    .orEmpty()
                    .split(" ")
                    .mapNotNull { token -> token.trim().toIntOrNull() }
                    .toSet()
                threads.add(InboxThread(id, recipientIds))
            }
        } catch (_: Exception) {
        }
        return threads
    }

    private fun participantNumbers(
        context: Context,
        threadId: Long,
        fallbackNumber: String?,
    ): Set<String> {
        val fromThread = try {
            context.getThreadParticipants(threadId, null)
                .mapNotNull { contact ->
                    contact.phoneNumbers.firstOrNull()?.normalizedNumber?.let { comparableNumber(it) }
                }
                .filter { it.isNotEmpty() }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
        if (fromThread.isNotEmpty()) {
            return fromThread
        }
        val fallback = fallbackNumber?.let { comparableNumber(it) }.orEmpty()
        return if (fallback.isEmpty()) emptySet() else setOf(fallback)
    }

    private fun comparableNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    /** Only fold true clones (same recipients, two thread IDs). Distinct groups stay distinct. */
    private fun collapseIdenticalThreads(candidates: List<Candidate>): List<Candidate> {
        if (candidates.size <= 1) {
            return candidates
        }
        val sorted = candidates.sortedWith(
            compareByDescending<Candidate> { it.recipientIds.size }
                .thenByDescending { it.isGroup }
                .thenByDescending { it.date }
        )
        val kept = ArrayList<Candidate>(sorted.size)
        for (candidate in sorted) {
            val duplicate = candidate.recipientIds.isNotEmpty() &&
                kept.any { it.recipientIds.isNotEmpty() && it.recipientIds == candidate.recipientIds }
            if (!duplicate) {
                kept.add(candidate)
            }
        }
        return kept
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun render(context: Context, items: List<OverlayItem>) {
        if (items.isEmpty()) {
            clear(context)
            return
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val host = container ?: LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            container = this
        }

        host.removeAllViews()
        val gap = context.resources.getDimensionPixelSize(R.dimen.unread_overlay_gap)
        items.forEachIndexed { index, item ->
            val bubble = inflateBubble(context, item, windowManager)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            if (index > 0) {
                lp.topMargin = gap
            }
            host.addView(bubble, lp)
        }

        val params = windowParams ?: buildLayoutParams(context).also { windowParams = it }
        clamp(context, params)
        if (!attached) {
            try {
                windowManager.addView(host, params)
                attached = true
            } catch (_: Exception) {
                attached = false
            }
        } else {
            try {
                windowManager.updateViewLayout(host, params)
            } catch (_: Exception) {
                attached = false
                try {
                    windowManager.addView(host, params)
                    attached = true
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun inflateBubble(
        context: Context,
        item: OverlayItem,
        windowManager: WindowManager,
    ): View {
        val binding = UnreadOverlayBubbleBinding.inflate(android.view.LayoutInflater.from(context))
        binding.unreadOverlayPhoto.outlineProvider = ViewOutlineProvider.BACKGROUND
        binding.unreadOverlayPhoto.clipToOutline = true
        if (item.isMore) {
            binding.unreadOverlayPhoto.beGone()
            binding.unreadOverlayMore.beVisible()
            binding.root.contentDescription = item.title
        } else {
            binding.unreadOverlayMore.beGone()
            binding.unreadOverlayPhoto.beVisible()
            binding.unreadOverlayPhoto.setImageBitmap(item.photo)
            binding.root.contentDescription = item.title
        }

        val showBadge = item.isMore || item.unreadCount > 1
        binding.unreadOverlayBadge.beVisibleIf(showBadge)
        if (showBadge) {
            binding.unreadOverlayBadge.text = if (item.unreadCount > 99) "99+" else item.unreadCount.toString()
        }

        bindDragAndTap(context, binding.root, windowManager, item)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindDragAndTap(
        context: Context,
        view: View,
        windowManager: WindowManager,
        item: OverlayItem,
    ) {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragging = false

        view.setOnTouchListener { _, event ->
            val params = windowParams ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downRawX).toInt()
                    val dy = (event.rawY - downRawY).toInt()
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) {
                        dragging = true
                    }
                    if (dragging) {
                        params.x = startX + dx
                        params.y = startY + dy
                        clamp(context, params)
                        try {
                            windowManager.updateViewLayout(container, params)
                        } catch (_: Exception) {
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        context.config.unreadOverlayX = params.x
                        context.config.unreadOverlayY = params.y
                    } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                        openTarget(context, item)
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun openTarget(context: Context, item: OverlayItem) {
        val intent = if (item.isMore) {
            Intent(context, MainActivity::class.java)
        } else {
            Intent(context, ThreadActivity::class.java).apply {
                putExtra(THREAD_ID, item.threadId)
                putExtra(THREAD_TITLE, item.title)
                if (item.phoneNumber.isNotBlank()) {
                    putExtra(THREAD_NUMBER, item.phoneNumber)
                }
            }
        }
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        if (!item.isMore) {
            openThreadId = item.threadId
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
        }
        refresh(context)
    }

    private fun buildLayoutParams(context: Context): WindowManager.LayoutParams {
        val metrics = context.resources.displayMetrics
        val size = context.resources.getDimensionPixelSize(R.dimen.unread_overlay_bubble_size)
        val margin = context.resources.getDimensionPixelSize(R.dimen.unread_overlay_edge_margin)
        val savedX = context.config.unreadOverlayX
        val savedY = context.config.unreadOverlayY
        val defaultX = metrics.widthPixels - size - margin
        val defaultY = metrics.heightPixels - size * 3 - margin * 6
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (savedX == Int.MIN_VALUE) defaultX.coerceAtLeast(margin) else savedX
            y = if (savedY == Int.MIN_VALUE) defaultY.coerceAtLeast(margin) else savedY
        }
    }

    private fun clamp(context: Context, params: WindowManager.LayoutParams) {
        val metrics = context.resources.displayMetrics
        val host = container
        val width = host?.width?.takeIf { it > 0 }
            ?: context.resources.getDimensionPixelSize(R.dimen.unread_overlay_bubble_size)
        val height = host?.height?.takeIf { it > 0 }
            ?: context.resources.getDimensionPixelSize(R.dimen.unread_overlay_bubble_size)
        params.x = params.x.coerceIn(0, (metrics.widthPixels - width).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (metrics.heightPixels - height).coerceAtLeast(0))
    }

    private fun clear(context: Context) {
        val host = container ?: return
        if (attached) {
            try {
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(host)
            } catch (_: Exception) {
            }
        }
        attached = false
        host.removeAllViews()
    }
}

fun Context.refreshUnreadOverlay() {
    UnreadOverlay.refresh(this)
}
