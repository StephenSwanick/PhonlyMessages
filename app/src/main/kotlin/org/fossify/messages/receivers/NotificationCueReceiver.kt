package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.messages.helpers.NotificationCue

class NotificationCueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationCue.ACTION_LOOKED -> NotificationCue.onPhoneLooked()
            NotificationCue.ACTION_LEFT -> NotificationCue.onPhoneLeft()
        }
    }
}
