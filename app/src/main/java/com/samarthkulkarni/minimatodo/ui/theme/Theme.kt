package com.samarthkulkarni.minimatodo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val MonochromeColorScheme = darkColorScheme(
    primary = SolidWhite,
    onPrimary = AmoledBlack,
    secondary = SolidWhite,
    onSecondary = AmoledBlack,
    background = AmoledBlack,
    onBackground = SolidWhite,
    surface = AmoledBlack,
    onSurface = SolidWhite,
    error = SolidWhite,
    onError = AmoledBlack
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MonochromeColorScheme,
        typography = Typography,
        content = content
    )
}
