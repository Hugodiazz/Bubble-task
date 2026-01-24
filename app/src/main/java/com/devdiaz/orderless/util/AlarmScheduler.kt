package com.devdiaz.orderless.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.devdiaz.orderless.data.model.HabitBubble
import com.devdiaz.orderless.data.model.TaskBubble
import com.devdiaz.orderless.receiver.AlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: TaskBubble) {
        if (!task.isReminderEnabled || task.reminderTime == null) return

        val reminderTime = parseReminderTime(task.reminderTime) ?: return

        // Use task.dueDate if present (assuming it is a timestamp in millis or similar representing
        // date)
        // If dueDate represents a full date, we use that date + reminderTime
        // If dueDate is just date (midnight), we add reminderTime
        // If no dueDate, we assume Today.

        val triggerTime = calculateTriggerTime(reminderTime, task.dueDate)

        if (triggerTime <= System.currentTimeMillis()) {
            Log.w(
                    "AlarmScheduler",
                    "Skipping scheduling for past time: $triggerTime (now: ${System.currentTimeMillis()})"
            )
            return
        }

        val intent =
                Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("EXTRA_MESSAGE", task.text)
                    putExtra("EXTRA_BUBBLE_ID", task.id)
                    putExtra("EXTRA_NOTIFICATION_ID", task.notificationId)
                }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    PendingIntent.getBroadcast(
                            context,
                            task.notificationId,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
            )
            Log.d("AlarmScheduler", "Scheduled task ${task.text} at $triggerTime")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Failed to schedule alarm", e)
        }
    }

    fun schedule(habit: HabitBubble) {
        if (!habit.isReminderEnabled || habit.reminderTime == null || habit.days.isEmpty()) return

        val reminderTime = parseReminderTime(habit.reminderTime) ?: return

        val triggerTime = calculateNextTriggerTime(reminderTime, habit.days)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_MESSAGE", habit.text)
            putExtra("EXTRA_BUBBLE_ID", habit.id)
            putExtra("EXTRA_NOTIFICATION_ID", habit.notificationId)
            // Extras for rescheduling
            putExtra("EXTRA_IS_RECURRING", true)
            putExtra("EXTRA_REMINDER_TIME", habit.reminderTime)
            putExtra("EXTRA_DAYS", habit.days.toIntArray())
        }

        scheduleAlarm(triggerTime, habit.notificationId, intent)
    }

    // Called by AlarmReceiver to reschedule
    fun scheduleNext(
        id: String,
        notificationId: Int,
        text: String,
        reminderTimeStr: String,
        days: IntArray
    ) {
        val reminderTime = parseReminderTime(reminderTimeStr) ?: return
        // Since this is called WHEN the alarm fires (or shortly after), we want the *next* one.
        // We ensure we look into the future (now + 1 minute)
        
        val triggerTime = calculateNextTriggerTime(reminderTime, days.toList(), minTime = System.currentTimeMillis() + 60_000)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_MESSAGE", text)
            putExtra("EXTRA_BUBBLE_ID", id)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
            putExtra("EXTRA_IS_RECURRING", true)
            putExtra("EXTRA_REMINDER_TIME", reminderTimeStr)
            putExtra("EXTRA_DAYS", days)
        }

        scheduleAlarm(triggerTime, notificationId, intent)
    }

    private fun scheduleAlarm(triggerTime: Long, notificationId: Int, intent: Intent) {
         try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            Log.d("AlarmScheduler", "Scheduled alarm at $triggerTime for id $notificationId")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Failed to schedule alarm", e)
        }
    }

    fun cancel(task: TaskBubble) {
        cancelAlarm(task.notificationId)
    }

    fun cancel(habit: HabitBubble) {
        cancelAlarm(habit.notificationId)
    }
    
    private fun cancelAlarm(notificationId: Int) {
         alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                notificationId,
                Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun parseReminderTime(timeStr: String): Pair<Int, Int>? {
        return try {
            val parts = timeStr.split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateTriggerTime(time: Pair<Int, Int>, dateMillis: Long?): Long {
        val calendar = Calendar.getInstance()
        if (dateMillis != null && dateMillis > 0) {
            calendar.timeInMillis = dateMillis
        }
        // Keep the date, but set the time
        calendar.set(Calendar.HOUR_OF_DAY, time.first)
        calendar.set(Calendar.MINUTE, time.second)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun calculateNextTriggerTime(time: Pair<Int, Int>, days: List<Int>, minTime: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = minTime
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // Check "today" based on minTime first
        calendar.set(Calendar.HOUR_OF_DAY, time.first)
        calendar.set(Calendar.MINUTE, time.second)
        
        val maxDays = 8 
        for (i in 0..maxDays) {
            val checkCalendar = calendar.clone() as Calendar
            checkCalendar.add(Calendar.DAY_OF_YEAR, i)
            // Ensure time is set correctly after adding days (sometimes DST changes etc might shift if added blindly, but add(DAY) handles it usually)
            // Just to be safe, re-set hour/min is good but clone already had it. 
            
            if (checkCalendar.timeInMillis <= minTime) continue
            
            // Calendar.SUNDAY is 1, we want 0.
            val dayOfWeek = checkCalendar.get(Calendar.DAY_OF_WEEK) - 1
            if (days.contains(dayOfWeek)) {
                return checkCalendar.timeInMillis
            }
        }
        
        // Fallback: Tomorrow same time
         val fallback = Calendar.getInstance()
         fallback.timeInMillis = minTime
         fallback.add(Calendar.DAY_OF_YEAR, 1)
         return fallback.timeInMillis
    }
}
