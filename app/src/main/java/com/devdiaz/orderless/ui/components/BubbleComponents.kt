package com.devdiaz.orderless.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devdiaz.orderless.data.model.BubbleColors
import com.devdiaz.orderless.data.model.Priority
import com.devdiaz.orderless.viewmodel.BubbleFilter

@Composable
fun ExpandableFilterFab(currentFilter: BubbleFilter, onFilterSelected: (BubbleFilter) -> Unit) {
        var expanded by remember { mutableStateOf(false) }

        Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
        ) {
                androidx.compose.animation.AnimatedVisibility(
                        visible = expanded,
                        enter =
                                androidx.compose.animation.fadeIn() +
                                        androidx.compose.animation.slideInVertically(
                                                initialOffsetY = { it / 2 }
                                        ),
                        exit =
                                androidx.compose.animation.fadeOut() +
                                        androidx.compose.animation.slideOutVertically(
                                                targetOffsetY = { it / 2 }
                                        )
                ) {
                        Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                FilterOptionFab(
                                        text = "Completadas",
                                        icon = Icons.Default.CheckCircle,
                                        isSelected = currentFilter == BubbleFilter.COMPLETED,
                                        onClick = {
                                                onFilterSelected(BubbleFilter.COMPLETED)
                                                expanded = false
                                        }
                                )
                                FilterOptionFab(
                                        text = "Hábitos",
                                        icon = Icons.Default.Bolt,
                                        isSelected = currentFilter == BubbleFilter.HABITS,
                                        onClick = {
                                                onFilterSelected(BubbleFilter.HABITS)
                                                expanded = false
                                        }
                                )
                                FilterOptionFab(
                                        text = "Tareas",
                                        icon = Icons.AutoMirrored.Filled.List,
                                        isSelected = currentFilter == BubbleFilter.ACTIVE,
                                        onClick = {
                                                onFilterSelected(BubbleFilter.ACTIVE)
                                                expanded = false
                                        }
                                )
                        }
                }

                SmallFloatingActionButton(
                        onClick = { expanded = !expanded },
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                ) {
                        Icon(
                                if (expanded) Icons.Default.Close else Icons.Default.FilterList,
                                contentDescription = "Filter",
                                modifier = Modifier.size(24.dp)
                        )
                }
        }
}

