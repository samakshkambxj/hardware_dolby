/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.audio

import android.media.audiofx.AudioEffect
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Controller for the VeynFx DSP chain, driven by the VeynFx native
 * effect (AxionOS, Apache-2.0).
 *
 * This is Stage A of the VeynFx integration: app-side control only.
 * The native side (libveynfxaidl + audio_effects.xml registration)
 * lives in the device tree. Until it lands, [init] fails gracefully,
 * [available] stays false and the UI shows an "engine missing"
 * placeholder instead of crashing.
 *
 * Protocol mirrors native/VeynFxParams.h: effect_param_t blobs carrying
 * one int32 paramId plus one int32 value (or a raw IR wav blob for the
 * convolver). Value scaling mirrors VeynFxEngine.cpp: parameters the
 * engine divides by 100 take percent-style ints here, raw parameters
 * take device units (dB, ms, Hz) as documented per setter.
 */
class VeynFxController(
    private val sessionId: Int = 0
) {
    private var effect: AudioEffect? = null

    val available: Boolean get() = effect != null

    fun init(): Boolean {
        if (effect != null) return true
        effect = try {
            AudioEffect(TYPE_UUID, IMPL_UUID, 0, sessionId).also {
                it.enabled = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "VeynFx effect not present: ${e.message}")
            null
        }
        return effect != null
    }

    fun setParam(paramId: Int, value: Int) {
        val fx = effect ?: return
        runCatching { fx.setParameter(intBytes(paramId), intBytes(value)) }
    }

    /**
     * Band-packed parameter: (band << 16) | (value & 0xFFFF). Use
     * [setBandParamSigned] when the low 16 bits carry a signed value
     * (threshold/makeup dB); the two's-complement truncation is what the
     * native side decodes with int16_t.
     */
    fun setBandParam(paramId: Int, band: Int, value: Int) {
        setParam(paramId, (band shl 16) or (value and 0xFFFF))
    }

    fun setBandParamSigned(paramId: Int, band: Int, value: Int) {
        setBandParam(paramId, band, value)
    }

    /** Raw IR wav bytes (PARAM_CONVOLVER_LOAD_IR_DATA). Binder-sized. */
    fun loadIrData(bytes: ByteArray) {
        val fx = effect ?: return
        runCatching { fx.setParameter(intBytes(P_CONV_LOAD_IR_DATA), bytes) }
    }

    fun release() {
        runCatching { effect?.release() }
        effect = null
    }

    private fun intBytes(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    companion object {
        private const val TAG = "VeynFxController"

        val TYPE_UUID: UUID =
            UUID.fromString("5867be72-4060-4c55-a378-c1cdef3e1353")
        val IMPL_UUID: UUID =
            UUID.fromString("f35cb927-a887-4f3d-847f-770634486d53")

        /** True once libveynfxaidl is registered in audio_effects.xml. */
        fun isEffectPresent(): Boolean {
            return runCatching {
                AudioEffect.queryEffects()?.any { it.uuid == IMPL_UUID } == true
            }.getOrDefault(false)
        }

        const val P_MASTER_ENABLE = 0x100
        const val P_OUTPUT_GAIN = 0x101

        const val P_WIDENER_ENABLE = 0x400
        const val P_WIDENER_WIDTH = 0x401

        const val P_COMP_ENABLE = 0x700
        const val P_COMP_THRESHOLD = 0x701
        const val P_COMP_RATIO = 0x702
        const val P_COMP_ATTACK = 0x703
        const val P_COMP_RELEASE = 0x704
        const val P_COMP_KNEE = 0x705
        const val P_COMP_MAKEUP = 0x706

        const val P_TUBE_ENABLE = 0x800
        const val P_TUBE_DRIVE = 0x801
        const val P_TUBE_MIX = 0x802

        const val P_AGC_ENABLE = 0x900
        const val P_AGC_TARGET = 0x901
        const val P_AGC_MAX_GAIN = 0x902
        const val P_AGC_SPEED = 0x903

        const val P_XFEED_ENABLE = 0xA00
        const val P_XFEED_LEVEL = 0xA01
        const val P_XFEED_CUTOFF = 0xA02

        const val P_SURR_ENABLE = 0xB00
        const val P_SURR_DELAY = 0xB01
        const val P_SURR_WIDTH = 0xB02

        const val P_CONV_ENABLE = 0xC00
        const val P_CONV_MIX = 0xC01
        const val P_CONV_LOAD_IR_DATA = 0xC03

        const val P_MCOMP_ENABLE = 0xD00
        const val P_MCOMP_BAND_THRESHOLD = 0xD01
        const val P_MCOMP_BAND_RATIO = 0xD02
        const val P_MCOMP_BAND_ATTACK = 0xD03
        const val P_MCOMP_BAND_RELEASE = 0xD04
        const val P_MCOMP_BAND_MAKEUP = 0xD05
        const val P_MCOMP_CROSSOVER = 0xD06

        const val P_EXC_ENABLE = 0xE00
        const val P_EXC_DRIVE = 0xE01
        const val P_EXC_BLEND = 0xE02
        const val P_EXC_FREQ = 0xE03

        const val P_SPAT_ENABLE = 0x1000
        const val P_SPAT_WIDTH = 0x1001
        const val P_SPAT_BLEND = 0x1003
        const val P_SPAT_HRTF = 0x1004

        /** Binder transaction ceiling guard for IR uploads. */
        const val MAX_IR_BYTES = 512 * 1024
    }
}
