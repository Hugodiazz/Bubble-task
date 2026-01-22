package com.devdiaz.orderless.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.devdiaz.orderless.data.model.HabitBubble
import com.devdiaz.orderless.data.model.HabitCompletion
import com.devdiaz.orderless.data.model.TaskBubble
import kotlinx.coroutines.flow.Flow

@Dao
interface BubbleDao {

    // --- Task Bubbles ---
    @Query("SELECT * FROM task_bubbles") fun getAllTasks(): Flow<List<TaskBubble>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTask(task: TaskBubble)

    @Update suspend fun updateTask(task: TaskBubble)

    @Delete suspend fun deleteTask(task: TaskBubble)

    // --- Habit Bubbles ---
    @Query("SELECT * FROM habit_bubbles") fun getAllHabits(): Flow<List<HabitBubble>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHabit(habit: HabitBubble)

    @Update suspend fun updateHabit(habit: HabitBubble)

    @Delete suspend fun deleteHabit(habit: HabitBubble)

    // --- Habit Completions ---
    @Query("SELECT * FROM habit_completions WHERE habitBubbleId = :habitId")
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun getCompletionsForDate(date: String): Flow<List<HabitCompletion>>

    @Query(
            "SELECT * FROM habit_completions WHERE habitBubbleId = :habitId AND date = :date LIMIT 1"
    )
    suspend fun getCompletion(habitId: String, date: String): HabitCompletion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletion)

    @Delete suspend fun deleteCompletion(completion: HabitCompletion)
}