@Composable
fun FilterOptionFab(
        text: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        isSelected: Boolean,
        onClick: () -> Unit
) {
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.clickable { onClick() }
        ) {
                Text(
                        text = text,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier =
                                Modifier.background(
                                                Color.Black.copy(alpha = 0.6f),
                                                RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(Modifier.width(12.dp))
                SmallFloatingActionButton(
                        onClick = onClick,
                        containerColor =
                                if (isSelected) Color(0xFF6366F1)
                                else Color.White, // Indigo 500 if selected
                        contentColor = if (isSelected) Color.White else Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                ) { Icon(icon, contentDescription = text, modifier = Modifier.size(20.dp)) }
        }
}

@Composable
fun BubbleItemView(
        onClick: () -> Unit,
        text: String,
        color: Color,
        size: Float,
        isCompletedToday: Boolean,
        modifier: Modifier = Modifier
) {
        Box(
                modifier =
                        modifier.size(size.dp)
                                .shadow(24.dp, CircleShape, ambientColor = color, spotColor = color)
                                .background(color, CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                .clip(CircleShape)
                                .clickable { onClick() },
                contentAlignment = Alignment.Center
        ) {
                // Shine effect
                Box(
                        modifier =
                                Modifier.align(Alignment.TopStart)
                                        .offset(x = (size * 0.15f).dp, y = (size * 0.15f).dp)
                                        .size((size * 0.25f).dp, (size * 0.15f).dp)
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                )

                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                ) {
                        if (isCompletedToday) {
                                Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                )
                        }
                        Text(
                                text = text,
                                color = Color(0xFF0F172A), // Slate 900
                                fontWeight = FontWeight.Black,
                                fontSize = if (size < 100) 10.sp else 12.sp,
                                lineHeight = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                }
        }
}

@Composable
fun CreationDialog(
        onDismiss: () -> Unit,
        onCreateTask: (String, Priority, Color) -> Unit,
        onCreateHabit: (String, List<Int>, Color) -> Unit
) {
        var itemType by remember { mutableStateOf("task") } // "task" or "habit"
        var inputValue by remember { mutableStateOf("") }
        var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
        var selectedColor by remember { mutableStateOf(BubbleColors.Blue400) }
        var selectedDays by remember { mutableStateOf((0..6).toList()) }

        Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
                Card(
                        modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = Color(0xFF0F172A)
                                ), // Slate 900
                        shape = RoundedCornerShape(32.dp),
                        border =
                                androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.1f)
                                )
                ) {
                        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                                // Type Selector
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .background(
                                                                Color.Black.copy(alpha = 0.4f),
                                                                RoundedCornerShape(24.dp)
                                                        )
                                                        .padding(4.dp)
                                ) {
                                        TypeButton(
                                                text = "Tarea",
                                                isSelected = itemType == "task",
                                                modifier = Modifier.weight(1f),
                                                onClick = { itemType = "task" }
                                        )
                                        TypeButton(
                                                text = "Hábito",
                                                isSelected = itemType == "habit",
                                                activeColor = Color(0xFF6366F1), // Indigo 500
                                                modifier = Modifier.weight(1f),
                                                onClick = { itemType = "habit" }
                                        )
                                }

                                Spacer(Modifier.height(24.dp))

                                // Name Input
                                Text(
                                        "NOMBRE",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                TextField(
                                        value = inputValue,
                                        onValueChange = { inputValue = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors =
                                                TextFieldDefaults.colors(
                                                        focusedContainerColor =
                                                                Color.White.copy(alpha = 0.05f),
                                                        unfocusedContainerColor =
                                                                Color.White.copy(alpha = 0.05f),
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White
                                                ),
                                        shape = RoundedCornerShape(16.dp),
                                        placeholder = {
                                                Text("...", color = Color.White.copy(alpha = 0.2f))
                                        }
                                )

                                Spacer(Modifier.height(24.dp))

                                // Task Priority
                                if (itemType == "task") {
                                        Text(
                                                "PRIORIDAD",
                                                color = Color.White.copy(alpha = 0.3f),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .background(
                                                                        Color.Black.copy(
                                                                                alpha = 0.4f
                                                                        ),
                                                                        RoundedCornerShape(12.dp)
                                                                )
                                                                .padding(4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                                Priority.values().forEach { priority ->
                                                        val isSelected =
                                                                selectedPriority == priority
                                                        Button(
                                                                onClick = {
                                                                        selectedPriority = priority
                                                                },
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .height(40.dp),
                                                                colors =
                                                                        ButtonDefaults.buttonColors(
                                                                                containerColor =
                                                                                        if (isSelected
                                                                                        )
                                                                                                Color.White
                                                                                        else
                                                                                                Color.Transparent,
                                                                                contentColor =
                                                                                        if (isSelected
                                                                                        )
                                                                                                Color.Black
                                                                                        else
                                                                                                Color.White
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.3f
                                                                                                        )
                                                                        ),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(0.dp)
                                                        ) {
                                                                Text(
                                                                        priority.label.uppercase(),
                                                                        fontSize = 9.sp,
                                                                        fontWeight =
                                                                                FontWeight.Black
                                                                )
                                                        }
                                                }
                                        }
                                } else {
                                        // Habit Days
                                        Text(
                                                "DÍAS DE LA SEMANA",
                                                color = Color.White.copy(alpha = 0.3f),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                val weekDays =
                                                        listOf("D", "L", "M", "M", "J", "V", "S")
                                                weekDays.forEachIndexed { index, label ->
                                                        val isSelected =
                                                                selectedDays.contains(index)
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(32.dp)
                                                                                .background(
                                                                                        if (isSelected
                                                                                        )
                                                                                                Color(
                                                                                                        0xFF6366F1
                                                                                                )
                                                                                        else
                                                                                                Color.White
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.05f
                                                                                                        ),
                                                                                        CircleShape
                                                                                )
                                                                                .border(
                                                                                        1.dp,
                                                                                        if (isSelected
                                                                                        )
                                                                                                Color(
                                                                                                        0xFF818CF8
                                                                                                )
                                                                                        else
                                                                                                Color.White
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.1f
                                                                                                        ),
                                                                                        CircleShape
                                                                                )
                                                                                .clip(CircleShape)
                                                                                .clickable {
                                                                                        selectedDays =
                                                                                                if (backgroundSelectedDays(
                                                                                                                selectedDays,
                                                                                                                index
                                                                                                        )
                                                                                                ) {
                                                                                                        selectedDays -
                                                                                                                index
                                                                                                } else {
                                                                                                        selectedDays +
                                                                                                                index
                                                                                                }
                                                                                },
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Text(
                                                                        label,
                                                                        color =
                                                                                if (isSelected)
                                                                                        Color.White
                                                                                else
                                                                                        Color.White
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.3f
                                                                                                ),
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }
                                                }
                                        }
                                }

                                Spacer(Modifier.height(24.dp))

                                // Colors
                                Text(
                                        "COLOR",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        BubbleColors.All.forEach { color ->
                                                val isSelected = selectedColor == color
                                                Box(
                                                        modifier =
                                                                Modifier.size(28.dp)
                                                                        .scale(
                                                                                if (isSelected)
                                                                                        1.25f
                                                                                else 1f
                                                                        )
                                                                        .background(
                                                                                color,
                                                                                CircleShape
                                                                        )
                                                                        .border(
                                                                                if (isSelected) 2.dp
                                                                                else 0.dp,
                                                                                Color.White,
                                                                                CircleShape
                                                                        )
                                                                        .clickable {
                                                                                selectedColor =
                                                                                        color
                                                                        }
                                                )
                                        }
                                }

                                Spacer(Modifier.height(32.dp))

                                // Submit Button
                                Button(
                                        onClick = {
                                                if (inputValue.isNotBlank()) {
                                                        if (itemType == "task") {
                                                                onCreateTask(
                                                                        inputValue,
                                                                        selectedPriority,
                                                                        selectedColor
                                                                )
                                                        } else {
                                                                onCreateHabit(
                                                                        inputValue,
                                                                        selectedDays,
                                                                        selectedColor
                                                                )
                                                        }
                                                        onDismiss() // Dialog closes after creating
                                                }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(60.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                if (itemType == "task")
                                                                        Color(0xFF2563EB)
                                                                else Color(0xFF4F46E5)
                                                ),
                                        shape = RoundedCornerShape(24.dp)
                                ) {
                                        Text(
                                                if (itemType == "task") "Lanzar" else "Crear",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Icon(
                                                if (itemType == "task") Icons.Default.Check
                                                else Icons.Default.Bolt,
                                                contentDescription = null
                                        )
                                }
                        }
                }
        }
}

