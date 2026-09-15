/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.audio

import android.media.audiofx.Visualizer
import android.util.Log

/**
 * Wraps Android's Visualizer API to expose FFT magnitude data for a
 * real-time spectrum display. Attaches to the same session (0) as
 * DynamicsEqualizerEngine, independent of it.
 */
class DynamicsVisualizerEngine(
    private val sessionId: Int = 0,
    private val barCount: Int = 32
) {
    private var visualizer: Visualizer? = null
    private var isReleased = false

    var onSpectrumUpdate: ((FloatArray) -> Unit)? = null

    fun start() {
        if (visualizer != null || isReleased) return

        try {
            val captureSize = Visualizer.getCaptureSizeRange()[1]
                .coerceAtMost(1024)

            visualizer = Visualizer(sessionId).apply {
                enabled = false
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            // Not used — we rely on FFT capture for the spectrum.
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft ?: return
                            onSpectrumUpdate?.invoke(computeBars(fft))
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Visualizer on session $sessionId", e)
            visualizer = null
        }
    }

    private fun computeBars(fft: ByteArray): FloatArray {
        // fft format: [0]=Re(0)(DC), [1]=Re(N/2), then interleaved Re/Im pairs.
        val n = fft.size / 2
        val magnitudes = FloatArray(n)
        for (i in 0 until n) {
            val re = fft.getOrElse(i * 2) { 0 }.toInt()
            val im = fft.getOrElse(i * 2 + 1) { 0 }.toInt()
            magnitudes[i] = kotlin.math.sqrt((re * re + im * im).toFloat())
        }

        val bars = FloatArray(barCount)
        val bandSize = (magnitudes.size / barCount).coerceAtLeast(1)
        for (bar in 0 until barCount) {
            val start = bar * bandSize
            val end = (start + bandSize).coerceAtMost(magnitudes.size)
            var sum = 0f
            var count = 0
            for (i in start until end) {
                sum += magnitudes[i]
                count++
            }
            bars[bar] = if (count > 0) (sum / count).coerceIn(0f, 255f) / 255f else 0f
        }
        return bars
    }

    fun stop() {
        visualizer?.enabled = false
    }

    fun release() {
        visualizer?.release()
        visualizer = null
        isReleased = true
    }

    companion object {
        private const val TAG = "DynamicsVisualizerEngine"
    }
}
