package com.samarthkulkarni.minimatodo.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(viewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val state = viewModel.uiState
    val success = state.authSuccess

    if (success != null) {
        WelcomeOverlay(
            isNewAccount = success.isNewAccount,
            onFinished = { viewModel.completeAuthFlow() }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text2("M I N I M A", size = 20, bold = true, letterSpaced = true)
        Spacer(modifier = Modifier.height(4.dp))
        Text2("SIGN IN TO SYNC YOUR TASKS", size = 11, color = Color.Gray)

        Spacer(modifier = Modifier.height(40.dp))

        UnderlinedField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            placeholder = "Email",
            keyboardType = KeyboardType.Email
        )

        Spacer(modifier = Modifier.height(24.dp))

        UnderlinedField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            placeholder = "Password",
            isPassword = true,
            imeAction = ImeAction.Done,
            onDone = { viewModel.submitEmailAuth() }
        )

        state.errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text2(it, size = 12, color = Color(1f, 0.4f, 0.4f))
        }
        state.infoMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text2(it, size = 12, color = Color.White)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Primary action button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (state.isLoading) Color.DarkGray else Color.White)
                .clickable(enabled = !state.isLoading) { viewModel.submitEmailAuth() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text2("CONTINUE", size = 13, bold = true, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Google button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                .clickable(enabled = !state.isLoading) { viewModel.signInWithGoogle() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text2("CONTINUE WITH GOOGLE", size = 13, bold = true, color = Color.White)
        }
    }
}

@Composable
private fun WelcomeOverlay(isNewAccount: Boolean, onFinished: () -> Unit) {
    var phase by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        delay(3000)
        phase = 2
        delay(2000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text2(
            text = when {
                phase == 1 && isNewAccount -> "Account Created"
                phase == 1 && !isNewAccount -> "Account Found"
                isNewAccount -> "Welcome"
                else -> "Welcome Back"
            },
            size = 20,
            bold = true,
            color = Color.White
        )
    }
}

@Composable
private fun UnderlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
        cursorBrush = SolidColor(Color.White),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text2(placeholder, size = 15, color = Color.DarkGray)
                    }
                    innerTextField()
                }
                Spacer(modifier = Modifier.height(6.dp))
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

@Composable
private fun Text2(
    text: String,
    size: Int,
    bold: Boolean = false,
    color: Color = Color.White,
    letterSpaced: Boolean = false,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Text(
        text = text,
        color = color,
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        letterSpacing = if (letterSpaced) 4.sp else 0.sp,
        modifier = modifier
    )
}
