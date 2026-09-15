/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.audio

import android.media.audiofx.DynamicsProcessing
import android.util.Log

/**
 * Standalone parametric EQ engine built on Android's generic
 * DynamicsProcessing API. Independent of the vendor Dolby DAP effect
 * (see DolbyAudioEffect) — both can run simultaneously.
 */
data class MbcBandParams(
    var freqLowCutoff: Float,
    var attackTimeMs: Float = 10f,
    var releaseTimeMs: Float = 100f,
    var ratio: Float = 2f,
    var thresholdDb: Float = -20f,
    var kneeWidthDb: Float = 5f,
    var noiseGateThresholdDb: Float = -50f,
    var expanderRatio: Float = 1f,
    var preGainDb: Float = 0f,
    var postGainDb: Float = 0f
)

class DynamicsEqualizerEngine(
    private val sessionId: Int = 0,
    private val bandCount: Int = 10
) {
    private var dp: DynamicsProcessing? = null
    private var isReleased = false
    private val channelCountFixed = 2

    // MBC: 3 bands (Low / Mid / High)
    val mbcBandCount = 3
    private var mbcEnabled = false
    val mbcBands: MutableList<MbcBandParams> = mutableListOf(
        MbcBandParams(freqLowCutoff = 20f),
        MbcBandParams(freqLowCutoff = 250f),
        MbcBandParams(freqLowCutoff = 4000f)
    )

    // Limiter (final stage, per-channel, no bands)
    private var limiterEnabled = false
    var limiterThresholdDb: Float = -1f
        private set
    var limiterReleaseMs: Float = 50f
        private set
    var limiterRatio: Float = 10f
        private set
    var limiterPostGainDb: Float = 0f
        private set

    // Band center frequencies (Hz) — 10-band ISO-ish spacing
    val bandFrequencies: FloatArray = floatArrayOf(
        31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
    ).let { if (it.size >= bandCount) it.copyOf(bandCount) else it }

    private val bandGains = FloatArray(bandFrequencies.size) { 0f }
    var preampDb: Float = 0f
        private set

    var enabled: Boolean = false
        private set

    fun init() {
        if (dp != null || isReleased) return

        val channelCount = channelCountFixed
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            channelCount,
            /* preEqInUse */ true,
            /* preEqBandCount */ bandFrequencies.size,
            /* mbcInUse */ true,
            /* mbcBandCount */ mbcBandCount,
            /* postEqInUse */ false,
            /* postEqBandCount */ 0,
            /* limiterInUse */ true
        ).build()

        dp = try {
            DynamicsProcessing(0, sessionId, config)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create DynamicsProcessing on session $sessionId", e)
            null
        }

        dp?.let { effect ->
            for (channel in 0 until channelCount) {
                val preEq = effect.getPreEqByChannelIndex(channel)
                preEq.setEnabled(true)
                for (band in bandFrequencies.indices) {
                    val eqBand = DynamicsProcessing.EqBand(
                        /* enabled */ true,
                        /* cutoffFrequency */ bandFrequencies[band],
                        /* gain */ bandGains[band]
                    )
                    effect.setPreEqBandByChannelIndex(channel, band, eqBand)
                }
                effect.setPreEqByChannelIndex(channel, preEq)
            }
            effect.setInputGainAllChannelsTo(preampDb)

            for (channel in 0 until channelCount) {
                val mbc = effect.getMbcByChannelIndex(channel)
                mbc.setEnabled(mbcEnabled)
                for (band in 0 until mbcBandCount) {
                    applyMbcBandToEffect(effect, channel, band)
                }
                effect.setMbcByChannelIndex(channel, mbc)

                applyLimiterToEffect(effect, channel)
            }
        }
    }

    private fun applyLimiterToEffect(effect: DynamicsProcessing, channel: Int) {
        val limiter = DynamicsProcessing.Limiter(
            /* enabled */ limiterEnabled,
            /* linked */ false,
            /* linkGroup */ 0,
            /* attackTime */ 1f,
            /* releaseTime */ limiterReleaseMs,
            /* ratio */ limiterRatio,
            /* threshold */ limiterThresholdDb,
            /* postGain */ limiterPostGainDb
        )
        effect.setLimiterByChannelIndex(channel, limiter)
    }

    fun setLimiterEnabled(value: Boolean) {
        limiterEnabled = value
        val effect = dp ?: return
        for (channel in 0 until channelCountFixed) {
            applyLimiterToEffect(effect, channel)
        }
    }

    fun isLimiterEnabled(): Boolean = limiterEnabled

    fun setLimiterThreshold(thresholdDb: Float) {
        limiterThresholdDb = thresholdDb.coerceIn(-30f, 0f)
        pushLimiter()
    }

    fun setLimiterRelease(releaseMs: Float) {
        limiterReleaseMs = releaseMs.coerceIn(1f, 1000f)
        pushLimiter()
    }

    fun setLimiterRatio(ratio: Float) {
        limiterRatio = ratio.coerceIn(1f, 20f)
        pushLimiter()
    }

    fun setLimiterPostGain(gainDb: Float) {
        limiterPostGainDb = gainDb.coerceIn(-20f, 20f)
        pushLimiter()
    }

    private fun pushLimiter() {
        val effect = dp ?: return
        for (channel in 0 until channelCountFixed) {
            applyLimiterToEffect(effect, channel)
        }
    }

    private fun applyMbcBandToEffect(effect: DynamicsProcessing, channel: Int, band: Int) {
        val p = mbcBands[band]
        val mbcBand = DynamicsProcessing.MbcBand(
            /* enabled */ true,
            /* cutoffFrequency */ p.freqLowCutoff,
            /* attackTime */ p.attackTimeMs,
            /* releaseTime */ p.releaseTimeMs,
            /* ratio */ p.ratio,
            /* threshold */ p.thresholdDb,
            /* kneeWidth */ p.kneeWidthDb,
            /* noiseGateThreshold */ p.noiseGateThresholdDb,
            /* expanderRatio */ p.expanderRatio,
            /* preGain */ p.preGainDb,
            /* postGain */ p.postGainDb
        )
        effect.setMbcBandByChannelIndex(channel, band, mbcBand)
    }

    fun setMbcEnabled(value: Boolean) {
        mbcEnabled = value
        val effect = dp ?: return
        for (channel in 0 until channelCountFixed) {
            val mbc = effect.getMbcByChannelIndex(channel)
            mbc.setEnabled(value)
            effect.setMbcByChannelIndex(channel, mbc)
        }
    }

    fun isMbcEnabled(): Boolean = mbcEnabled

    fun setMbcThreshold(band: Int, thresholdDb: Float) {
        if (band !in 0 until mbcBandCount) return
        mbcBands[band].thresholdDb = thresholdDb.coerceIn(-60f, 0f)
        pushMbcBand(band)
    }

    fun setMbcRatio(band: Int, ratio: Float) {
        if (band !in 0 until mbcBandCount) return
        mbcBands[band].ratio = ratio.coerceIn(1f, 20f)
        pushMbcBand(band)
    }

    fun setMbcAttack(band: Int, attackMs: Float) {
        if (band !in 0 until mbcBandCount) return
        mbcBands[band].attackTimeMs = attackMs.coerceIn(1f, 200f)
        pushMbcBand(band)
    }

    fun setMbcRelease(band: Int, releaseMs: Float) {
        if (band !in 0 until mbcBandCount) return
        mbcBands[band].releaseTimeMs = releaseMs.coerceIn(10f, 1000f)
        pushMbcBand(band)
    }

    fun getMbcBand(band: Int): MbcBandParams = mbcBands[band]

    private fun pushMbcBand(band: Int) {
        val effect = dp ?: return
        for (channel in 0 until channelCountFixed) {
            applyMbcBandToEffect(effect, channel, band)
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        dp?.enabled = value
    }

    fun setBandGain(band: Int, gainDb: Float) {
        if (band !in bandFrequencies.indices) return
        val clamped = gainDb.coerceIn(-20f, 20f)
        bandGains[band] = clamped
        val effect = dp ?: return
        val channelCount = 2
        for (channel in 0 until channelCount) {
            val eqBand = DynamicsProcessing.EqBand(true, bandFrequencies[band], clamped)
            effect.setPreEqBandByChannelIndex(channel, band, eqBand)
        }
    }

    fun getBandGain(band: Int): Float =
        bandGains.getOrElse(band) { 0f }

    fun setPreamp(gainDb: Float) {
        preampDb = gainDb.coerceIn(-20f, 20f)
        dp?.setInputGainAllChannelsTo(preampDb)
    }

    /** Flatlines preamp + all pre-EQ bands back to 0 dB. */
    fun resetBands() {
        setPreamp(0f)
        for (band in bandFrequencies.indices) {
            setBandGain(band, 0f)
        }
    }

    /** Restores MBC thresholds/ratios/attacks/releases to their defaults. */
    fun resetMbc() {
        for (band in 0 until mbcBandCount) {
            setMbcThreshold(band, MBC_DEFAULT_THRESHOLD_DB)
            setMbcRatio(band, MBC_DEFAULT_RATIO)
            setMbcAttack(band, MBC_DEFAULT_ATTACK_MS)
            setMbcRelease(band, MBC_DEFAULT_RELEASE_MS)
        }
    }

    /** Restores limiter threshold/release/ratio/post-gain to defaults. */
    fun resetLimiter() {
        setLimiterThreshold(LIMITER_DEFAULT_THRESHOLD_DB)
        setLimiterRelease(LIMITER_DEFAULT_RELEASE_MS)
        setLimiterRatio(LIMITER_DEFAULT_RATIO)
        setLimiterPostGain(LIMITER_DEFAULT_POST_GAIN_DB)
    }

    fun release() {
        dp?.release()
        dp = null
        isReleased = true
    }

    companion object {
        private const val TAG = "DynamicsEqualizerEngine"

        // Factory defaults for the reset paths below (must match the
        // MbcBandParams data-class defaults and limiter field initializers).
        const val MBC_DEFAULT_THRESHOLD_DB = -20f
        const val MBC_DEFAULT_RATIO = 2f
        const val MBC_DEFAULT_ATTACK_MS = 10f
        const val MBC_DEFAULT_RELEASE_MS = 100f
        const val LIMITER_DEFAULT_THRESHOLD_DB = -1f
        const val LIMITER_DEFAULT_RELEASE_MS = 50f
        const val LIMITER_DEFAULT_RATIO = 10f
        const val LIMITER_DEFAULT_POST_GAIN_DB = 0f
    }
}
