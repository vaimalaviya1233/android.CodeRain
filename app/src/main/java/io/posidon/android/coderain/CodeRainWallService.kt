package io.posidon.android.coderain

import android.app.WallpaperColors
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.core.graphics.luminance
import androidx.core.graphics.toColor
import androidx.core.graphics.toXfermode
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

val Context.colorSettings by preferencesDataStore("color")

val BACKGROUND_COLOR_KEY = intPreferencesKey("background-color")
val FOREGROUND_COLOR_KEY = intPreferencesKey("foreground-color")
val ACCENT_COLOR_KEY = intPreferencesKey("accent-color")
val GLOW_COLOR_KEY = intPreferencesKey("glow-color")

val BACKGROUND_COLOR_DEFAULT = 0x17101e
val FOREGROUND_COLOR_DEFAULT = 0x654dc6
val ACCENT_COLOR_DEFAULT = 0xaa4dff
val GLOW_COLOR_DEFAULT = 0xaa33ff

class CodeRainWallService : WallpaperService() {

    private var engine: Engine? = null

    override fun onCreateEngine() = Engine().also { engine = it }
    private val frameRate = 60

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        engine?.loadColors()
        return super.onStartCommand(intent, flags, startId)
    }

    inner class Engine : WallpaperService.Engine() {

        var mViewWidth = 0
        var mViewHeight = 0

        var yPositions = IntArray(0)
        var xOffset = 0f
        var maxI = 0
        var waitUntil = DoubleArray(0)
        var lastRedX = 0
        var lastRedY = 0
        var cellWidth = 64f
        var cellHeight = 64f
        val trailBlurF = 0.05f
        val blurF = 0.04f
        val glowF = 1.2f

        private val clearPaint = Paint().apply {
            style = Paint.Style.FILL
        }
        private val fullClearPaint = Paint().apply {
            style = Paint.Style.FILL
        }
        private val charPaint = Paint().apply {
            style = Paint.Style.FILL
            typeface = resources.getFont(R.font.nasin_nanpa)
            textSize = cellHeight * 0.9f
            maskFilter = BlurMaskFilter(cellHeight * blurF, BlurMaskFilter.Blur.NORMAL)
        }
        private val accentTrailPaint = Paint().apply {
            style = Paint.Style.FILL
            typeface = resources.getFont(R.font.nasin_nanpa)
            textSize = cellHeight * 0.9f
            maskFilter = BlurMaskFilter(cellHeight * trailBlurF, BlurMaskFilter.Blur.NORMAL)
        }
        private val accentPaint = Paint().apply {
            style = Paint.Style.FILL
            typeface = resources.getFont(R.font.nasin_nanpa)
            textSize = cellHeight * 0.9f
            maskFilter = BlurMaskFilter(cellHeight * blurF, BlurMaskFilter.Blur.NORMAL)
        }
        private val glowPaint = Paint().apply {
            style = Paint.Style.FILL
            typeface = resources.getFont(R.font.nasin_nanpa)
            textSize = cellHeight * 0.9f
            maskFilter = BlurMaskFilter(cellHeight * glowF, BlurMaskFilter.Blur.NORMAL)
            xfermode = PorterDuff.Mode.ADD.toXfermode()
        }

        private val drawHandler = Handler(Looper.myLooper()!!)
        private var visible = false

        init {
            loadColors()
        }

        private fun setDimensions() {
            val cells = mViewWidth / cellWidth
            yPositions = IntArray(cells.toInt())
            waitUntil = DoubleArray(cells.toInt())
            xOffset = (cells % 1) * cellWidth
            maxI = (mViewHeight / cellHeight).toInt()
        }

        internal fun loadColors() {
            MainScope().launch {
                val data = colorSettings.data.first()
                val backgroundColor = (data[BACKGROUND_COLOR_KEY] ?: BACKGROUND_COLOR_DEFAULT) and 0xffffff
                val foregroundColor = (data[FOREGROUND_COLOR_KEY] ?: FOREGROUND_COLOR_DEFAULT) and 0xffffff
                clearPaint.color = backgroundColor or 0xdd000000.toInt()
                fullClearPaint.color = backgroundColor or 0xff000000.toInt()
                charPaint.color = foregroundColor or 0x88000000.toInt()
                accentTrailPaint.color = foregroundColor or 0xff000000.toInt()
                accentPaint.color = (data[ACCENT_COLOR_KEY] ?: ACCENT_COLOR_DEFAULT) or 0xff000000.toInt()
                glowPaint.color = (data[GLOW_COLOR_KEY] ?: GLOW_COLOR_DEFAULT) or 0xff000000.toInt()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    notifyColorsChanged()
                }
            }
        }

        private fun fullClear(canvas: Canvas) = canvas.drawPaint(fullClearPaint)

        private fun clear(canvas: Canvas) = canvas.drawPaint(clearPaint)

        private fun draw(canvas: Canvas) {
            for (i in yPositions.indices) {
                val text = String(Character.toChars(0xf1900 + (Random((System.currentTimeMillis().toInt() / 60) xor i).nextFloat () * 0x88).toInt()))
                val paint = if (abs(lastRedY + lastRedX - i - yPositions[i]) > 3) {
                    lastRedX = i
                    lastRedY = yPositions[i]
                    charPaint
                } else accentPaint
                if (System.currentTimeMillis() - waitUntil[i] > 0) {
                    val trailPaint = if (paint == accentPaint)
                        accentTrailPaint
                    else charPaint
                    if (Random.nextFloat () > 0.3) {
                        waitUntil[i] = 0.0
                        drawText (canvas, text, i, trailPaint)
                        if (yPositions[i] > maxI)
                            yPositions[i] = 0
                        else
                            yPositions[i]++
                    } else {
                        waitUntil[i] = System.currentTimeMillis() + Random.nextDouble() * 1200 + 300
                    }
                } else {
                    if (paint == accentPaint)
                        drawText (canvas, text, i, glowPaint)
                    drawText (canvas, text, i, paint)
                }
            }
        }

        private fun drawText(canvas: Canvas, text: String, i: Int, paint: Paint) {
            canvas.drawText(text, xOffset + i * cellWidth, (yPositions[i] + 1) * cellHeight, paint)
        }

        private fun frame() {
            val holder = surfaceHolder
            Thread.sleep(10)
            holder.lockCanvas().also {
                clear(it)
                draw(it)
            }.run(holder::unlockCanvasAndPost)
            drawHandler.removeCallbacks(::frame)
            if (visible) {
                drawHandler.postDelayed(::frame, 1000L / frameRate)
            }
        }

        override fun onDestroy() {
            drawHandler.removeCallbacks(::frame)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) frame()
            else drawHandler.removeCallbacks(::frame)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            loadColors()
            setDimensions()
            holder.lockCanvas().apply(::fullClear).run(holder::unlockCanvasAndPost)
            frame()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int, width: Int, height: Int
        ) {
            mViewWidth = width
            mViewHeight = height
            loadColors()
            setDimensions()
            holder.lockCanvas().apply(::fullClear).run(holder::unlockCanvasAndPost)
            frame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            visible = false
            drawHandler.removeCallbacks(::frame)
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        override fun onComputeColors(): WallpaperColors {
            val background = fullClearPaint.color
            val foreground = charPaint.color or 0xff000000.toInt()
            val accent = accentPaint.color
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) WallpaperColors(
                background.toColor(),
                foreground.toColor(),
                accent.toColor(),
                if (background.luminance < 0.3f) WallpaperColors.HINT_SUPPORTS_DARK_THEME
                else WallpaperColors.HINT_SUPPORTS_DARK_TEXT
            ) else WallpaperColors(
                background.toColor(),
                foreground.toColor(),
                accent.toColor(),
            )
        }
    }
}