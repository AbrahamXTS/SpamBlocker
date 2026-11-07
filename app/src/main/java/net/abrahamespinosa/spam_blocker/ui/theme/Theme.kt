package net.abrahamespinosa.spam_blocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpamBlockerColors = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF6FF7F4),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF805600),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDEA6),
    onSecondaryContainer = Color(0xFF291800),
    background = Color(0xFFF8FAF9),
    surface = Color(0xFFF8FAF9),
    surfaceVariant = Color(0xFFDAE5E3),
    onSurfaceVariant = Color(0xFF3F4947),
)

@Composable
fun SpamBlockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpamBlockerColors,
        typography = Typography(),
        content = content,
    )
}