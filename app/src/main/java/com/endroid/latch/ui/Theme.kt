package com.endroid.latch.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.endroid.latch.data.ThemeMode
import kotlin.math.max
import kotlin.math.min

fun schemeFromSeed(seed: Long, dark: Boolean): ColorScheme {
    val base = Color(seed or 0xFF000000)
    val primary = base
    val container = if (dark) primary.darken(0.35f) else primary.lighten(0.55f)
    val surface = if (dark) Color(0xFF101418) else Color(0xFFF7FAFA)
    val onPrimary = if (dark) Color(0xFF00322F) else Color.White
    return if (dark) {
        darkColorScheme(
            primary = primary.lighten(0.25f),
            onPrimary = onPrimary,
            primaryContainer = container,
            onPrimaryContainer = primary.lighten(0.6f),
            secondary = primary.lighten(0.15f),
            surface = surface,
            background = surface,
        )
    } else {
        lightColorScheme(
            primary = primary.darken(0.05f),
            onPrimary = Color.White,
            primaryContainer = container,
            onPrimaryContainer = primary.darken(0.4f),
            secondary = primary.darken(0.15f),
            surface = surface,
            background = surface,
        )
    }
}

private fun Color.lighten(amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = min(1f, red + (1f - red) * a),
        green = min(1f, green + (1f - green) * a),
        blue = min(1f, blue + (1f - blue) * a),
        alpha = alpha,
    )
}

private fun Color.darken(amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(red = max(0f, red * (1f - a)), green = max(0f, green * (1f - a)), blue = max(0f, blue * (1f - a)), alpha = alpha)
}

@Composable
fun LatchTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    seedColor: Long = 0xFF006A6AL,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = schemeFromSeed(seedColor, dark)
    MaterialTheme(colorScheme = scheme, content = content)
}
