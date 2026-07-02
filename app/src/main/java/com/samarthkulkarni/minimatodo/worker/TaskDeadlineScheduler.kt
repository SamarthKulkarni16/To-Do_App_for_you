package com.samarthkulkarni.minimatodo.worker

import android.content.Context
import androidx.work.*
import com.samarthkulkarni.minimatodo.data.Task
import java.util.concurrent.TimeUnit

object TaskDeadlineScheduler {

    fun scheduleDeadlineNotification(context: Context, task: Task) {
        val endDate = task.endDate ?: return
        val taskId = task.id

        // Target notification time: exactly 1 hour (3600000 ms) before endDate
        val targetTimeMs = endDate - 3600000L
        val currentTimeMs = System.currentTimeMillis()
        val delayMs = targetTimeMs - currentTimeMs

        if (delayMs > 0) {
            val data = Data.Builder()
                .putInt("TASK_ID", taskId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<TaskDeadlineWorker>()
                .setInputData(data)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag("task_deadline_$taskId")
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "task_deadline_$taskId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        } else {
            // Cancel any existing if the deadline is now too close or in the past
            cancelDeadlineNotification(context, taskId)
        }
    }

    fun cancelDeadlineNotification(context: Context, taskId: Int) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork("task_deadline_$taskId")
    }
}
