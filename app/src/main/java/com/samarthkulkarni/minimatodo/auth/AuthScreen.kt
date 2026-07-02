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

@Composable
fun AuthScreen(viewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val state = viewModel.uiState

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text2("M I N I M A", size = 20, bold = true, letterSpaced = true)
        Spacer(modifier = Modifier.height(4.dp))
        Text2(
            if (state.mode == AuthMode.SIGN_IN) "SIGN IN TO SYNC YOUR TASKS" else "CREATE AN ACCOUNT",
            size = 11,
            color = Color.Gray
        )

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
            Text2(
                if (state.mode == AuthMode.SIGN_IN) "SIGN IN" else "SIGN UP",
                size = 13,
                bold = true,
                color = Color.Black
            )
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

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text2(
                if (state.mode == AuthMode.SIGN_IN) "New here? " else "Already have an account? ",
                size = 12,
                color = Color.Gray
            )
            Text2(
                if (state.mode == AuthMode.SIGN_IN) "Create account" else "Sign in",
                size = 12,
                bold = true,
                color = Color.White,
                modifier = Modifier.clickable { viewModel.toggleMode() }
            )
        }
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
