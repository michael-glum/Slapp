package com.example.slapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat

class NotificationHandler {

    fun createChannel(context: Context, name: String,
                              descriptionText: String, importance: Int) {
        // Create the NotificationChannel.
        val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system.
        val notificationManager = context.getSystemService(ComponentActivity.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }

}