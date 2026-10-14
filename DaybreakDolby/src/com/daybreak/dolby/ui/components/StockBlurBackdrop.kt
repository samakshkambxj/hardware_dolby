/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.max

/**
 * Snapshot backdrop blur for devices without the refraction engine.
 * Captures the pixels behind this surface from the activity root and
 * draws them back GPU-blurred, refreshing on attach, resize, updates,
 * and scrolls. Never used where the live engine runs.
 */
@Composable
fun StockBlurBackdrop(
    tint: Color,
    modifier: Modifier = Modifier,
    blurRadiusPx: Float = 20f,
    updateKey: Any = Unit,
) {
    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                SnapshotBlurView(context).also {
                    it.blurRadiusPx = blurRadiusPx
                }
            },
            update = { view ->
                view.blurRadiusPx = blurRadiusPx
                @Suppress("UNUSED_EXPRESSION")
                updateKey
                view.requestRefresh()
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tint),
        )
    }
}

/** Root snapshot view. Draws nothing until the first capture lands. */
class SnapshotBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var blurRadiusPx: Float = 20f
        set(value) {
            if (field != value) {
                field = value
                applyRenderEffect()
            }
        }

    private var snapshot: Bitmap? = null
    private val snapshotCanvas = Canvas()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val drawRect = android.graphics.Rect()
    private var attached = false
    private var lastRefreshMs = 0L
    private var scrollObserver: ViewTreeObserver? = null

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        requestRefresh()
    }

    init {
        setWillNotDraw(false)
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
                if (blurRadiusPx < 1f) {
                    setRenderEffect(null)
                } else {
                    setRenderEffect(
                        RenderEffect.createBlurEffect(
                            blurRadiusPx.coerceIn(1f, 64f),
                            blurRadiusPx.coerceIn(1f, 64f),
                            Shader.TileMode.CLAMP,
                        ),
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    fun requestRefresh() {
        if (!attached || width <= 0 || height <= 0 || !isAttachedToWindow) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastRefreshMs < 50L) return
        lastRefreshMs = now
        post { refresh() }
    }

    private fun refresh() {
        if (!attached || width <= 0 || height <= 0 || !isAttachedToWindow) return
        val root = rootView ?: return
        try {
            val selfPos = IntArray(2)
            val rootPos = IntArray(2)
            getLocationInWindow(selfPos)
            root.getLocationInWindow(rootPos)
            val left = selfPos[0] - rootPos[0]
            val top = selfPos[1] - rootPos[1]

            val downsample = 8
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
            val was = visibility
            visibility = INVISIBLE
            try {
                root.draw(snapshotCanvas)
            } finally {
                visibility = was
            }
            snapshotCanvas.restore()
            snapshotCanvas.setBitmap(null)
            invalidate()
        } catch (_: Exception) {
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = snapshot
        if (bitmap != null && !bitmap.isRecycled) {
            drawRect.set(0, 0, width, height)
            canvas.drawBitmap(bitmap, null, drawRect, bitmapPaint)
        }
    }
}
