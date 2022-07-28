package io.posidon.android.coderain.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.luminance
import androidx.core.view.ViewCompat
import io.posidon.android.coderain.*
import kotlinx.coroutines.flow.map

@Composable
fun Context.CodeRainTheme(
    content: @Composable () -> Unit
) {
    val backgroundColor = remember {
        colorSettings.data.map {
            it[BACKGROUND_COLOR_KEY] ?: BACKGROUND_COLOR_DEFAULT
        }
    }.collectAsState(initial = 0)
    val accentColor = remember {
        colorSettings.data.map {
            it[ACCENT_COLOR_KEY] ?: ACCENT_COLOR_DEFAULT
        }
    }.collectAsState(initial = 0)
    val bg = Color(backgroundColor.value or 0xff000000.toInt())
    val accent = Color(accentColor.value or 0xff000000.toInt())
    val isDark = backgroundColor.value.luminance < 0.3f
    val colorScheme = if (isDark) darkColorScheme(
        background = bg,
        primary = accent,
        secondary = accent,
        tertiary = accent,
        onPrimary = bg,
        onSecondary = bg,
        onTertiary = bg,
        outline = Color(0x22ffffff),
    ) else lightColorScheme(
        background = bg,
        primary = accent,
        secondary = accent,
        tertiary = accent,
        onPrimary = bg,
        onSecondary = bg,
        onTertiary = bg,
        outline = Color(0x22000000),
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = bg.toArgb()
            ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}