package com.samarthkulkarni.minimatodo.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(context)
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val ids = manager.getGlanceIds(TodoWidget::class.java)
                    for (id in ids) {
                        TodoWidget().update(context, id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
