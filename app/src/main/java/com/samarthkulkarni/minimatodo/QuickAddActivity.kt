package com.samarthkulkarni.minimatodo

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.samarthkulkarni.minimatodo.data.AppDatabase
import com.samarthkulkarni.minimatodo.data.MarkdownTaskStore
import com.samarthkulkarni.minimatodo.data.Task
import com.samarthkulkarni.minimatodo.widget.TodoWidgetReceiver
import com.samarthkulkarni.minimatodo.worker.SyncScheduler
import kotlinx.coroutines.launch

class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var textState by remember { mutableStateOf("") }
            val db = remember { AppDatabase.getDatabase(applicationContext) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA000000)) // Translucent overlay over home screen
                    .clickable { finish() },
                contentAlignment = Alignment.Center
            ) {
                // Main Dialog panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(Color.Black, shape = RoundedCornerShape(4.dp))
                        .border(1.dp, Color.White, shape = RoundedCornerShape(4.dp))
                        .clickable(enabled = false) {}
                        .padding(20.dp)
                ) {
                    Text(
                        text = "QUICK ADD TASK",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Minimal custom underlined text field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        BasicTextField(
                            value = textState,
                            onValueChange = { textState = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(Color.White),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (textState.trim().isNotEmpty()) {
                                    saveAndExit(db, textState.trim())
                                }
                            }),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                Column {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (textState.isEmpty()) {
                                            Text(
                                                text = "Type task...",
                                                color = Color.Gray,
                                                fontSize = 16.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    // Underline
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

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "CANCEL",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { finish() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ADD",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    if (textState.trim().isNotEmpty()) {
                                        saveAndExit(db, textState.trim())
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Request auto focus keyboard
            LaunchedEffect(Unit) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            }
        }
    }

    private fun saveAndExit(db: AppDatabase, text: String) {
        lifecycleScope.launch {
            db.taskDao().insertTask(
                Task(
                    text = text,
                    isCompleted = false,
                    startDate = null,
                    endDate = null,
                    completedTimestamp = null
                )
            )
            // Write through to tasks.md so the file stays the source of truth for sync.
            MarkdownTaskStore(applicationContext).writeAll(db.taskDao().getAllTasksDirect())
            SyncScheduler.requestImmediateSync(applicationContext)
            // Refresh home widgets immediately
            TodoWidgetReceiver.updateAllWidgets(applicationContext)
            finish()
        }
    }
}
