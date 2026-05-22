package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.unit.ColorProvider
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.QuickAddActivity
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.worker.TaskDeadlineScheduler
import java.text.SimpleDateFormat
import java.util.*

class TodoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val db = remember { AppDatabase.getDatabase(context) }
            val activeTasksFlow = remember { db.taskDao().getActiveTasks() }
            val activeTasks by activeTasksFlow.collectAsState(initial = emptyList())
            val animatingTasks by WidgetAnimationState.animatingTasks.collectAsState()

            WidgetContent(context, activeTasks, animatingTasks)
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        tasks: List<Task>,
        animatingTasks: Map<Int, WidgetTaskAnim>
    ) {
        // Display filtering logic: normal tasks and today's tasks
        val filteredTasks = remember(tasks) {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val todayString = sdf.format(Date())
            tasks.filter { task ->
                (task.startDate == null && task.endDate == null) ||
                (task.startDate != null && sdf.format(Date(task.startDate)) == todayString) ||
                (task.endDate != null && sdf.format(Date(task.endDate)) == todayString)
            }
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
                .padding(12.dp)
        ) {
            // Widget Header
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODO",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // Minimalist "+" button that launches QuickAddActivity
                Text(
                    text = "+",
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier
                        .clickable(actionStartActivity(android.content.Intent(context, QuickAddActivity::class.java)))
                        .padding(horizontal = 8.dp)
                )
            }

            // Separator white line
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(androidx.compose.ui.graphics.Color.White)
                    .padding(bottom = 8.dp)
            ) {}

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Task List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO ACTIVE TASKS",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                            fontSize = 11.sp
                        )
                    )
                }
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    val displayList = filteredTasks.take(4) // Show up to 4 items in standard widget size
                    displayList.forEach { task ->
                        val anim = animatingTasks[task.id]
                        TaskRow(task = task, anim = anim)
                    }
                }
            }
        }
    }

    @Composable
    private fun TaskRow(task: Task, anim: WidgetTaskAnim?) {
        // Animation calculations
        val scale = anim?.glimpseScale ?: 1.0f
        val alpha = anim?.glimpseFade ?: 1.0f
        val progress = anim?.progress ?: 0.0f

        val fontSize = (13f * scale).sp
        val padHorizontal = ((1f - scale) * 24f).dp
        val padVertical = ((1f - scale) * 12f).dp

        val textColor = androidx.compose.ui.graphics.Color(1f, 1f, 1f, alpha)

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(
                    horizontal = padHorizontal,
                    vertical = padVertical + 4.dp
                )
                .clickable(actionRunCallback<CompleteTaskAction>(actionParametersOf(taskIdKey to task.id)))
        ) {
            Box(
                modifier = GlanceModifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                // Task Text
                Text(
                    text = if (task.endDate != null) "•  ${task.text} (H)" else "•  ${task.text}",
                    style = TextStyle(
                        color = ColorProvider(textColor),
                        fontSize = fontSize
                    ),
                    modifier = GlanceModifier.padding(end = 16.dp)
                )

                // Strike-through line (grows during Stage 1 and scales/fades in Stage 2)
                if (anim != null) {
                    val strikeWidth = (140 * progress * scale).dp
                    val lineAlpha = alpha
                    val lineStrokeColor = androidx.compose.ui.graphics.Color(1f, 1f, 1f, lineAlpha)
                    Box(
                        modifier = GlanceModifier
                            .width(strikeWidth)
                            .height(1.dp)
                            .background(lineStrokeColor)
                    ) {}
                }
            }
        }
    }

    companion object {
        val taskIdKey = ActionParameters.Key<Int>("task_id")
    }
}

class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TodoWidget.taskIdKey] ?: return

        // Stage 1 (Strike-through) starts: 2.0s duration
        val steps = 10
        WidgetAnimationState.animatingTasks.value = WidgetAnimationState.animatingTasks.value + (taskId to WidgetTaskAnim(
            taskId = taskId,
            progress = 0.0f,
            stage = 1,
            glimpseFade = 1.0f,
            glimpseScale = 1.0f
        ))
        
        TodoWidget().update(context, glanceId)

        for (i in 1..steps) {
            kotlinx.coroutines.delay(200) // 200ms * 10 = 2000ms (Exactly 2.0s!)
            val progressVal = i.toFloat() / steps
            WidgetAnimationState.animatingTasks.value = WidgetAnimationState.animatingTasks.value.toMutableMap().apply {
                if (containsKey(taskId)) {
                    this[taskId] = this[taskId]!!.copy(progress = progressVal)
                }
            }
            TodoWidget().update(context, glanceId)
        }

        // Stage 2: rapid scale downward (Z-Axis Fading) & alpha fade - 300ms total
        WidgetAnimationState.animatingTasks.value = WidgetAnimationState.animatingTasks.value.toMutableMap().apply {
            if (containsKey(taskId)) {
                this[taskId] = this[taskId]!!.copy(stage = 2, progress = 1.0f)
            }
        }
        TodoWidget().update(context, glanceId)

        val stage2Steps = 3
        for (i in 1..stage2Steps) {
            kotlinx.coroutines.delay(100) // 100ms * 3 = 300ms
            val progressPct = i.toFloat() / stage2Steps
            val alphaVal = 1.0f - progressPct
            val scaleVal = 1.0f - progressPct
            WidgetAnimationState.animatingTasks.value = WidgetAnimationState.animatingTasks.value.toMutableMap().apply {
                if (containsKey(taskId)) {
                    this[taskId] = this[taskId]!!.copy(
                        glimpseFade = alphaVal.coerceIn(0f, 1f),
                        glimpseScale = scaleVal.coerceIn(0.01f, 1f)
                    )
                }
            }
            TodoWidget().update(context, glanceId)
        }

        // Stage 3 (The Shift): Save task completed in Room, remove animation state, refresh layout
        val db = AppDatabase.getDatabase(context)
        val task = db.taskDao().getTaskById(taskId)
        if (task != null) {
            db.taskDao().updateTask(
                task.copy(
                    isCompleted = true,
                    completedTimestamp = System.currentTimeMillis()
                )
            )
            TaskDeadlineScheduler.cancelDeadlineNotification(context, taskId)
        }

        WidgetAnimationState.animatingTasks.value = WidgetAnimationState.animatingTasks.value - taskId
        TodoWidget().update(context, glanceId)
        
        // Broadcast updates to all widgets to slide remaining tasks
        TodoWidgetReceiver.updateAllWidgets(context)
    }
}
