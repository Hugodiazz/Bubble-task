package com.devdiaz.orderless.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.devdiaz.orderless.data.model.HabitBubble
import com.devdiaz.orderless.data.model.HabitCompletion
import com.devdiaz.orderless.data.model.TaskBubble

@Database(
        entities = [TaskBubble::class, HabitBubble::class, HabitCompletion::class],
        version = 1,
        exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BubbleDatabase : RoomDatabase() {
    abstract fun bubbleDao(): BubbleDao
}
