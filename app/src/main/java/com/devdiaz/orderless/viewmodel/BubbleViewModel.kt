package com.devdiaz.orderless.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.devdiaz.orderless.BubbleApplication
import com.devdiaz.orderless.data.model.*
import com.devdiaz.orderless.data.repository.BubbleRepository
import com.devdiaz.orderless.util.AlarmScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BubbleViewModel(
        private val repository: BubbleRepository,
        private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<TaskBubble>>(emptyList())
    val tasks: StateFlow<List<TaskBubble>> = _tasks.asStateFlow()

    private val _habits = MutableStateFlow<List<HabitBubble>>(emptyList())
    val habits: StateFlow<List<HabitBubble>> = _habits.asStateFlow()

    private val _todayCompletions = MutableStateFlow<Set<String>>(emptySet())
    val todayCompletions: StateFlow<Set<String>> = _todayCompletions.asStateFlow()

    private val _section = MutableStateFlow(BubbleSection.ACTIVE)
    val section: StateFlow<BubbleSection> = _section.asStateFlow()

    private val _status = MutableStateFlow(BubbleStatus.TASKS)
    val status: StateFlow<BubbleStatus> = _status.asStateFlow()

    // Date Reactivity
    private val _currentDate = MutableStateFlow(getTodayDate())
    // Trigger updates every minute to check for date change
    private val _timer =
            kotlinx.coroutines.flow.flow {
                while (true) {
                    emit(Unit)
                    kotlinx.coroutines.delay(60_000)
                }
            }

    private var draggingId: String? = null

    // Canvas dimensions (will be updated from UI)
    private var canvasWidth = 1000f
    private var canvasHeight = 1000f

    init {
        // Collect Tasks from DB
        viewModelScope.launch {
            repository.allTasks.collect { dbTasks ->
                _tasks.update { currentTasks ->
                    // Merge DB data with local physics state
                    dbTasks.map { dbTask ->
                        val localTask = currentTasks.find { it.id == dbTask.id }
                        if (localTask != null) {
                            dbTask.copy(
                                    x = localTask.x,
                                    y = localTask.y,
                                    vx = localTask.vx,
                                    vy = localTask.vy
                            )
                        } else {
                            dbTask.copy(
                                    x = if (dbTask.x == 0f) canvasWidth / 2 else dbTask.x,
                                    y = if (dbTask.y == 0f) canvasHeight / 2 else dbTask.y
                            )
                        }
                    }
                }
            }
        }

        // Monitor Date Changes
        viewModelScope.launch {
            _timer.collect {
                val newDate = getTodayDate()
                if (_currentDate.value != newDate) {
                    _currentDate.value = newDate
                }
            }
        }

        // Collect Habits from DB with Date Reactivity
        viewModelScope.launch {
            combine(repository.allHabits, _currentDate) { dbHabits, _ ->
                val todayIndex = getCurrentDayOfWeek()
                dbHabits.filter { it.days.contains(todayIndex) }
            }
                    .collect { visibleHabits ->
                        _habits.update { currentHabits ->
                            visibleHabits.map { dbHabit ->
                                val localHabit = currentHabits.find { it.id == dbHabit.id }
                                if (localHabit != null) {
                                    dbHabit.copy(
                                            x = localHabit.x,
                                            y = localHabit.y,
                                            vx = localHabit.vx,
                                            vy = localHabit.vy
                                    )
                                } else {
                                    dbHabit.copy(
                                            x = if (dbHabit.x == 0f) canvasWidth / 2 else dbHabit.x,
                                            y = if (dbHabit.y == 0f) canvasHeight / 2 else dbHabit.y
                                    )
                                }
                            }
                        }
                    }
        }

        // Collect Today's Completions with Date Reactivity
        viewModelScope.launch {
            _currentDate.flatMapLatest { date -> repository.getCompletionsForDate(date) }.collect {
                    completions ->
                _todayCompletions.value = completions.map { it.habitBubbleId }.toSet()
            }
        }
    }
    // Initial example data

    fun updateCanvasSize(width: Float, height: Float) {
        canvasWidth = width
        canvasHeight = height
    }

    fun setSection(newSection: BubbleSection) {
        _section.value = newSection
    }

    fun setStatus(newStatus: BubbleStatus) {
        _status.value = newStatus
    }

    fun setDraggingId(id: String?) {
        draggingId = id
    }

    // Physics Engine
    fun updatePhysics() {
        _tasks.update { currentTasks ->
            @Suppress("UNCHECKED_CAST") updateBubblePhysics(currentTasks, false) as List<TaskBubble>
        }
        _habits.update { currentHabits ->
            @Suppress("UNCHECKED_CAST")
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

    fun addTask(
            text: String,
            priority: Priority,
            color: Color,
            dueDate: Long?,
            reminderTime: String?,
            isReminderEnabled: Boolean
    ) {
        val size = priority.size
        val newTask =
                TaskBubble(
                        id = "t${System.currentTimeMillis()}",
                        text = text,
                        size = size,
                        radius = size / 2,
                        color = color.value.toLong(),
                        x = canvasWidth / 2,
                        y = canvasHeight / 2, // Start at center
                        vx = (Math.random().toFloat() - 0.5f) * 2f, // Slightly fasted launch
                        vy = (Math.random().toFloat() - 0.5f) * 2f,
                        priority = priority,
                        dueDate = dueDate,
                        reminderTime = reminderTime,
                        notificationId = System.currentTimeMillis().toInt(),
                        isReminderEnabled = isReminderEnabled
                )
        viewModelScope.launch { repository.insertTask(newTask) }
        alarmScheduler.schedule(newTask)
    }

    fun addHabit(
            text: String,
            days: List<Int>,
            color: Color,
            reminderTime: String?,
            isReminderEnabled: Boolean
    ) {
        val size = 90f // Reduced from original
        val newHabit =
                HabitBubble(
                        id = "h${System.currentTimeMillis()}",
                        text = text,
                        size = size,
                        radius = size / 2,
                        color = color.value.toLong(),
                        x = canvasWidth / 2,
                        y = canvasHeight / 2,
                        vx = (Math.random().toFloat() - 0.5f) * 1f,
                        vy = (Math.random().toFloat() - 0.5f) * 1f,
                        days = days,
                        reminderTime = reminderTime,
                        notificationId = System.currentTimeMillis().toInt(),
                        isReminderEnabled = isReminderEnabled
                )
        viewModelScope.launch { repository.insertHabit(newHabit) }
        alarmScheduler.schedule(newHabit)
    }

    fun toggleTaskComplete(id: String) {
        val task = _tasks.value.find { it.id == id }
        if (task != null) {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            viewModelScope.launch { repository.updateTask(updatedTask) }
        }
    }

    fun toggleHabitComplete(id: String) {
        val today = getTodayDate()
        viewModelScope.launch {
            val existing = repository.getCompletion(id, today)
            if (existing != null) {
                repository.deleteCompletion(existing)
            } else {
                repository.insertCompletion(HabitCompletion(habitBubbleId = id, date = today))
            }
        }
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getCurrentDayOfWeek(): Int {
        val calendar = Calendar.getInstance()
        // Calendar.SUNDAY is 1, Monday is 2, etc.
        // We want 0 = Sunday, 1 = Monday.
        return calendar.get(Calendar.DAY_OF_WEEK) - 1
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            if (id.startsWith("t")) {
                val task = _tasks.value.find { it.id == id }
                if (task != null) {
                    repository.deleteTask(task)
                    alarmScheduler.cancel(task)
                }
            } else {
                val habit = _habits.value.find { it.id == id }
                if (habit != null) {
                    repository.deleteHabit(habit)
                    alarmScheduler.cancel(habit)
                }
            }
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[APPLICATION_KEY] as BubbleApplication)
                BubbleViewModel(app.repository, AlarmScheduler(app))
            }
        }
    }
}

enum class BubbleSection(val label: String) {
    ACTIVE("Tareas"),
    HABITS("Hábitos")
}

enum class BubbleStatus(val label: String) {
    TASKS("Pendientes"),
    COMPLETED("Completado")
}
