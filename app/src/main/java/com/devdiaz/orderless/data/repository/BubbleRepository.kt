package com.devdiaz.orderless.data.repository

import com.devdiaz.orderless.data.local.BubbleDao
import com.devdiaz.orderless.data.model.HabitBubble
import com.devdiaz.orderless.data.model.HabitCompletion
import com.devdiaz.orderless.data.model.TaskBubble
import kotlinx.coroutines.flow.Flow

class BubbleRepository(private val bubbleDao: BubbleDao) {

    val allTasks: Flow<List<TaskBubble>> = bubbleDao.getAllTasks()
    val allHabits: Flow<List<HabitBubble>> = bubbleDao.getAllHabits()

    suspend fun insertTask(task: TaskBubble) {
        bubbleDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskBubble) {
        bubbleDao.updateTask(task)
    }

    suspend fun deleteTask(task: TaskBubble) {
        bubbleDao.deleteTask(task)
    }

    suspend fun insertHabit(habit: HabitBubble) {
        bubbleDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitBubble) {
        bubbleDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: HabitBubble) {
        bubbleDao.deleteHabit(habit)
    }

    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>> {
        return bubbleDao.getCompletionsForHabit(habitId)
    }

    fun getCompletionsForDate(date: String): Flow<List<HabitCompletion>> {
        return bubbleDao.getCompletionsForDate(date)
    }

    suspend fun getCompletion(habitId: String, date: String): HabitCompletion? {
        return bubbleDao.getCompletion(habitId, date)
    }

    suspend fun insertCompletion(completion: HabitCompletion) {
        bubbleDao.insertCompletion(completion)
    }

    suspend fun deleteCompletion(completion: HabitCompletion) {
        bubbleDao.deleteCompletion(completion)
    }
}
