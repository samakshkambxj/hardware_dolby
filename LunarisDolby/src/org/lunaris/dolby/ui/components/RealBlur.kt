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
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
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
import kotlin.math.max

/**
 * Navbar-only real backdrop blur.
 *
 * Snapshots the pixels behind the floating nav pill from the activity root
 * and draws them back GPU-blurred. Deliberately used ONLY by
 * [FloatingNavToolbar] — the top bar stays a solid/translucent surface so
 * the two never compete and we pay the snapshot cost once, not twice.
 *
 * Lag fixes vs the earlier dual-bar version:
 * - No infinite ticker loop (that re-drew the whole root every 240ms even
 *   when idle and kept the UI thread hot).
 * - Re-snapshot is on-demand only (attach / size change / [updateKey] /
 *   any scroll in the window via ViewTreeObserver) and throttled to ~100ms
 *   with coalescing, so pager swipes and list scrolls refresh often enough
 *   to avoid a stale "delay" but never spam root.draw() per frame.
 * - Capture is posted to the message queue, never run synchronously inside
 *   Compose layout/draw, which was a major jank source.
 * - Heavier downsample (8 = 1/64 px) and smaller radius (20f): cheaper
 *   upload + cheaper GPU blur, same frosted read under the tint veil.
 * - Hardware layer so the RenderEffect result is GPU-cached.
 */
@Composable
fun RealBlurBackdrop(
    tint: Color,
    modifier: Modifier = Modifier,
    blurRadiusPx: Float = 20f,
    updateKey: Any = Unit,
    downsample: Int = 8
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
                @Suppress("UNUSED_EXPRESSION")
                updateKey
                view.requestRefresh()
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tint)
        )
    }
    LaunchedEffect(updateKey) {
        viewHolder.value?.requestRefresh()
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

    var blurRadiusPx: Float = 20f
        set(value) {
            field = value
            applyRenderEffect()
        }
    var downsample: Int = 8
        set(value) {
            field = value.coerceIn(4, 8)
            snapshot?.recycle()
            snapshot = null
        }

    private var snapshot: Bitmap? = null
    private val snapshotCanvas = Canvas()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var attached = false
    private var lastCaptureMs = 0L
    private var pending = false
    private var scrollObserver: ViewTreeObserver? = null
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        // Scroll of any list in the window moves the pixels behind the
        // pill — refresh (throttled + coalesced in requestRefresh, so
        // flings collapse to ~10fps captures, never one per frame).
        requestRefresh()
    }

    companion object {
        private const val MIN_INTERVAL_MS = 100L
    }

    init {
        setWillNotDraw(false)
        // GPU-cache the blurred output; cheaper than re-rasterizing per frame.
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        applyRenderEffect()
        try {
            rootView?.viewTreeObserver
                ?.takeIf { it.isAlive }
                ?.let {
                    it.addOnScrollChangedListener(scrollListener)
                    scrollObserver = it
                }
        } catch (_: Exception) {
            // Best-effort: blur still refreshes via updateKey/size changes.
        }
        requestRefresh()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attached = false
        try {
            val vto = scrollObserver?.takeIf { it.isAlive }
                ?: rootView?.viewTreeObserver?.takeIf { it.isAlive }
            vto?.removeOnScrollChangedListener(scrollListener)
        } catch (_: Exception) {
        }
        scrollObserver = null
        removeCallbacks(null)
        pending = false
        snapshot?.recycle()
        snapshot = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) requestRefresh()
    }

    private fun applyRenderEffect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && attached) {
            try {
                setRenderEffect(
                    RenderEffect.createBlurEffect(
                        blurRadiusPx.coerceIn(4f, 64f),
                        blurRadiusPx.coerceIn(4f, 64f),
                        Shader.TileMode.CLAMP
                    )
                )
            } catch (_: Exception) {
                // OEM without RenderEffect: snapshot stays sharp, tint covers.
            }
        }
    }

    /**
     * Throttled, coalesced refresh request. Safe to call from Compose update
     * blocks or scroll-driven recompositions — rapid calls collapse into one
     * deferred capture instead of one root.draw() per frame.
     */
    fun requestRefresh() {
        if (!attached || width <= 0 || height <= 0 || !isAttachedToWindow) return
        val now = SystemClock.uptimeMillis()
        val elapsed = now - lastCaptureMs
        if (elapsed < MIN_INTERVAL_MS) {
            if (!pending) {
                pending = true
                postDelayed(
                    {
                        pending = false
                        refresh()
                    },
                    MIN_INTERVAL_MS - elapsed
                )
            }
            return
        }
        // Defer off the calling pass (composition/layout/draw) to avoid jank.
        post { refresh() }
    }

    /**
     * Re-capture the pixels behind this view. Runs on the UI thread via
     * post(), never synchronously inside draw.
     */
    fun refresh() {
        if (!attached || width <= 0 || height <= 0 || !isAttachedToWindow) return
        val root = rootView ?: return
        lastCaptureMs = SystemClock.uptimeMillis()
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