@Composable
fun TypeButton(
        text: String,
        isSelected: Boolean,
        modifier: Modifier = Modifier,
        activeColor: Color = Color.White,
        onClick: () -> Unit
) {
        Button(
                onClick = onClick,
                modifier = modifier.padding(horizontal = 2.dp),
                colors =
                        ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) activeColor else Color.Transparent,
                                contentColor =
                                        if (isSelected)
                                                (if (activeColor == Color.White) Color.Black
                                                else Color.White)
                                        else Color.White.copy(alpha = 0.3f)
                        ),
                shape = RoundedCornerShape(24.dp)
        ) {
                Text(
                        text.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                )
        }
}

@Composable
fun BubbleActionMenu(
        bubbleText: String,
        onComplete: () -> Unit,
        onDelete: () -> Unit,
        onDismiss: () -> Unit
) {
        Dialog(onDismissRequest = onDismiss) {
                Card(
                        modifier = Modifier.width(280.dp).wrapContentHeight(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(24.dp),
                        border =
                                androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color.White.copy(alpha = 0.1f)
                                )
                ) {
                        Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Text(
                                        text = bubbleText,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 2,
                                        modifier = Modifier.padding(bottom = 24.dp)
                                )

                                Button(
                                        onClick = {
                                                onComplete()
                                                onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF22C55E)
                                                ), // Green
                                        shape = RoundedCornerShape(12.dp)
                                ) {
                                        Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Marcar completado", fontWeight = FontWeight.Bold)
                                }

                                Spacer(Modifier.height(12.dp))

                                Button(
                                        onClick = {
                                                onDelete()
                                                onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFEF4444)
                                                ), // Red
                                        shape = RoundedCornerShape(12.dp)
                                ) {
                                        Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Eliminar", fontWeight = FontWeight.Bold)
                                }

                                Spacer(Modifier.height(12.dp))

                                TextButton(
                                        onClick = onDismiss,
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) { Text("Cancelar", color = Color.White.copy(alpha = 0.5f)) }
                        }
                }
        }
}

private fun backgroundSelectedDays(current: List<Int>, index: Int): Boolean {
        return current.contains(index)
}
