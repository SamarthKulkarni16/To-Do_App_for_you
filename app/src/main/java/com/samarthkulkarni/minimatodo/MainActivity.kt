package com.samarthkulkarni.minimatodo

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samarthkulkarni.minimatodo.auth.AuthScreen
import com.samarthkulkarni.minimatodo.auth.AuthViewModel
import com.samarthkulkarni.minimatodo.data.AppDatabase
import com.samarthkulkarni.minimatodo.data.Task
import com.samarthkulkarni.minimatodo.data.TaskRepository
import com.samarthkulkarni.minimatodo.data.MarkdownTaskStore
import com.samarthkulkarni.minimatodo.ui.theme.MyApplicationTheme
import com.samarthkulkarni.minimatodo.worker.TaskDeadlineScheduler
import com.samarthkulkarni.minimatodo.worker.SyncScheduler
import com.samarthkulkarni.minimatodo.widget.TodoWidgetReceiver
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authViewModel.handleDeeplink(intent)
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    containerColor = Color.Black
                ) { innerPadding ->
                    val sessionStatus by authViewModel.sessionStatus.collectAsStateWithLifecycle()

                    LaunchedEffect(sessionStatus) {
                        when (sessionStatus) {
                            is SessionStatus.Authenticated -> {
                                SyncScheduler.schedulePeriodicSync(applicationContext)
                                SyncScheduler.requestImmediateSync(applicationContext)
                            }
                            is SessionStatus.NotAuthenticated -> {
                                SyncScheduler.cancelPeriodicSync(applicationContext)
                            }
                            else -> Unit
                        }
                    }

                    when (sessionStatus) {
                        is SessionStatus.Authenticated -> {
                            MainScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                onSignOut = { authViewModel.signOut() }
                            )
                        }
                        is SessionStatus.Initializing -> {
                            // Session is still being restored from storage; avoid an auth-screen flash.
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                        }
                        else -> {
                            AuthScreen(
                                viewModel = authViewModel,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        authViewModel.handleDeeplink(intent)
    }
}

// VM architectural block representing local interactions
class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = TaskRepository(db.taskDao(), MarkdownTaskStore(application), application)

    val activeTasks = repository.activeTasks
    val completedTasks = repository.completedTasks

    init {
        viewModelScope.launch {
            repository.hydrateFromMarkdownIfNeeded()
        }
    }

    fun addTask(text: String, startDate: Long?, endDate: Long?) {
        viewModelScope.launch {
            val task = Task(text = text, isCompleted = false, startDate = startDate, endDate = endDate)
            val newId = repository.insertTask(task)
            val insertedTask = task.copy(id = newId.toInt())

            if (endDate != null) {
                TaskDeadlineScheduler.scheduleDeadlineNotification(getApplication(), insertedTask)
            }
            TodoWidgetReceiver.updateAllWidgets(getApplication())
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = true,
                completedTimestamp = System.currentTimeMillis()
            )
            repository.updateTask(updated)
            TaskDeadlineScheduler.cancelDeadlineNotification(getApplication(), task.id)
            TodoWidgetReceiver.updateAllWidgets(getApplication())
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            TaskDeadlineScheduler.cancelDeadlineNotification(getApplication(), task.id)
            TodoWidgetReceiver.updateAllWidgets(getApplication())
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = viewModel(),
    onSignOut: () -> Unit = {}
) {
    val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val completedTasks by viewModel.completedTasks.collectAsStateWithLifecycle(initialValue = emptyList())

    var currentTab by remember { mutableStateOf("TODO") } // "TODO" or "HISTORY"
    var inputText by remember { mutableStateOf("") }

    // Scheduling states inside text entry
    var scheduledStartDate by remember { mutableStateOf<Long?>(null) }
    var scheduledEndDate by remember { mutableStateOf<Long?>(null) }
    var showScheduleOverlay by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Detect triple-tap on blank screen areas
    var tripleTapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    val onBlankAreaTap = {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < 1500) {
            tripleTapCount++
        } else {
            tripleTapCount = 1
        }
        lastTapTime = now

        if (tripleTapCount >= 3) {
            currentTab = if (currentTab == "TODO") "HISTORY" else "TODO"
            tripleTapCount = 0
            Toast.makeText(context, "Revealing ${if (currentTab == "TODO") "Active Tasks" else "Task History"}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(horizontal = 16.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                onBlankAreaTap()
            }
    ) {
        // App Title Header
        Text(
            text = "M I N I M A",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp, bottom = 12.dp)
        )

        // Active screens panel (Navigation tab panel row completely hidden as instructed)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    onBlankAreaTap()
                }
        ) {
            if (currentTab == "TODO") {
                ActiveTasksScreen(
                    tasks = activeTasks,
                    onComplete = { viewModel.completeTask(it) },
                    onDelete = { viewModel.deleteTask(it) },
                    onBlankAreaTap = onBlankAreaTap
                )
            } else {
                HistoryScreen(
                    tasks = completedTasks,
                    onDelete = { viewModel.deleteTask(it) },
                    onBlankAreaTap = onBlankAreaTap
                )
            }
        }

        // Add task footer (visible only on Active screen)
        if (currentTab == "TODO") {
            Spacer(modifier = Modifier.height(8.dp))

            // Display active scheduling summary if set
            if (scheduledStartDate != null || scheduledEndDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        scheduledStartDate?.let {
                            Text(
                                text = "Start: ${formatDisplayDate(it)}",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                        scheduledEndDate?.let {
                            Text(
                                text = "End: ${formatDisplayDate(it)}",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = "CLEAR",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                scheduledStartDate = null
                                scheduledEndDate = null
                                Toast.makeText(context, "Schedule Cleared", Toast.LENGTH_SHORT).show()
                            }
                            .padding(4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Input TextField underlined
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (inputText.trim().isNotEmpty()) {
                                viewModel.addTask(
                                    inputText.trim(),
                                    scheduledStartDate,
                                    scheduledEndDate
                                )
                                inputText = ""
                                scheduledStartDate = null
                                scheduledEndDate = null
                            }
                        }),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Column {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (inputText.isEmpty()) {
                                        Text(
                                            text = "Enter task...",
                                            color = Color.DarkGray,
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // White underlined bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White)
                                )
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Clock icon toggle styled with custom Canvas
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { showScheduleOverlay = !showScheduleOverlay }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        // Clock outline circle
                        drawCircle(
                            color = Color.White,
                            radius = size.minDimension / 2,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        // Hours hand pointing up-right
                        drawLine(
                            color = Color.White,
                            start = center,
                            end = Offset(center.x, center.y - 6.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        // Minutes hand pointing right
                        drawLine(
                            color = Color.White,
                            start = center,
                            end = Offset(center.x + 4.dp.toPx(), center.y),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Schedule Active dot indicator
                    if (scheduledStartDate != null || scheduledEndDate != null) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White, shape = RoundedCornerShape(3.dp))
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }
    }

    // Schedule overlay popup custom layout
    if (showScheduleOverlay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xBB000000))
                .clickable { showScheduleOverlay = false },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                // Decorative top border line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SCHEDULING OPTIONS",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Start date picker row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSystemDateTimePicker(context) { timestamp ->
                                scheduledStartDate = timestamp
                            }
                        }
                        .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "START DATE/TIME",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = scheduledStartDate?.let { formatDisplayDate(it) } ?: "NOT SET",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // End date picker row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSystemDateTimePicker(context) { timestamp ->
                                scheduledEndDate = timestamp
                            }
                        }
                        .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "END DATE/TIME",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = scheduledEndDate?.let { formatDisplayDate(it) } ?: "NOT SET",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CLEAR ALL",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                scheduledStartDate = null
                                scheduledEndDate = null
                                showScheduleOverlay = false
                                Toast.makeText(context, "Schedule Cleared", Toast.LENGTH_SHORT).show()
                            }
                            .padding(8.dp)
                    )

                    Text(
                        text = "CONFIRM",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { showScheduleOverlay = false }
                            .padding(8.dp)
                    )
                }
            }
        }
    } else {
        // Sign-out footer, visible only on the History tab (kept out of the way of the main flow).
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SIGN OUT",
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clickable { onSignOut() }
                        .padding(8.dp)
                )
            }
        }
    }

