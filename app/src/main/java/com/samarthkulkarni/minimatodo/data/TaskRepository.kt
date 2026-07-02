package com.samarthkulkarni.minimatodo.data

import kotlinx.coroutines.flow.Flow

/**
 * Room remains the fast local index for the UI and widget (synchronous Flow queries).
 * The `tasks.md` file (via [markdownStore]) is the actual persisted source of truth: every
 * mutation writes through to it, and on first run after install (or if Room's DB was cleared)
 * it hydrates Room back from the file. This same file is what the sync layer will read from /
 * write to when reconciling with Supabase.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val markdownStore: MarkdownTaskStore
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
        val newId = taskDao.insertTask(task)
        syncMarkdownFromRoom()
        return newId
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
        syncMarkdownFromRoom()
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        syncMarkdownFromRoom()
    }

    private suspend fun syncMarkdownFromRoom() {
        markdownStore.writeAll(taskDao.getAllTasksDirect())
    }
}
