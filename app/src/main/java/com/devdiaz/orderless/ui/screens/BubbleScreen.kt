package com.devdiaz.orderless.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devdiaz.orderless.data.model.Bubble
import com.devdiaz.orderless.data.model.HabitBubble
import com.devdiaz.orderless.data.model.TaskBubble
import com.devdiaz.orderless.ui.components.BubbleActionMenu
import com.devdiaz.orderless.ui.components.BubbleItemView
import com.devdiaz.orderless.ui.components.CreationDialog
import com.devdiaz.orderless.ui.components.ExpandableFilterFab
import com.devdiaz.orderless.viewmodel.BubbleFilter
import com.devdiaz.orderless.viewmodel.BubbleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun BubbleScreen(viewModel: BubbleViewModel = viewModel()) {
        val tasks by viewModel.tasks.collectAsState()
        val habits by viewModel.habits.collectAsState()
        val filter by viewModel.filter.collectAsState() // ACTIVE, HABITS, COMPLETED

        var showCreationDialog by remember { mutableStateOf(false) }
        var selectedBubble by remember { mutableStateOf<Bubble?>(null) }

        // Physics Loop
        LaunchedEffect(Unit) {
                while (isActive) {
                        viewModel.updatePhysics()
                        delay(16) // Approx 60 FPS
                }
        }

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(Color(0xFF020617)) // Slate 950
                                .onSizeChanged { size ->
                                        viewModel.updateCanvasSize(
                                                size.width.toFloat(),
                                                size.height.toFloat()
                                        )
                                }
        ) {
                BackgroundDecoration()

                // Content Layer
                Box(modifier = Modifier.fillMaxSize()) {
                        val visibleBubbles: List<Bubble> =
                                when (filter) {
                                        BubbleFilter.ACTIVE -> tasks.filter { !it.isCompleted }
                                        BubbleFilter.HABITS ->
                                                habits // Filter by day? React code does, but
                                        // simplified here
                                        BubbleFilter.COMPLETED ->
                                                emptyList() // Render list separately
                                }

                        if (filter != BubbleFilter.COMPLETED) {
                                visibleBubbles.forEach { bubble ->
                                        key(bubble.id) {
                                                DraggableBubble(
                                                        bubble = bubble,
                                                        onDragStart = {
                                                                viewModel.setDraggingId(bubble.id)
                                                        },
                                                        onDragEnd = { vx, vy ->
                                                                viewModel.setDraggingId(null)
                                                                viewModel.launchBubble(
                                                                        bubble.id,
                                                                        vx,
                                                                        vy
                                                                )
                                                        },
                                                        onDrag = { x, y ->
                                                                viewModel.updateBubblePosition(
                                                                        bubble.id,
                                                                        x,
                                                                        y
                                                                )
                                                        },
                                                        onClick = { selectedBubble = bubble }
                                                )
                                        }
                                }
                        } else {
                                val completedTasks = tasks.filter { it.isCompleted }

                                androidx.compose.foundation.lazy.LazyColumn(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .padding(
                                                                top = 100.dp,
                                                                bottom = 32.dp
                                                        ), // Adjust for nav bar
                                        contentPadding = PaddingValues(horizontal = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                        items(completedTasks.size) { index ->
                                                val task = completedTasks[index]
                                                CompletedTaskItem(
                                                        task = task,
                                                        onRestore = {
                                                                viewModel.toggleTaskComplete(
                                                                        task.id
                                                                )
                                                        },
                                                        onDelete = { viewModel.deleteItem(task.id) }
                                                )
                                        }
                                }
                        }
                }

                // FABs
                Column(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp),
                        horizontalAlignment = Alignment.End
                ) {
                        ExpandableFilterFab(
                                currentFilter = filter,
                                onFilterSelected = { viewModel.setFilter(it) }
                        )

                        FloatingActionButton(
                                onClick = { showCreationDialog = true },
                                modifier = Modifier.size(72.dp),
                                containerColor = Color.White,
                                contentColor = Color.Black,
                                shape = CircleShape
                        ) {
                                Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add",
                                        modifier = Modifier.size(32.dp)
                                )
                        }
                }

                if (showCreationDialog) {
                        CreationDialog(
                                onDismiss = { showCreationDialog = false },
                                onCreateTask = { text, priority, color ->
                                        viewModel.addTask(text, priority, color)
                                },
                                onCreateHabit = { text, days, color ->
                                        viewModel.addHabit(text, days, color)
                                }
                        )
                }

                selectedBubble?.let { bubble ->
                        BubbleActionMenu(
                                bubbleText = bubble.text,
                                onComplete = {
                                        if (bubble is TaskBubble)
                                                viewModel.toggleTaskComplete(bubble.id)
                                        else viewModel.toggleHabitComplete(bubble.id)
                                        selectedBubble = null
                                },
                                onDelete = {
                                        viewModel.deleteItem(bubble.id)
                                        selectedBubble = null
                                },
                                onDismiss = { selectedBubble = null }
                        )
                }
        }
}

