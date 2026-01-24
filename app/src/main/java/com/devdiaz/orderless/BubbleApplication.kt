package com.devdiaz.orderless

import android.app.Application
import androidx.room.Room
import com.devdiaz.orderless.data.local.BubbleDatabase
import com.devdiaz.orderless.data.repository.BubbleRepository

class BubbleApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(applicationContext, BubbleDatabase::class.java, "bubble_database")
                .build()
    }

    val repository by lazy { BubbleRepository(database.bubbleDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val name = "PopTasks Reminders"
        val descriptionText = "Notifications for PopTasks and Habits"
        val importance = android.app.NotificationManager.IMPORTANCE_HIGH
        val channel =
                android.app.NotificationChannel("poptasks_reminders", name, importance).apply {
                    description = descriptionText
                }
        val notificationManager: android.app.NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as
                        android.app.NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
