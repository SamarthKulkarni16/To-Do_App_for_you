package com.samarthkulkarni.minimatodo.data

import io.github.jan.supabase.postgrest.from

/**
 * Reconciles the local `tasks.md` (via Room) with the Supabase `tasks` table.
 *
 * Strategy: push local changes up first (assigning remoteId to newly-created tasks along the
 * way), then pull remote changes down, merging with last-write-wins on `updated_at`. Simple
 * on purpose -- this is a single-user personal todo list synced across a handful of the same
 * user's devices, not a multi-writer CRDT problem.
 */
class SyncRepository(
    private val taskDao: TaskDao,
    private val markdownStore: MarkdownTaskStore
) {
    private val client get() = SupabaseClientProvider.client

    /** Returns true if the whole cycle completed without any per-item failures. */
    suspend fun syncNow(userId: String): Boolean {
        var allOk = true
        allOk = push(userId) && allOk
        allOk = pull(userId) && allOk
        markdownStore.writeAll(taskDao.getAllTasksDirect())
        return allOk
    }

    private suspend fun push(userId: String): Boolean {
        var allOk = true
        val localTasks = taskDao.getAllIncludingDeleted()

        for (task in localTasks) {
            try {
                if (task.isDeleted && task.remoteId == null) {
                    // Never made it to the server -- nothing to push, just clean it up.
                    taskDao.purgeById(task.id)
                    continue
                }

                val dto = task.toDto(userId)
                val result = client.from("tasks")
                    .upsert(dto) { select() }
                    .decodeSingle<TaskDto>()

                if (task.isDeleted) {
                    // Server now has the tombstone; safe to remove the local row entirely.
                    taskDao.purgeById(task.id)
                } else if (task.remoteId == null) {
                    // First sync for this task -- remember the server-assigned UUID.
                    taskDao.updateTask(task.copy(remoteId = result.id))
                }
            } catch (e: Exception) {
                allOk = false
            }
        }
        return allOk
    }

    private suspend fun pull(userId: String): Boolean {
        return try {
            val remoteTasks = client.from("tasks")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<TaskDto>()

            for (remote in remoteTasks) {
                val remoteId = remote.id ?: continue
                val local = taskDao.getTaskByRemoteId(remoteId)
                val remoteUpdatedAtMs = remote.updatedAt.let {
                    java.time.Instant.parse(it).toEpochMilli()
                }

                when {
                    local == null && remote.deletedAt == null -> {
                        taskDao.insertTask(remote.toTask())
                    }
                    local != null && remoteUpdatedAtMs > local.updatedAt -> {
                        if (remote.deletedAt != null) {
                            taskDao.purgeById(local.id)
                        } else {
                            taskDao.updateTask(remote.toTask(localId = local.id))
                        }
                    }
                    // else: local is newer or equal -- leave as-is, it'll be pushed next cycle.
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
