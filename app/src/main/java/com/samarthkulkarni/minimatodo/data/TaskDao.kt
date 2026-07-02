package com.samarthkulkarni.minimatodo.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isDeleted = 0 ORDER BY id DESC")
    fun getActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND isDeleted = 0 ORDER BY completedTimestamp DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Query("SELECT * FROM tasks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getTaskByRemoteId(remoteId: String): Task?

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isDeleted = 0 ORDER BY id DESC")
    suspend fun getActiveTasksDirect(): List<Task>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY id DESC")
    suspend fun getAllTasksDirect(): List<Task>

    /** Includes soft-deleted rows -- this is what the sync worker and tasks.md write-through use. */
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    suspend fun getAllIncludingDeleted(): List<Task>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun countTasks(): Int

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun purgeById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)
}
