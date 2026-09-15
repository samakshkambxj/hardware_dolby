/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * Real backdrop blur for in-window bars (top bar, bottom nav toolbar).
 *
 * The old frost stacked translucent gradients + a primary-tinted glow. That
 * is decoration, not blur: scrolling rows stayed sharp behind the bar and
 * clashed with the dialog blur (see ApplyDialogWindowBlur, which uses the
 * platform blur-behind layer and is genuinely frosted).
 *
 * Same-window content cannot use blur-behind (dialog windows only), and
 * RenderEffect on a Compose node blurs its *own* pixels, never the backdrop.
 * So this view snapshots the actual pixels behind the bar from the activity
 * root, draws them back, and blurs the snapshot:
 *
 * - API 31+: GPU [RenderEffect] blur on the snapshot (same frosted-glass
 *   read as the audio-output dialog background).
 * - Below S: two-pass box blur on the downsampled snapshot (no RenderEffect).
 *
 * No new dependencies: framework + Compose ui only, Soong-safe.
 *
 * Usage: place [RealBlurBackdrop] as the first layer inside the bar, with
 * the tint above it and sharp content (title, buttons) on top.
 *
 * @param blurRadiusPx GPU blur radius at full scale. 24-32 matches the
 * dialog frost (~90px blurBehind at window scale reads similarly dense).
 * @param tint Neutral veil over the blur for legibility. Keep it neutral
 * (surface), never white/specular and never saturated: glow is what this
 * replaces.
 * @param updateKey Bump when the backdrop changed (e.g. bucketed scroll
 * fraction) to re-snapshot. A slow ticker also refreshes for animations.
 */
@Composable
fun RealBlurBackdrop(
    tint: Color,
    modifier: Modifier = Modifier,
    blurRadiusPx: Float = 28f,
    updateKey: Any = Unit,
    downsample: Int = 4
) {
    val viewHolder = remember { mutableStateOf<BackdropBlurView?>(null) }
    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                BackdropBlurView(context).also {
                    it.blurRadiusPx = blurRadiusPx
                    it.downsample = downsample
                    viewHolder.value = it
                }
            },
            update = { view ->
                view.blurRadiusPx = blurRadiusPx
                // Read updateKey so every change re-runs this block.
                @Suppress("UNUSED_EXPRESSION")
                updateKey
                view.refresh()
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tint)
        )
    }
    // Keep animated backdrops (particles, flings) fresh without spamming:
    // snapshots are tiny (~1/16 area) and blur is GPU-side.
    LaunchedEffect(Unit) {
        while (true) {
            delay(240)
            viewHolder.value?.refresh()
        }
    }
    LaunchedEffect(updateKey) {
        viewHolder.value?.refresh()
    }
}

/**
 * Snapshots the activity root behind this view and draws it blurred.
 * Draws nothing until the first snapshot lands (transparent).
 */
class BackdropBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var blurRadiusPx: Float = 28f
        set(value) {
            field = value
            applyRenderEffect()
        }
    var downsample: Int = 4
        set(value) {
            field = value.coerceIn(2, 8)
            snapshot?.recycle()
            snapshot = null
        }

    private var snapshot: Bitmap? = null
    private val snapshotCanvas = Canvas()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var attached = false

    init {
        // We draw the snapshot ourselves; no framework background.
        setWillNotDraw(false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        applyRenderEffect()
        post { refresh() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attached = false
        snapshot?.recycle()
        snapshot = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) post { refresh() }
    }

    private fun applyRenderEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && attached) {
            try {
                setRenderEffect(
                    RenderEffect.createBlurEffect(
                        blurRadiusPx.coerceIn(4f, 120f),
                        blurRadiusPx.coerceIn(4f, 120f),
                        Shader.TileMode.CLAMP
                    )
                )
            } catch (_: Exception) {
                // OEM without RenderEffect: snapshot stays sharp, tint covers.
            }
        }
    }

    /**
     * Re-capture the pixels behind this view. Safe to call any time outside
     * the draw pass (Compose update blocks, ticker, layout changes).
     */
    fun refresh() {
        if (!attached || width <= 0 || height <= 0 || !isAttachedToWindow) return
        val root = rootView ?: return
        try {
            val selfPos = IntArray(2)
            val rootPos = IntArray(2)
            getLocationInWindow(selfPos)
            root.getLocationInWindow(rootPos)
            val left = selfPos[0] - rootPos[0]
            val top = selfPos[1] - rootPos[1]

            val sw = max(1, width / downsample)
            val sh = max(1, height / downsample)
            var bmp = snapshot
            if (bmp == null || bmp.isRecycled || bmp.width != sw || bmp.height != sh) {
                bmp?.takeIf { !it.isRecycled }?.recycle()
                bmp = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                snapshot = bmp
            }
            val bitmap = bmp ?: return
            snapshotCanvas.setBitmap(bitmap)
            snapshotCanvas.save()
            val scale = 1f / downsample
            snapshotCanvas.scale(scale, scale)
            snapshotCanvas.translate(-left.toFloat(), -top.toFloat())
            // Hide self so the capture holds only the backdrop, never this
            // view's own last frame (no feedback smear). INVISIBLE avoids a
            // layout pass; we are outside draw so this is safe.
            val was = visibility
            visibility = INVISIBLE
            try {
                root.draw(snapshotCanvas)
            } finally {
                visibility = was
            }
            snapshotCanvas.restore()
            snapshotCanvas.setBitmap(null)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                boxBlurInPlace(bitmap, 2)
            }
            invalidate()
        } catch (_: Exception) {
            // Capture is best-effort; the tint layer still legibilizes text.
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = snapshot
        if (bitmap != null && !bitmap.isRecycled) {
            canvas.drawBitmap(
                bitmap,
                null,
                android.graphics.Rect(0, 0, width, height),
                bitmapPaint
            )
        }
    }

    /** Two-pass box blur for pre-S fallback (tiny bitmap: radius 2 is plenty). */
    private fun boxBlurInPlace(bitmap: Bitmap, radius: Int) {
        if (radius < 1) return
        val w = bitmap.width
        val h = bitmap.height
        if (w < 3 || h < 3) return
        try {
            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            val tmp = IntArray(w * h)
            val div = radius * 2 + 1
            // Horizontal pass.
            for (y in 0 until h) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                for (x in -radius..radius) {
                    val c = pixels[y * w + x.coerceIn(0, w - 1)]
                    a += (c ushr 24) and 0xFF
                    r += (c ushr 16) and 0xFF
                    g += (c ushr 8) and 0xFF
                    b += c and 0xFF
                }
                for (x in 0 until w) {
                    tmp[y * w + x] =
                        ((a / div) shl 24) or ((r / div) shl 16) or ((g / div) shl 8) or (b / div)
                    val outX = (x - radius).coerceIn(0, w - 1)
                    val inX = (x + radius + 1).coerceIn(0, w - 1)
                    val outC = pixels[y * w + outX]
                    val inC = pixels[y * w + inX]
                    a += ((inC ushr 24) and 0xFF) - ((outC ushr 24) and 0xFF)
                    r += ((inC ushr 16) and 0xFF) - ((outC ushr 16) and 0xFF)
                    g += ((inC ushr 8) and 0xFF) - ((outC ushr 8) and 0xFF)
                    b += (inC and 0xFF) - (outC and 0xFF)
                }
            }
            // Vertical pass.
            for (x in 0 until w) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                for (y in -radius..radius) {
                    val c = tmp[y.coerceIn(0, h - 1) * w + x]
                    a += (c ushr 24) and 0xFF
                    r += (c ushr 16) and 0xFF
                    g += (c ushr 8) and 0xFF
                    b += c and 0xFF
                }
                for (y in 0 until h) {
                    pixels[y * w + x] =
                        ((a / div) shl 24) or ((r / div) shl 16) or ((g / div) shl 8) or (b / div)
                    val outY = (y - radius).coerceIn(0, h - 1)
                    val inY = (y + radius + 1).coerceIn(0, h - 1)
                    val outC = tmp[outY * w + x]
                    val inC = tmp[inY * w + x]
                    a += ((inC ushr 24) and 0xFF) - ((outC ushr 24) and 0xFF)
                    r += ((inC ushr 16) and 0xFF) - ((outC ushr 16) and 0xFF)
                    g += ((inC ushr 8) and 0xFF) - ((outC ushr 8) and 0xFF)
                    b += (inC and 0xFF) - (outC and 0xFF)
                }
            }
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        } catch (_: Exception) {
        }
    }
}
