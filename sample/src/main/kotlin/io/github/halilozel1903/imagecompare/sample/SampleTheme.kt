package io.github.halilozel1903.imagecompare.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** A blue theme for the Image Compare sample. */
@Composable
fun SampleTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFFAEC6FF),
            onPrimary = Color(0xFF002E6A),
            primaryContainer = Color(0xFF124397),
            onPrimaryContainer = Color(0xFFD8E2FF),
            secondary = Color(0xFFBFC6DC),
            secondaryContainer = Color(0xFF3F4759),
            onSecondaryContainer = Color(0xFFDBE2F9),
            tertiary = Color(0xFFFFB68B),
            tertiaryContainer = Color(0xFF6B3A17),
            onTertiaryContainer = Color(0xFFFFDBC8),
            background = Color(0xFF111318),
            onBackground = Color(0xFFE2E2E9),
            surface = Color(0xFF111318),
            onSurface = Color(0xFFE2E2E9),
            surfaceVariant = Color(0xFF44474F),
            onSurfaceVariant = Color(0xFFC4C6D0),
            surfaceContainerLowest = Color(0xFF0C0E13),
            surfaceContainerLow = Color(0xFF191C20),
            surfaceContainer = Color(0xFF1D2024),
            surfaceContainerHigh = Color(0xFF282A2F),
            surfaceContainerHighest = Color(0xFF33353A),
            outline = Color(0xFF8E9099),
            outlineVariant = Color(0xFF44474F),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2B5BD7),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD8E2FF),
            onPrimaryContainer = Color(0xFF001A43),
            secondary = Color(0xFF565E71),
            secondaryContainer = Color(0xFFDBE2F9),
            onSecondaryContainer = Color(0xFF141B2C),
            tertiary = Color(0xFF8F4E22),
            tertiaryContainer = Color(0xFFFFDBC8),
            onTertiaryContainer = Color(0xFF331200),
            background = Color(0xFFF9F9FF),
            onBackground = Color(0xFF191C20),
            surface = Color(0xFFF9F9FF),
            onSurface = Color(0xFF191C20),
            surfaceVariant = Color(0xFFE1E2EC),
            onSurfaceVariant = Color(0xFF44474F),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFEDEDF4),
            surfaceContainerHigh = Color(0xFFE7E8EE),
            surfaceContainerHighest = Color(0xFFE2E2E9),
            outline = Color(0xFF74777F),
            outlineVariant = Color(0xFFC4C6D0),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
