package com.devdiaz.orderless.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devdiaz.orderless.data.model.*
import kotlin.math.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BubbleViewModel : ViewModel() {

    private val _tasks = MutableStateFlow<List<TaskBubble>>(emptyList())
    val tasks: StateFlow<List<TaskBubble>> = _tasks.asStateFlow()

    private val _habits = MutableStateFlow<List<HabitBubble>>(emptyList())
    val habits: StateFlow<List<HabitBubble>> = _habits.asStateFlow()

    private val _filter = MutableStateFlow(BubbleFilter.ACTIVE)
    val filter: StateFlow<BubbleFilter> = _filter.asStateFlow()

    private var draggingId: String? = null

    // Canvas dimensions (will be updated from UI)
    private var canvasWidth = 1000f
    private var canvasHeight = 1000f

    init {
        // Initial example data
        viewModelScope.launch {
            _tasks.value =
                    listOf(
                            TaskBubble(
                                    id = "t1",
                                    text = "Comprar café",
                                    color = BubbleColors.Green400,
                                    x = 300f,
                                    y = 400f,
                                    vx = 0.5f,
                                    vy = 0.5f,
                                    radius = 50f,
                                    size = 100f,
                                    priority = Priority.MEDIUM
                            ),
                            TaskBubble(
                                    id = "t2",
                                    text = "Hacer ejercicio",
                                    color = BubbleColors.Blue400,
                                    x = 600f,
                                    y = 500f,
                                    vx = -0.5f,
                                    vy = 0.5f,
                                    radius = 80f,
                                    size = 160f,
                                    priority = Priority.HIGH
                            )
                    )

            _habits.value =
                    listOf(
                            HabitBubble(
                                    id = "h1",
                                    text = "Beber Agua",
                                    color = BubbleColors.Blue400,
                                    x = 300f,
                                    y = 400f,
                                    vx = 0.5f,
                                    vy = -0.5f,
                                    radius = 45f,
                                    size = 90f,
                                    days = listOf(0, 1, 2, 3, 4, 5, 6)
                            ),
                            HabitBubble(
                                    id = "h2",
                                    text = "Meditar",
                                    color = BubbleColors.Purple400,
                                    x = 500f,
                                    y = 500f,
                                    vx = -0.5f,
                                    vy = 0.5f,
                                    radius = 45f,
                                    size = 90f,
                                    days = listOf(1, 2, 3, 4, 5)
                            )
                    )
        }
    }

    fun updateCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
    }

    fun setFilter(newFilter: BubbleFilter) {
        _filter.value = newFilter
    }

    fun setDraggingId(id: String?) {
        draggingId = id
    }

    // Physics Engine
    fun updatePhysics() {
        _tasks.update { currentTasks ->
            updateBubblePhysics(currentTasks, false) as List<TaskBubble>
        }
        _habits.update { currentHabits ->
            updateBubblePhysics(currentHabits, true) as List<HabitBubble>
        }
    }

    private fun updateBubblePhysics(items: List<Bubble>, isHabitLayer: Boolean): List<Bubble> {
        // First pass: movement and wall collision
        val movedItems =
                items
                        .map { item ->
                            if (item.id == draggingId ||
                                            (item is TaskBubble &&
                                                    item.isCompleted &&
                                                    !isHabitLayer)
                            ) {
                                return@map item
                            }

                            // Apply velocity
                            var nx = item.x + item.vx
                            var ny = item.y + item.vy

                            // Friction/Damping
                            var nvx = item.vx * 0.992f
                            var nvy = item.vy * 0.992f

                            // Wall collisions with clamping to prevent tunneling
                            val minX = item.radius
                            val maxX = canvasWidth - item.radius

                            if (nx <= minX) {
                                nx = minX
                                nvx = abs(nvx) * 0.7f // Force positive velocity
                            } else if (nx >= maxX) {
                                nx = maxX
                                nvx = -abs(nvx) * 0.7f // Force negative velocity
                            }

                            // Top/Bottom bound
                            val minY = item.radius
                            val maxY = canvasHeight - item.radius

                            if (ny <= minY) {
                                ny = minY
                                nvy = abs(nvy) * 0.7f // Force positive velocity
                            } else if (ny >= maxY) {
                                ny = maxY
                                nvy = -abs(nvy) * 0.7f // Force negative velocity
                            }

                            // Update bubble state
                            when (item) {
                                is TaskBubble -> item.copy(x = nx, y = ny, vx = nvx, vy = nvy)
                                is HabitBubble -> item.copy(x = nx, y = ny, vx = nvx, vy = nvy)
                                else -> item
                            }
                        }
                        .toMutableList()

        // Second pass: Bubble Collisions (Iterative)
        val iterations = 5
        for (k in 0 until iterations) {
            for (i in movedItems.indices) {
                for (j in i + 1 until movedItems.size) {
                    val b1 = movedItems[i]
                    val b2 = movedItems[j]

                    val dx = b2.x - b1.x
                    val dy = b2.y - b1.y
                    val distanceSq = dx * dx + dy * dy
                    val minDistance = b1.radius + b2.radius

                    if (distanceSq < minDistance * minDistance) {
                        val distance = sqrt(distanceSq)
                        val overlap = minDistance - distance

                        // Prevent division by zero
                        val safeDistance = if (distance < 0.1f) 0.1f else distance

                        val nx = dx / safeDistance
                        val ny = dy / safeDistance

                        // Positional correction (push apart)
                        val separationX = nx * overlap * 0.5f
                        val separationY = ny * overlap * 0.5f

                        // Apply separation iteratively - using smaller correction per iteration
                        if (b1.id != draggingId) {
                            movedItems[i] =
                                    applyForce(
                                            movedItems[i],
                                            -separationX,
                                            -separationY,
                                            -nx * 0.05f / iterations,
                                            -ny * 0.05f / iterations
                                    )
                        }
                        if (b2.id != draggingId) {
                            movedItems[j] =
                                    applyForce(
                                            movedItems[j],
                                            separationX,
                                            separationY,
                                            nx * 0.05f / iterations,
                                            ny * 0.05f / iterations
                                    )
                        }
                    }
                }
            }
        }
        return movedItems
    }

    private fun applyForce(bubble: Bubble, dx: Float, dy: Float, dvx: Float, dvy: Float): Bubble {
        return when (bubble) {
            is TaskBubble ->
                    bubble.copy(
                            x = bubble.x + dx,
                            y = bubble.y + dy,
                            vx = bubble.vx + dvx,
                            vy = bubble.vy + dvy
                    )
            is HabitBubble ->
                    bubble.copy(
                            x = bubble.x + dx,
                            y = bubble.y + dy,
                            vx = bubble.vx + dvx,
                            vy = bubble.vy + dvy
                    )
            else -> bubble
        }
    }

    fun addTask(text: String, priority: Priority, color: Color) {
        val size = priority.size
        val newTask =
                TaskBubble(
                        id = "t${System.currentTimeMillis()}",
                        text = text,
                        size = size,
                        radius = size / 2,
                        color = color,
                        x = canvasWidth / 2,
                        y = canvasHeight / 2, // Start at center
                        vx = (Math.random().toFloat() - 0.5f) * 2f, // Slightly fasted launch
                        vy = (Math.random().toFloat() - 0.5f) * 2f,
                        priority = priority
                )
        _tasks.update { it + newTask }
    }

    fun addHabit(text: String, days: List<Int>, color: Color) {
        val size = 90f // Reduced from original
        val newHabit =
                HabitBubble(
                        id = "h${System.currentTimeMillis()}",
                        text = text,
                        size = size,
                        radius = size / 2,
                        color = color,
                        x = canvasWidth / 2,
                        y = canvasHeight / 2,
                        vx = (Math.random().toFloat() - 0.5f) * 1f,
                        vy = (Math.random().toFloat() - 0.5f) * 1f,
                        days = days
                )
        _habits.update { it + newHabit }
    }

    fun toggleTaskComplete(id: String) {
        _tasks.update { list ->
            list.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it }
        }
    }

    fun toggleHabitComplete(id: String) {
        _habits.update { list ->
            list.map { if (it.id == id) it.copy(completedToday = !it.completedToday) else it }
        }
    }

    fun deleteItem(id: String) {
        if (id.startsWith("t")) {
            _tasks.update { it.filter { t -> t.id != id } }
        } else {
            _habits.update { it.filter { h -> h.id != id } }
        }
    }

    fun updateBubblePosition(id: String, x: Float, y: Float) {
        _tasks.update { list ->
            list.map { if (it.id == id) it.copy(x = x, y = y, vx = 0f, vy = 0f) else it }
        }
        _habits.update { list ->
            list.map { if (it.id == id) it.copy(x = x, y = y, vx = 0f, vy = 0f) else it }
        }
    }

    fun launchBubble(id: String, vx: Float, vy: Float) {
        _tasks.update { list -> list.map { if (it.id == id) it.copy(vx = vx, vy = vy) else it } }
        _habits.update { list -> list.map { if (it.id == id) it.copy(vx = vx, vy = vy) else it } }
    }
}

enum class BubbleFilter {
    ACTIVE,
    HABITS,
    COMPLETED
}
