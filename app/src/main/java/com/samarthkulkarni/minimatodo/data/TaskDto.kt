package com.samarthkulkarni.minimatodo.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Mirrors the `tasks` table in Supabase (see docs/supabase-schema.sql). Postgrest-kt
 * (de)serializes this directly to/from the REST API using kotlinx.serialization, matching
 * columns by SerialName (snake_case) rather than the Kotlin property name.
 */
@Serializable
data class TaskDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val text: String,
    @SerialName("is_completed") val isCompleted: Boolean,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/** Epoch millis <-> ISO-8601 helpers (Supabase timestamptz columns are ISO-8601 over REST). */
private fun Long?.toIso(): String? = this?.let { Instant.ofEpochMilli(it).toString() }
private fun String?.fromIso(): Long? = this?.let { Instant.parse(it).toEpochMilli() }

fun Task.toDto(userId: String): TaskDto = TaskDto(
    id = remoteId,
    userId = userId,
    text = text,
    isCompleted = isCompleted,
    startDate = startDate.toIso(),
    endDate = endDate.toIso(),
    completedAt = completedTimestamp.toIso(),
    updatedAt = updatedAt.toIso() ?: Instant.now().toString(),
    deletedAt = if (isDeleted) (updatedAt.toIso()) else null
)

/** Converts a remote row into a local Task. [localId] should be reused when updating an
 *  existing local row (0 lets Room auto-generate one for a brand-new task). */
fun TaskDto.toTask(localId: Int = 0): Task = Task(
    id = localId,
    text = text,
    isCompleted = isCompleted,
    startDate = startDate.fromIso(),
    endDate = endDate.fromIso(),
    completedTimestamp = completedAt.fromIso(),
    remoteId = id,
    updatedAt = updatedAt.fromIso() ?: System.currentTimeMillis(),
    isDeleted = deletedAt != null
)
