package com.samarthkulkarni.minimatodo.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val activeTasks: Flow<List<Task>> = taskDao.getActiveTasks()
    val completedTasks: Flow<List<Task>> = taskDao.getCompletedTasks()

    suspend fun getTaskById(id: Int): Task? = taskDao.getTaskById(id)

    suspend fun getActiveTasksDirect(): List<Task> = taskDao.getActiveTasksDirect()

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
}
