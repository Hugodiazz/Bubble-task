package com.devdiaz.orderless.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

interface Bubble {
    val id: String
    val text: String
    val color: Long
    var x: Float
    var y: Float
    var vx: Float
    var vy: Float
    val radius: Float
    val size: Float
}

enum class Priority(val label: String, val size: Float) {
    LOW("Baja", 100f),
    MEDIUM("Media", 130f),
    HIGH("Alta", 160f)
}

@Entity(tableName = "task_bubbles")
data class TaskBubble(
        @PrimaryKey override val id: String,
        override val text: String,
        @ColumnInfo(name = "color") override val color: Long,
        override var x: Float,
        override var y: Float,
        override var vx: Float,
        override var vy: Float,
        override val radius: Float,
        override val size: Float,
        val priority: Priority,
        var isCompleted: Boolean = false,
        val createdAt: Long = System.currentTimeMillis(),
        val completedAt: Long? = null,
        val reminderTime: String?, // Formato "HH:mm"
        val notificationId: Int, // Un ID único para gestionar/cancelar la alarma
        val isReminderEnabled: Boolean = false
) : Bubble

@Entity(tableName = "habit_bubbles")
data class HabitBubble(
        @PrimaryKey override val id: String,
        override val text: String,
        @ColumnInfo(name = "color") override val color: Long,
        override var x: Float,
        override var y: Float,
        override var vx: Float,
        override var vy: Float,
        override val radius: Float,
        override val size: Float,
        val createdAt: Long = System.currentTimeMillis(),
        val days: List<Int>, // 0 = Sunday, 1 = Monday, etc.
        val reminderTime: String?, // Formato "HH:mm"
        val notificationId: Int, // Un ID único para gestionar/cancelar la alarma
        val isReminderEnabled: Boolean = false
) : Bubble

@Entity(tableName = "habit_completions", primaryKeys = ["habitBubbleId", "date"])
data class HabitCompletion(
        val habitBubbleId: String,
        val timestamp: Long = System.currentTimeMillis(),
        val date: String // Formato "yyyy-MM-dd" para fácil indexación
)

object BubbleColors {
    const val Blue400 = 0xFF60A5FA
    const val Purple400 = 0xFFC084FC
    const val Pink400 = 0xFFF472B6
    const val Green400 = 0xFF4ADE80
    const val Yellow400 = 0xFFFACC15
    const val Orange400 = 0xFFFB923C
    const val Red400 = 0xFFF87171
    const val Teal400 = 0xFF2DD4BF

    val All = listOf(Blue400, Purple400, Pink400, Green400, Yellow400, Orange400, Red400, Teal400)
}
