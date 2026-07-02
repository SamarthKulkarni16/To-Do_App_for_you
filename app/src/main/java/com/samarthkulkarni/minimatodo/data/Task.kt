package com.samarthkulkarni.minimatodo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isCompleted: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val completedTimestamp: Long? = null,
    /** Supabase row UUID once this task has been synced at least once. Null = never synced. */
    val remoteId: String? = null,
    /** Local wall-clock ms of the last change to this task. Used for last-write-wins sync merges. */
    val updatedAt: Long = System.currentTimeMillis(),
    /** Soft-delete flag. Deleted tasks are hidden from the UI but kept until the sync worker
     *  has confirmed the deletion on Supabase, then purged locally. */
    val isDeleted: Boolean = false
)
