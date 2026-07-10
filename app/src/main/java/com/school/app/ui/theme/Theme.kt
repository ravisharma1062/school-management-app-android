package com.school.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF1D4ED8)
private val BlueDark = Color(0xFF93B4F8)
private val Teal = Color(0xFF0D9488)

private val LightColors = lightColorScheme(
    primary = Blue,
    secondary = Teal,
    surfaceVariant = Color(0xFFEEF2FA),
)

private val DarkColors = darkColorScheme(
    primary = BlueDark,
    secondary = Color(0xFF5EEAD4),
)

@Composable
fun SchoolAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