@Composable
fun ActiveTasksScreen(
    tasks: List<Task>,
    onComplete: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onBlankAreaTap: () -> Unit = {}
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onBlankAreaTap() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NO ACTIVE TASKS",
                color = Color.DarkGray,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onBlankAreaTap() }
        ) {
            items(tasks, key = { it.id }) { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom monochrome Checkbox
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(1.dp, Color.White)
                            .clickable { onComplete(task) }
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.text,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (task.startDate != null || task.endDate != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val scheduleStr = buildString {
                                task.startDate?.let { append("Start: ${formatDisplayDate(it)}   ") }
                                task.endDate?.let { append("Deadline: ${formatDisplayDate(it)}") }
                            }
                            Text(
                                text = scheduleStr,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Minimal DELETE selector
                    Text(
                        text = "DELETE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onDelete(task) }
                            .padding(4.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color(0xFF222222))
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(
    tasks: List<Task>,
    onDelete: (Task) -> Unit,
    onBlankAreaTap: () -> Unit = {}
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onBlankAreaTap() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NO COMPLETED HISTORY",
                color = Color.DarkGray,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onBlankAreaTap() }
        ) {
            items(tasks, key = { it.id }) { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.text,
                            color = Color.Gray,
                            fontSize = 15.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val completionTime = task.completedTimestamp ?: System.currentTimeMillis()
                        Text(
                            text = "Completed: ${formatDisplayDate(completionTime)}",
                            color = Color.DarkGray,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "REMOVE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onDelete(task) }
                            .padding(4.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color(0xFF222222))
                )
            }
        }
    }
}

// System Date-Picker Invocation Helper
private fun showSystemDateTimePicker(context: Context, onResult: (Long) -> Unit) {
    val current = Calendar.getInstance()
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            current.set(Calendar.YEAR, year)
            current.set(Calendar.MONTH, month)
            current.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            android.app.TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    current.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    current.set(Calendar.MINUTE, minute)
                    current.set(Calendar.SECOND, 0)
                    current.set(Calendar.MILLISECOND, 0)
                    onResult(current.timeInMillis)
                },
                current.get(Calendar.HOUR_OF_DAY),
                current.get(Calendar.MINUTE),
                false
            ).show()
        },
        current.get(Calendar.YEAR),
        current.get(Calendar.MONTH),
        current.get(Calendar.DAY_OF_MONTH)
    ).show()
}

// Display date formatting helper
private fun formatDisplayDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

