package org.fossify.messages.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.fossify.messages.helpers.UnreadOverlay

class UnreadOverlayService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            UnreadOverlay.refresh(applicationContext)
        }
        return START_STICKY
    }
}
