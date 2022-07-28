package io.posidon.android.coderain

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.godaddy.android.colorpicker.ClassicColorPicker
import io.posidon.android.coderain.ui.theme.CodeRainTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodeRainTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                    ) {
                        ColorSetting(R.string.background, BACKGROUND_COLOR_KEY) { BACKGROUND_COLOR_DEFAULT }
                        ColorSetting(R.string.foreground, FOREGROUND_COLOR_KEY) { FOREGROUND_COLOR_DEFAULT }
                        ColorSetting(R.string.accent, ACCENT_COLOR_KEY) { ACCENT_COLOR_DEFAULT }
                        ColorSetting(R.string.glow, GLOW_COLOR_KEY) { GLOW_COLOR_DEFAULT }
                        var isWallApplied by remember {
                            mutableStateOf(
                                getSystemService(WallpaperManager::class.java)
                                    .wallpaperInfo?.serviceName == CodeRainWallService::class.java.name
                            )
                        }
                        OnLifecycleEvent { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_RESUME,
                                Lifecycle.Event.ON_START,
                                -> {
                                    isWallApplied = getSystemService(WallpaperManager::class.java)
                                        .wallpaperInfo?.serviceName == CodeRainWallService::class.java.name
                                }
                                else -> Unit
                            }
                        }
                        Button(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 16.dp, bottom = 48.dp),
                            onClick = {
                                if (isWallApplied) {
                                    startService(Intent(this@MainActivity, CodeRainWallService::class.java))
                                    return@Button
                                }
                                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                                intent.putExtra(
                                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    ComponentName(applicationContext, CodeRainWallService::class.java)
                                )
                                startActivity(intent)
                            }
                        ) {
                            Text(text = if (isWallApplied) "Refresh Wallpaper" else "Set Wallpaper")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Context.ColorSetting(labelID: Int, key: Preferences.Key<Int>, default: () -> Int) {
    val scope = rememberCoroutineScope()
    val color by remember {
        colorSettings.data.map {
            it[key] ?: default()
        }
    }.collectAsState(initial = 0)
    var openDialog by remember { mutableStateOf(false) }
    val colorValue = Color(color or 0xff000000.toInt())

    Row(
        modifier = Modifier
            .fillMaxWidth(1f)
            .clickable(onClick = {
                openDialog = !openDialog
            })
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = getString(labelID),
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
        )
        val s = RoundedCornerShape(5.dp)
        Surface(
            color = colorValue,
            shape = s,
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.CenterVertically)
                .border(1.dp, MaterialTheme.colorScheme.outline, s),
        ) {}
    }
    if (openDialog) {
        ClassicColorPicker(
            color = colorValue,
            showAlphaBar = false,
            modifier = Modifier
                .defaultMinSize(minHeight = 128.dp)
                .aspectRatio(3f / 2f, true)
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            onColorChanged = {
                scope.launch {
                    colorSettings.edit { p ->
                        p[key] = it.toColor().toArgb()
                    }
                }
            }
        )
    }
    Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
}