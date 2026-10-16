package com.endroid.latch.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.endroid.latch.data.ThemeMode
import kotlin.math.max
import kotlin.math.min

fun schemeFromSeed(seed: Long, dark: Boolean): ColorScheme {
    val base = Color(seed or 0xFF000000L)
    val primary = base
    val secondary = primary.rotateHue(32f)
    val tertiary = primary.rotateHue(150f)
    return if (dark) {
        darkColorScheme(
            primary = primary.lighten(0.28f),
            onPrimary = Color(0xFF00201E),
            primaryContainer = primary.darken(0.25f),
            onPrimaryContainer = primary.lighten(0.55f),
            secondary = secondary.lighten(0.22f),
            onSecondary = Color(0xFF102019),
            secondaryContainer = secondary.darken(0.3f),
            onSecondaryContainer = secondary.lighten(0.5f),
            tertiary = tertiary.lighten(0.25f),
            onTertiary = Color(0xFF001F24),
            tertiaryContainer = tertiary.darken(0.28f),
            onTertiaryContainer = tertiary.lighten(0.5f),
            background = Color(0xFF0B1212),
            onBackground = Color(0xFFE1E8E7),
            surface = Color(0xFF0B1212),
            onSurface = Color(0xFFE1E8E7),
            surfaceVariant = Color(0xFF3A4A49),
            onSurfaceVariant = Color(0xFFB9CAC8),
            surfaceContainerLowest = Color(0xFF060A0A),
            surfaceContainerLow = Color(0xFF131A1A),
            surfaceContainer = Color(0xFF171F1F),
            surfaceContainerHigh = Color(0xFF212929),
            surfaceContainerHighest = Color(0xFF2C3434),
            outline = Color(0xFF849391),
            outlineVariant = Color(0xFF3A4A49),
        )
    } else {
        lightColorScheme(
            primary = primary.darken(0.08f),
            onPrimary = Color.White,
            primaryContainer = primary.lighten(0.55f),
            onPrimaryContainer = primary.darken(0.42f),
            secondary = secondary.darken(0.12f),
            onSecondary = Color.White,
            secondaryContainer = secondary.lighten(0.55f),
            onSecondaryContainer = secondary.darken(0.4f),
            tertiary = tertiary.darken(0.1f),
            onTertiary = Color.White,
            tertiaryContainer = tertiary.lighten(0.55f),
            onTertiaryContainer = tertiary.darken(0.4f),
            background = Color(0xFFF4FBFA),
            onBackground = Color(0xFF151D1C),
            surface = Color(0xFFF4FBFA),
            onSurface = Color(0xFF151D1C),
            surfaceVariant = Color(0xFFD5E5E3),
            onSurfaceVariant = Color(0xFF3A4A49),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFEEF6F5),
            surfaceContainer = Color(0xFFE8F1F0),
            surfaceContainerHigh = Color(0xFFE2EBEA),
            surfaceContainerHighest = Color(0xFFDCE6E5),
            outline = Color(0xFF6A7A78),
            outlineVariant = Color(0xFFB9CAC8),
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
    return Color(
        red = max(0f, red * (1f - a)),
        green = max(0f, green * (1f - a)),
        blue = max(0f, blue * (1f - a)),
        alpha = alpha,
    )
}

/** Cheap hue shift in HSV space for secondary/tertiary roles. */
private fun Color.rotateHue(degrees: Float): Color {
    val r = red; val g = green; val b = blue
    val maxc = max(r, max(g, b)); val minc = min(r, min(g, b))
    val delta = maxc - minc
    var h = 0f
    if (delta > 1e-5f) {
        h = when (maxc) {
            r -> ((g - b) / delta) % 6f
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        } * 60f
        if (h < 0) h += 360f
    }
    val s = if (maxc <= 0f) 0f else delta / maxc
    val v = maxc
    h = (h + degrees) % 360f
    if (h < 0) h += 360f
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
    val m = v - c
    val (rp, gp, bp) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(rp + m, gp + m, bp + m, alpha)
}

private val LatchTypography = Typography(
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp),
)

@Composable
fun LatchTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    seedColor: Long = 0xFF006A6AL,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    var scheme = schemeFromSeed(seedColor, dark)
    if (dark && amoledDark) {
        val black = Color(0xFF000000)
        scheme = scheme.copy(
            background = black,
            surface = black,
            surfaceContainerLowest = black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF111111),
            surfaceContainerHigh = Color(0xFF181818),
            surfaceContainerHighest = Color(0xFF1E1E1E),
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = LatchTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
