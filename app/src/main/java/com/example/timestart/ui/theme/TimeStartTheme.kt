package com.example.timestart.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val ReferenceLightColors = lightColorScheme(
    primary = Color(0xFF167A43),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8F4E0),
    onPrimaryContainer = Color(0xFF00391A),
    secondary = Color(0xFF466152),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1F4E6),
    onSecondaryContainer = Color(0xFF10261A),
    tertiary = Color(0xFF4C3B98),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE9E0FF),
    onTertiaryContainer = Color(0xFF231452),
    background = Color(0xFFFAF6FD),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFAF6FD),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF2EEF6),
    onSurfaceVariant = Color(0xFF625B66),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF6F5F8),
    outline = Color(0xFF807781),
    error = Color(0xFFBA1A1A),
)

@Composable
fun TimeStartTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else ReferenceLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        ),
        typography = Typography().run {
            copy(
                headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
                titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
                labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        },
        content = content,
    )
}
