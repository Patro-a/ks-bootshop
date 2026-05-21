package com.jarvis.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.R

/**
 * Keeps JARVIS alive when the app is minimized.
 * Start via context.startForegroundService(Intent(context, JarvisService::class.java))
 */
class JarvisService : Service() {

    companion object {
        private const val CHANNEL_ID      = "jarvis_bg_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun createChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID,
            "JARVIS Assistant",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "JARVIS is listening in the background" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS is active")
            .setContentText("Tap to open")
            .setSmallIcon(R.drawable.ic_jarvis_notif)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
    }
}
