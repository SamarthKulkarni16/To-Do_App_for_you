package com.samarthkulkarni.minimatodo.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.samarthkulkarni.minimatodo.data.AppDatabase
import com.samarthkulkarni.minimatodo.data.MarkdownTaskStore
import com.samarthkulkarni.minimatodo.data.SupabaseClientProvider
import com.samarthkulkarni.minimatodo.data.SyncRepository

/**
 * Pushes/pulls tasks.md <-> Supabase. No-ops quietly if nobody is signed in yet (mandatory
 * sign-in means this should only really fire post-auth, but this guard covers races around
 * sign-out).
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = SupabaseClientProvider.auth.currentUserOrNull()?.id
            ?: return Result.success()

        val db = AppDatabase.getDatabase(applicationContext)
        val syncRepository = SyncRepository(db.taskDao(), MarkdownTaskStore(applicationContext))

        return if (syncRepository.syncNow(userId)) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
