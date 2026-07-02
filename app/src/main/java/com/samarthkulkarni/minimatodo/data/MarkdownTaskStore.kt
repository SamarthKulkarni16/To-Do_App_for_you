package com.samarthkulkarni.minimatodo.data

import android.content.Context
import java.io.File

/**
 * Reads/writes the full task list to a single, human-readable `tasks.md` file in the app's
 * private files directory (NOT the cache dir -- this file persists and is what actually gets
 * synced to Supabase). Room continues to serve fast reads for the UI and widget, but every
 * mutation writes through to this file, which is the real source of truth on-device.
 *
 * Format: one `## ` section per task, with metadata as a bullet list underneath.
 * Chosen over one-file-per-task because a todo list is naturally one document, the widget/sync
 * worker need to read/write the whole set at once, and a single file keeps that O(1) instead of
 * O(n) file operations.
 */
class MarkdownTaskStore(context: Context) {

    private val file = File(context.filesDir, "tasks.md")

    @Synchronized
    fun readAll(): List<Task> {
        if (!file.exists()) return emptyList()
        return parse(file.readText())
    }

    @Synchronized
    fun writeAll(tasks: List<Task>) {
        file.writeText(serialize(tasks))
    }

    private fun serialize(tasks: List<Task>): String {
        val sb = StringBuilder()
        sb.appendLine("# Minima Todo")
        sb.appendLine()
        for (task in tasks) {
            sb.appendLine("## ${escapeTitle(task.text)}")
            sb.appendLine("- id: ${task.id}")
            sb.appendLine("- remote_id: ${task.remoteId ?: ""}")
            sb.appendLine("- completed: ${task.isCompleted}")
            sb.appendLine("- start: ${task.startDate ?: ""}")
            sb.appendLine("- end: ${task.endDate ?: ""}")
            sb.appendLine("- completed_at: ${task.completedTimestamp ?: ""}")
            sb.appendLine("- updated_at: ${task.updatedAt}")
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun parse(content: String): List<Task> {
        val tasks = mutableListOf<Task>()
        val lines = content.lines()

        var title: String? = null
        var id = 0
        var remoteId: String? = null
        var completed = false
        var start: Long? = null
        var end: Long? = null
        var completedAt: Long? = null
        var updatedAt: Long = System.currentTimeMillis()

        fun flush() {
            val t = title
            if (t != null) {
                tasks.add(
                    Task(
                        id = id,
                        text = unescapeTitle(t),
                        isCompleted = completed,
                        startDate = start,
                        endDate = end,
                        completedTimestamp = completedAt,
                        remoteId = remoteId,
                        updatedAt = updatedAt
                    )
                )
            }
            title = null
            id = 0
            remoteId = null
            completed = false
            start = null
            end = null
            completedAt = null
            updatedAt = System.currentTimeMillis()
        }

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            when {
                line.startsWith("## ") -> {
                    flush()
                    title = line.removePrefix("## ").trim()
                }
                line.startsWith("- id: ") -> id = line.removePrefix("- id: ").trim().toIntOrNull() ?: 0
                line.startsWith("- remote_id: ") -> remoteId = line.removePrefix("- remote_id: ").trim().ifBlank { null }
                line.startsWith("- completed: ") -> completed = line.removePrefix("- completed: ").trim().toBoolean()
                line.startsWith("- start: ") -> start = line.removePrefix("- start: ").trim().toLongOrNull()
                line.startsWith("- end: ") -> end = line.removePrefix("- end: ").trim().toLongOrNull()
                line.startsWith("- completed_at: ") -> completedAt = line.removePrefix("- completed_at: ").trim().toLongOrNull()
                line.startsWith("- updated_at: ") -> updatedAt = line.removePrefix("- updated_at: ").trim().toLongOrNull() ?: updatedAt
            }
        }
        flush()

        return tasks
    }

    private fun escapeTitle(text: String): String = text.replace("\n", " ")
    private fun unescapeTitle(text: String): String = text
}
