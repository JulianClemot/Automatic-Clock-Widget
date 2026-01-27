package com.julian.automaticclockwidget.airplanemode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.julian.automaticclockwidget.R

class AirplaneModeMonitorService : Service() {
    private val airplaneModeReceiver = AirplaneModeReceiver()

    override fun onCreate() {
        super.onCreate()

        // Create notification channel
        createNotificationChannel()

        // Start as foreground service
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Babe Schedule Monitoring")
            .setContentText("Watching for airplane mode changes...")
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Register receiver
        val filter = IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        registerReceiver(airplaneModeReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(airplaneModeReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Airplane Mode Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "airplane_mode_monitor"
        private const val NOTIFICATION_ID = 1
    }
}