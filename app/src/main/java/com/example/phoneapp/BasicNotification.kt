package com.example.phoneapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class BasicNotification(
    private val context: Context
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun showBasicNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, "basic")
            .setContentTitle("Basic Notification")
            .setContentText("This is a notification")
            .setSmallIcon(R.drawable._50px_hl_gonarch_model)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(
            Random.nextInt(),
            notification
        )
    }
}