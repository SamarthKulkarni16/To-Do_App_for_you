package com.example.widget

import kotlinx.coroutines.flow.MutableStateFlow

data class WidgetTaskAnim(
    val taskId: Int,
    val progress: Float, // 0.0 to 1.0
    val stage: Int,     // 1 or 2
    val glimpseFade: Float, // 1.0 to 0.0
    val glimpseScale: Float // 1.0 to 0.0
)

object WidgetAnimationState {
    val animatingTasks = MutableStateFlow<Map<Int, WidgetTaskAnim>>(emptyMap())
}
