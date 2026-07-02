package com.samarthkulkarni.minimatodo.data

import android.content.Context
import com.samarthkulkarni.minimatodo.worker.SyncScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Room remains the fast local index for the UI and widget (synchronous Flow queries).
 * The `tasks.md` file (via [markdownStore]) is the actual persisted source of truth: every
 * mutation writes through to it, and on first run after install (or if Room's DB was cleared)
 * it hydrates Room back from the file. Every mutation also nudges [SyncScheduler] for a
 * near-instant background sync to Supabase while online.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val markdownStore: MarkdownTaskStore,
    private val context: Context
) {
    val activeTasks: Flow<List<Task>> = taskDao.getActiveTasks()
    val completedTasks: Flow<List<Task>> = taskDao.getCompletedTasks()

    /** Call once at app startup. Rebuilds Room from tasks.md if Room is empty but the file isn't. */
    suspend fun hydrateFromMarkdownIfNeeded() {
        if (taskDao.countTasks() == 0) {
            val fileTasks = markdownStore.readAll()
            if (fileTasks.isNotEmpty()) {
                taskDao.insertAll(fileTasks)
            }
        }
    }

    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)

    suspend fun getActiveTasksDirect(): List<Task> = taskDao.getActiveTasksDirect()

    suspend fun insertTask(task: Task): Long {
        val newId = taskDao.insertTask(task.copy(updatedAt = System.currentTimeMillis()))
        syncMarkdownFromRoom()
        return newId
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
        syncMarkdownFromRoom()
    }

    /** Soft delete: hidden from the UI immediately, but kept until the sync worker confirms
     *  the deletion reached Supabase (or confirms the task was never synced), then purged. */
    suspend fun deleteTask(task: Task) {
        taskDao.updateTask(task.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        syncMarkdownFromRoom()
    }

    private suspend fun syncMarkdownFromRoom() {
        markdownStore.writeAll(taskDao.getAllTasksDirect())
        SyncScheduler.requestImmediateSync(context)
    }
}
