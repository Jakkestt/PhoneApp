package com.ad_coding.notificationscourse

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class NotificationApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val notificationChannel = NotificationChannel(
            "basic",
            "basic notification channel",
            NotificationManager.IMPORTANCE_HIGH
        )

        notificationChannel.description = "A notification channel for water reminders"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }
}