@Composable
fun DraggableBubble(
        bubble: Bubble,
        onDragStart: () -> Unit,
        onDragEnd: (vx: Float, vy: Float) -> Unit, // Returns velocity
        onDrag: (x: Float, y: Float) -> Unit,
        onClick: () -> Unit
) {
        // Velocity tracking
        var lastPosition by remember { mutableStateOf(Offset(bubble.x, bubble.y)) }
        var velocity by remember { mutableStateOf(Offset.Zero) }
        var dragStartTime by remember { mutableStateOf(0L) }
        var accumulatedDragAmount by remember { mutableStateOf(0f) }

        val density = LocalDensity.current

        Box(
                modifier =
                        Modifier.offset(
                                        x = with(density) { (bubble.x - bubble.radius).toDp() },
                                        y = with(density) { (bubble.y - bubble.radius).toDp() }
                                )
                                .pointerInput(Unit) {
                                        detectDragGestures(
                                                onDragStart = {
                                                        onDragStart()
                                                        lastPosition = Offset(bubble.x, bubble.y)
                                                        velocity = Offset.Zero
                                                        accumulatedDragAmount = 0f
                                                },
                                                onDragEnd = {
                                                        // Scale velocity similar to React
                                                        onDragEnd(velocity.x * 5f, velocity.y * 5f)
                                                },
                                                onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val newPos = lastPosition + dragAmount

                                                        // Simple velocity
                                                        velocity = dragAmount

                                                        accumulatedDragAmount +=
                                                                dragAmount.getDistance()

                                                        lastPosition = newPos
                                                        onDrag(newPos.x, newPos.y)
                                                }
                                        )
                                }
        ) {
                BubbleItemView(
                        onClick = onClick,
                        text = bubble.text,
                        color = bubble.color,
                        size = bubble.size,
                        isCompletedToday = (bubble is HabitBubble && bubble.completedToday)
                )
        }
}

@Composable
fun BackgroundDecoration() {
        Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors =
                                                listOf(
                                                        Color(0xFF1E3A8A).copy(alpha = 0.3f),
                                                        Color.Transparent
                                                ),
                                        center = Offset(width * 0.25f, height * 0.25f),
                                        radius = width * 0.5f
                                )
                )

                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors =
                                                listOf(
                                                        Color(0xFF581C87).copy(alpha = 0.3f),
                                                        Color.Transparent
                                                ),
                                        center = Offset(width * 0.75f, height * 0.75f),
                                        radius = width * 0.5f
                                )
                )
        }
}

@Composable
fun CompletedTaskItem(task: TaskBubble, onRestore: () -> Unit, onDelete: () -> Unit) {
        androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                        androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.05f)
                        ),
                border =
                        androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.White.copy(alpha = 0.1f)
                        ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
                Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                ) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                        ) {
                                Box(
                                        modifier =
                                                Modifier.size(12.dp)
                                                        .background(task.color, CircleShape)
                                )
                                Spacer(Modifier.width(16.dp))
                                androidx.compose.material3.Text(
                                        text = task.text,
                                        color = Color.White.copy(alpha = 0.4f),
                                        style =
                                                androidx.compose.material3.MaterialTheme.typography
                                                        .bodyLarge.copy(
                                                        textDecoration =
                                                                androidx.compose.ui.text.style
                                                                        .TextDecoration.LineThrough,
                                                        fontWeight =
                                                                androidx.compose.ui.text.font
                                                                        .FontWeight.Bold
                                                )
                                )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.material3.IconButton(
                                        onClick = onRestore,
                                        modifier =
                                                Modifier.background(
                                                        Color.Transparent,
                                                        androidx.compose.foundation.shape
                                                                .RoundedCornerShape(12.dp)
                                                )
                                ) {
                                        androidx.compose.material3.Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Restore",
                                                tint = Color.Green
                                        )
                                }
                                androidx.compose.material3.IconButton(
                                        onClick = onDelete,
                                        modifier =
                                                Modifier.background(
                                                        Color.Transparent,
                                                        androidx.compose.foundation.shape
                                                                .RoundedCornerShape(12.dp)
                                                )
                                ) {
                                        androidx.compose.material3.Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red
                                        )
                                }
                        }
                }
        }
}
