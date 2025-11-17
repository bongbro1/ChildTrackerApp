package com.example.childtrackerapp.schedule.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Màu sắc cho Light Theme - Xanh dương nhạt như ảnh mẫu
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF42A5F5),           // Xanh nhạt hơn (Light Blue 400)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFFFFF),  // Trắng cho container
    onPrimaryContainer = Color(0xFF1565C0),
    secondary = Color(0xFF64B5F6),         // Xanh sáng hơn
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFFFFF), // Trắng cho secondary container
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = Color(0xFF81D4FA),          // Xanh cyan nhạt
    onTertiary = Color(0xFF01579B),
    error = Color(0xFFEF5350),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFC62828),
    background = Color(0xFFF5F7FA),        // Nền xám rất nhạt
    onBackground = Color(0xFF424242),
    surface = Color.White,
    surfaceVariant = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFEEEEEE),
    scrim = Color(0xFF000000)
)

// Màu sắc cho Dark Theme - Xanh dương đậm và tối
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),           // Xanh sáng cho dark mode
    onPrimary = Color(0xFF0D47A1),         // Xanh đậm
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF4FC3F7),
    onSecondary = Color(0xFF01579B),
    secondaryContainer = Color(0xFF0277BD),
    onSecondaryContainer = Color(0xFFB3E5FC),
    tertiary = Color(0xFF4DD0E1),
    onTertiary = Color(0xFF006064),
    error = Color(0xFFEF5350),
    onError = Color(0xFFB71C1C),
    errorContainer = Color(0xFFC62828),
    onErrorContainer = Color(0xFFFFCDD2),
    background = Color(0xFF0D1B2A),        // Nền xanh đậm
    onBackground = Color(0xFFE1E8ED),
    surface = Color(0xFF1B263B),           // Bề mặt xanh tối
    onSurface = Color(0xFFE1E8ED),
    surfaceVariant = Color(0xFF415A77),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF78909C),
    outlineVariant = Color(0xFF455A64),
    scrim = Color(0xFF000000)
)

@Composable
fun ChildTrackerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}