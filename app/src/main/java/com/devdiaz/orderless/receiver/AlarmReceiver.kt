package com.devdiaz.orderless.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.devdiaz.orderless.MainActivity
import com.devdiaz.orderless.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: return
        val bubbleId = intent.getStringExtra("EXTRA_BUBBLE_ID") ?: ""
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0)

        // Intent to open the app when notification is clicked
        val tapIntent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
        val pendingIntent: PendingIntent =
                PendingIntent.getActivity(context, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder =
                NotificationCompat.Builder(context, "poptasks_reminders")
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle("Recordatorio")
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)

        val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // permissions might be missing
            e.printStackTrace()
        }

        // Handle Rescheduling for Habits
        val isRecurring = intent.getBooleanExtra("EXTRA_IS_RECURRING", false)
        if (isRecurring) {
            val reminderTime = intent.getStringExtra("EXTRA_REMINDER_TIME")
            val days = intent.getIntArrayExtra("EXTRA_DAYS")

            if (reminderTime != null && days != null) {
                val scheduler = com.devdiaz.orderless.util.AlarmScheduler(context)
                scheduler.scheduleNext(
                        id = bubbleId,
                        notificationId = notificationId,
                        text = message,
                        reminderTimeStr = reminderTime,
                        days = days
                )
            }
        }
    }
}
