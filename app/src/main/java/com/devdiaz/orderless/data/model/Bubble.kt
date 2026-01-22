package com.devdiaz.orderless.data.model

import androidx.compose.ui.graphics.Color

interface Bubble {
    val id: String
    val text: String
    val color: Color
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

data class TaskBubble(
        override val id: String,
        override val text: String,
        override val color: Color,
        override var x: Float,
        override var y: Float,
        override var vx: Float,
        override var vy: Float,
        override val radius: Float,
        override val size: Float,
        val priority: Priority,
        var isCompleted: Boolean = false,
        val createdAt: Long = System.currentTimeMillis()
) : Bubble

data class HabitBubble(
        override val id: String,
        override val text: String,
        override val color: Color,
        override var x: Float,
        override var y: Float,
        override var vx: Float,
        override var vy: Float,
        override val radius: Float,
        override val size: Float,
        val days: List<Int>, // 0 = Sunday, 1 = Monday, etc.
        var completedToday: Boolean = false
) : Bubble

object BubbleColors {
    val Blue400 = Color(0xFF60A5FA)
    val Purple400 = Color(0xFFC084FC)
    val Pink400 = Color(0xFFF472B6)
    val Green400 = Color(0xFF4ADE80)
    val Yellow400 = Color(0xFFFACC15)
    val Orange400 = Color(0xFFFB923C)
    val Red400 = Color(0xFFF87171)
    val Teal400 = Color(0xFF2DD4BF)

    val All = listOf(Blue400, Purple400, Pink400, Green400, Yellow400, Orange400, Red400, Teal400)
}
