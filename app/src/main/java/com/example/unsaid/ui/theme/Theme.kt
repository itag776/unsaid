package com.example.unsaid.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

// We define ONLY the light theme because Unsaid is a "Paper" app.
// Even if the phone is in Dark Mode, the app stays white (like a sheet of paper).

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = Color.White,

    // The most important part: The Background
    background = PaperWhite,
    onBackground = InkCharcoal,

    surface = PaperWhite,
    onSurface = InkCharcoal,

    secondary = FadedGray,
    onSecondary = InkCharcoal,
)

@Composable
fun UnsaidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // We force FALSE to keep our paper look
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Always use Light Scheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set the status bar (top bar) to our paper color
            window.statusBarColor = PaperWhite.toArgb()
            // Make the status bar icons dark (so you can see battery/time)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // This connects your Fonts (Libre/Inter)
        content = content
    )
}