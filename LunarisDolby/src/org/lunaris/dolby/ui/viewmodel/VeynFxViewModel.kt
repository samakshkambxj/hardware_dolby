/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lunaris.dolby.audio.VeynFxController
import org.lunaris.dolby.data.VeynFxStateData
import org.lunaris.dolby.data.VeynFxStateStore
import org.lunaris.dolby.utils.ToastHelper
import java.io.File

data class VeynMcompBandUiState(
    val thresholdDb: Int = -20,
    val ratio: Int = 4,
    val attackMs: Int = 5,
    val releaseMs: Int = 50,
    val makeupDb: Int = 0
)

data class VeynFxUiState(
    val available: Boolean = false,
    val masterEnabled: Boolean = false,
    val outputGain: Int = 100,
    val widenerEnabled: Boolean = false,
    val widenerWidth: Int = 100,
    val tubeEnabled: Boolean = false,
    val tubeDrive: Int = 50,
    val tubeMix: Int = 100,
    val exciterEnabled: Boolean = false,
    val exciterDrive: Int = 50,
    val exciterBlend: Int = 50,
    val exciterFreq: Int = 8000,
    val xfeedEnabled: Boolean = false,
    val xfeedLevel: Int = 30,
    val xfeedCutoff: Int = 700,
    val agcEnabled: Boolean = false,
    val agcTargetDb: Int = -20,
    val agcMaxGainDb: Int = 10,
    val agcSpeed: Int = 50,
    val compEnabled: Boolean = false,
    val compThresholdDb: Int = -20,
    val compRatio: Int = 4,
    val compAttackMs: Int = 10,
    val compReleaseMs: Int = 100,
    val compKneeDb: Int = 5,
    val compMakeupDb: Int = 0,
    val surrEnabled: Boolean = false,
    val surrDelay: Int = 20,
    val surrWidth: Int = 50,
    val spatEnabled: Boolean = false,
    val spatWidth: Int = 50,
    val spatBlend: Int = 100,
    val spatHrtf: Int = 0,
    val convEnabled: Boolean = false,
    val convMix: Int = 100,
    val convIrName: String = "",
    val mcompEnabled: Boolean = false,
    val mcompBands: List<VeynMcompBandUiState> = List(4) { VeynMcompBandUiState() },
    val mcompXovers: List<Int> = listOf(200, 1000, 5000)
)

/**
 * Stage-A controller ViewModel for the VeynFx native DSP chain. Owns
 * [VeynFxController] (dumb transport) plus persisted musical state;
 * every setter pushes the wire value immediately so the effect never
 * drifts from the UI. When the native lib is absent everything still
 * works as state — the section just reports the engine missing.
 */
class VeynFxViewModel(application: Application) : AndroidViewModel(application) {

    private val controller = VeynFxController(sessionId = 0)
    private val stateStore = VeynFxStateStore(application)

    private val _uiState = MutableStateFlow(VeynFxUiState())
    val uiState: StateFlow<VeynFxUiState> = _uiState.asStateFlow()

    init {
        val available = controller.init()
        restorePersistedState()
        _uiState.value = buildState(available)
    }

    private fun update(transform: (VeynFxUiState) -> VeynFxUiState) {
        _uiState.value = transform(_uiState.value)
        persistState()
    }

    private fun persistState() {
        val s = _uiState.value
        stateStore.save(
            VeynFxStateData(
                masterEnabled = s.masterEnabled,
                outputGain = s.outputGain,
                widenerEnabled = s.widenerEnabled,
                widenerWidth = s.widenerWidth,
                tubeEnabled = s.tubeEnabled,
                tubeDrive = s.tubeDrive,
                tubeMix = s.tubeMix,
                exciterEnabled = s.exciterEnabled,
                exciterDrive = s.exciterDrive,
                exciterBlend = s.exciterBlend,
                exciterFreq = s.exciterFreq,
                xfeedEnabled = s.xfeedEnabled,
                xfeedLevel = s.xfeedLevel,
                xfeedCutoff = s.xfeedCutoff,
                agcEnabled = s.agcEnabled,
                agcTargetDb = s.agcTargetDb,
                agcMaxGainDb = s.agcMaxGainDb,
                agcSpeed = s.agcSpeed,
                compEnabled = s.compEnabled,
                compThresholdDb = s.compThresholdDb,
                compRatio = s.compRatio,
                compAttackMs = s.compAttackMs,
                compReleaseMs = s.compReleaseMs,
                compKneeDb = s.compKneeDb,
                compMakeupDb = s.compMakeupDb,
                surrEnabled = s.surrEnabled,
                surrDelay = s.surrDelay,
                surrWidth = s.surrWidth,
                spatEnabled = s.spatEnabled,
                spatWidth = s.spatWidth,
                spatBlend = s.spatBlend,
                spatHrtf = s.spatHrtf,
                convEnabled = s.convEnabled,
                convMix = s.convMix,
                convIrName = s.convIrName,
                mcompEnabled = s.mcompEnabled,
                mcompThresholds = s.mcompBands.map { it.thresholdDb },
                mcompRatios = s.mcompBands.map { it.ratio },
                mcompAttacks = s.mcompBands.map { it.attackMs },
                mcompReleases = s.mcompBands.map { it.releaseMs },
                mcompMakeups = s.mcompBands.map { it.makeupDb },
                mcompXovers = s.mcompXovers
            )
        )
    }

    private fun buildState(available: Boolean): VeynFxUiState {
        val stored = stateStore.load()
        return if (stored == null) {
            VeynFxUiState(available = available)
        } else {
            VeynFxUiState(
                available = available,
                masterEnabled = stored.masterEnabled,
                outputGain = stored.outputGain,
                widenerEnabled = stored.widenerEnabled,
                widenerWidth = stored.widenerWidth,
                tubeEnabled = stored.tubeEnabled,
                tubeDrive = stored.tubeDrive,
                tubeMix = stored.tubeMix,
                exciterEnabled = stored.exciterEnabled,
                exciterDrive = stored.exciterDrive,
                exciterBlend = stored.exciterBlend,
                exciterFreq = stored.exciterFreq,
                xfeedEnabled = stored.xfeedEnabled,
                xfeedLevel = stored.xfeedLevel,
                xfeedCutoff = stored.xfeedCutoff,
                agcEnabled = stored.agcEnabled,
                agcTargetDb = stored.agcTargetDb,
                agcMaxGainDb = stored.agcMaxGainDb,
                agcSpeed = stored.agcSpeed,
                compEnabled = stored.compEnabled,
                compThresholdDb = stored.compThresholdDb,
                compRatio = stored.compRatio,
                compAttackMs = stored.compAttackMs,
                compReleaseMs = stored.compReleaseMs,
                compKneeDb = stored.compKneeDb,
                compMakeupDb = stored.compMakeupDb,
                surrEnabled = stored.surrEnabled,
                surrDelay = stored.surrDelay,
                surrWidth = stored.surrWidth,
                spatEnabled = stored.spatEnabled,
                spatWidth = stored.spatWidth,
                spatBlend = stored.spatBlend,
                spatHrtf = stored.spatHrtf,
                convEnabled = stored.convEnabled,
                convMix = stored.convMix,
                convIrName = stored.convIrName,
                mcompEnabled = stored.mcompEnabled,
                mcompBands = (0 until 4).map { i ->
                    VeynMcompBandUiState(
                        thresholdDb = stored.mcompThresholds.getOrElse(i) { -20 },
                        ratio = stored.mcompRatios.getOrElse(i) { 4 },
                        attackMs = stored.mcompAttacks.getOrElse(i) { 5 },
                        releaseMs = stored.mcompReleases.getOrElse(i) { 50 },
                        makeupDb = stored.mcompMakeups.getOrElse(i) { 0 }
                    )
                },
                mcompXovers = (0 until 3).map { stored.mcompXovers.getOrElse(it) { defaults[it] } }
            )
        }
    }

    private val defaults = listOf(200, 1000, 5000)

    /** Pushes the full persisted state into the live effect. */
    private fun restorePersistedState() {
        val s = _uiState.value
        // NOTE: _uiState is still default here on first boot; read the
        // store directly so a cold start restores everything.
        val stored = stateStore.load() ?: return
        pushAll(stored.toUiState())
    }

    private fun VeynFxStateData.toUiState(): VeynFxUiState {
        return VeynFxUiState(
            available = _uiState.value.available,
            masterEnabled = masterEnabled,
            outputGain = outputGain,
            widenerEnabled = widenerEnabled,
            widenerWidth = widenerWidth,
            tubeEnabled = tubeEnabled,
            tubeDrive = tubeDrive,
            tubeMix = tubeMix,
            exciterEnabled = exciterEnabled,
            exciterDrive = exciterDrive,
            exciterBlend = exciterBlend,
            exciterFreq = exciterFreq,
            xfeedEnabled = xfeedEnabled,
            xfeedLevel = xfeedLevel,
            xfeedCutoff = xfeedCutoff,
            agcEnabled = agcEnabled,
            agcTargetDb = agcTargetDb,
            agcMaxGainDb = agcMaxGainDb,
            agcSpeed = agcSpeed,
            compEnabled = compEnabled,
            compThresholdDb = compThresholdDb,
            compRatio = compRatio,
            compAttackMs = compAttackMs,
            compReleaseMs = compReleaseMs,
            compKneeDb = compKneeDb,
            compMakeupDb = compMakeupDb,
            surrEnabled = surrEnabled,
            surrDelay = surrDelay,
            surrWidth = surrWidth,
            spatEnabled = spatEnabled,
            spatWidth = spatWidth,
            spatBlend = spatBlend,
            spatHrtf = spatHrtf,
            convEnabled = convEnabled,
            convMix = convMix,
            convIrName = convIrName,
            mcompEnabled = mcompEnabled,
            mcompBands = (0 until 4).map { i ->
                VeynMcompBandUiState(
                    thresholdDb = mcompThresholds.getOrElse(i) { -20 },
                    ratio = mcompRatios.getOrElse(i) { 4 },
                    attackMs = mcompAttacks.getOrElse(i) { 5 },
                    releaseMs = mcompReleases.getOrElse(i) { 50 },
                    makeupDb = mcompMakeups.getOrElse(i) { 0 }
                )
            },
            mcompXovers = (0 until 3).map { mcompXovers.getOrElse(it) { defaults[it] } }
        )
    }

    private fun pushAll(s: VeynFxUiState) {
        val c = controller
        c.setParam(VeynFxController.P_MASTER_ENABLE, if (s.masterEnabled) 1 else 0)
        c.setParam(VeynFxController.P_OUTPUT_GAIN, s.outputGain)
        c.setParam(VeynFxController.P_WIDENER_ENABLE, if (s.widenerEnabled) 1 else 0)
        c.setParam(VeynFxController.P_WIDENER_WIDTH, s.widenerWidth)
        c.setParam(VeynFxController.P_TUBE_ENABLE, if (s.tubeEnabled) 1 else 0)
        c.setParam(VeynFxController.P_TUBE_DRIVE, s.tubeDrive)
        c.setParam(VeynFxController.P_TUBE_MIX, s.tubeMix)
        c.setParam(VeynFxController.P_EXC_ENABLE, if (s.exciterEnabled) 1 else 0)
        c.setParam(VeynFxController.P_EXC_DRIVE, s.exciterDrive)
        c.setParam(VeynFxController.P_EXC_BLEND, s.exciterBlend)
        c.setParam(VeynFxController.P_EXC_FREQ, s.exciterFreq)
        c.setParam(VeynFxController.P_XFEED_ENABLE, if (s.xfeedEnabled) 1 else 0)
        c.setParam(VeynFxController.P_XFEED_LEVEL, s.xfeedLevel)
        c.setParam(VeynFxController.P_XFEED_CUTOFF, s.xfeedCutoff)
        c.setParam(VeynFxController.P_AGC_ENABLE, if (s.agcEnabled) 1 else 0)
        c.setParam(VeynFxController.P_AGC_TARGET, s.agcTargetDb * 100)
        c.setParam(VeynFxController.P_AGC_MAX_GAIN, s.agcMaxGainDb * 100)
        c.setParam(VeynFxController.P_AGC_SPEED, s.agcSpeed)
        c.setParam(VeynFxController.P_COMP_ENABLE, if (s.compEnabled) 1 else 0)
        c.setParam(VeynFxController.P_COMP_THRESHOLD, s.compThresholdDb * 100)
        c.setParam(VeynFxController.P_COMP_RATIO, s.compRatio * 100)
        c.setParam(VeynFxController.P_COMP_ATTACK, s.compAttackMs * 10)
        c.setParam(VeynFxController.P_COMP_RELEASE, s.compReleaseMs)
        c.setParam(VeynFxController.P_COMP_KNEE, s.compKneeDb * 100)
        c.setParam(VeynFxController.P_COMP_MAKEUP, s.compMakeupDb * 100)
        c.setParam(VeynFxController.P_SURR_ENABLE, if (s.surrEnabled) 1 else 0)
        c.setParam(VeynFxController.P_SURR_DELAY, s.surrDelay)
        c.setParam(VeynFxController.P_SURR_WIDTH, s.surrWidth)
        c.setParam(VeynFxController.P_SPAT_ENABLE, if (s.spatEnabled) 1 else 0)
        c.setParam(VeynFxController.P_SPAT_WIDTH, s.spatWidth)
        c.setParam(VeynFxController.P_SPAT_BLEND, s.spatBlend)
        c.setParam(VeynFxController.P_SPAT_HRTF, s.spatHrtf)
        c.setParam(VeynFxController.P_CONV_ENABLE, if (s.convEnabled) 1 else 0)
        c.setParam(VeynFxController.P_CONV_MIX, s.convMix)
        s.mcompBands.forEachIndexed { i, b ->
            c.setBandParamSigned(VeynFxController.P_MCOMP_BAND_THRESHOLD, i, b.thresholdDb * 10)
            c.setBandParam(VeynFxController.P_MCOMP_BAND_RATIO, i, b.ratio * 100)
            c.setBandParam(VeynFxController.P_MCOMP_BAND_ATTACK, i, b.attackMs * 10)
            c.setBandParam(VeynFxController.P_MCOMP_BAND_RELEASE, i, b.releaseMs * 10)
            c.setBandParamSigned(VeynFxController.P_MCOMP_BAND_MAKEUP, i, b.makeupDb * 10)
        }
        s.mcompXovers.forEachIndexed { i, hz ->
            c.setBandParam(VeynFxController.P_MCOMP_CROSSOVER, i, hz)
        }
        c.setParam(VeynFxController.P_MCOMP_ENABLE, if (s.mcompEnabled) 1 else 0)
        restoreIr(s.convIrName)
    }

    private fun irFile(): File = File(getApplication<Application>().filesDir, IR_FILE_NAME)

    /** Re-pushes the saved IR wav bytes after a restart. */
    private fun restoreIr(irName: String) {
        if (irName.isEmpty()) return
        val file = irFile()
        if (!file.exists()) return
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return
        if (bytes.size > VeynFxController.MAX_IR_BYTES) return
        controller.loadIrData(bytes)
    }

    fun setMasterEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_MASTER_ENABLE, if (enabled) 1 else 0)
        update { it.copy(masterEnabled = enabled) }
    }

    fun setOutputGain(gain: Float) {
        val v = gain.toInt().coerceIn(0, 200)
        controller.setParam(VeynFxController.P_OUTPUT_GAIN, v)
        update { it.copy(outputGain = v) }
    }

    fun setWidenerEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_WIDENER_ENABLE, if (enabled) 1 else 0)
        update { it.copy(widenerEnabled = enabled) }
    }

    fun setWidenerWidth(width: Float) {
        val v = width.toInt().coerceIn(0, 200)
        controller.setParam(VeynFxController.P_WIDENER_WIDTH, v)
        update { it.copy(widenerWidth = v) }
    }

    fun setTubeEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_TUBE_ENABLE, if (enabled) 1 else 0)
        update { it.copy(tubeEnabled = enabled) }
    }

    fun setTubeDrive(drive: Float) {
        val v = drive.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_TUBE_DRIVE, v)
        update { it.copy(tubeDrive = v) }
    }

    fun setTubeMix(mix: Float) {
        val v = mix.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_TUBE_MIX, v)
        update { it.copy(tubeMix = v) }
    }

    fun setExciterEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_EXC_ENABLE, if (enabled) 1 else 0)
        update { it.copy(exciterEnabled = enabled) }
    }

    fun setExciterDrive(drive: Float) {
        val v = drive.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_EXC_DRIVE, v)
        update { it.copy(exciterDrive = v) }
    }

    fun setExciterBlend(blend: Float) {
        val v = blend.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_EXC_BLEND, v)
        update { it.copy(exciterBlend = v) }
    }

    fun setExciterFreq(freqHz: Float) {
        val v = freqHz.toInt().coerceIn(1000, 20000)
        controller.setParam(VeynFxController.P_EXC_FREQ, v)
        update { it.copy(exciterFreq = v) }
    }

    fun setXfeedEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_XFEED_ENABLE, if (enabled) 1 else 0)
        update { it.copy(xfeedEnabled = enabled) }
    }

    fun setXfeedLevel(level: Float) {
        val v = level.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_XFEED_LEVEL, v)
        update { it.copy(xfeedLevel = v) }
    }

    fun setXfeedCutoff(cutoffHz: Float) {
        val v = cutoffHz.toInt().coerceIn(200, 5000)
        controller.setParam(VeynFxController.P_XFEED_CUTOFF, v)
        update { it.copy(xfeedCutoff = v) }
    }

    fun setAgcEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_AGC_ENABLE, if (enabled) 1 else 0)
        update { it.copy(agcEnabled = enabled) }
    }

    fun setAgcTargetDb(db: Float) {
        val v = db.toInt().coerceIn(-30, 0)
        controller.setParam(VeynFxController.P_AGC_TARGET, v * 100)
        update { it.copy(agcTargetDb = v) }
    }

    fun setAgcMaxGainDb(db: Float) {
        val v = db.toInt().coerceIn(0, 30)
        controller.setParam(VeynFxController.P_AGC_MAX_GAIN, v * 100)
        update { it.copy(agcMaxGainDb = v) }
    }

    fun setAgcSpeed(speed: Float) {
        val v = speed.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_AGC_SPEED, v)
        update { it.copy(agcSpeed = v) }
    }

    fun setCompEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_COMP_ENABLE, if (enabled) 1 else 0)
        update { it.copy(compEnabled = enabled) }
    }

    fun setCompThresholdDb(db: Float) {
        val v = db.toInt().coerceIn(-60, 0)
        controller.setParam(VeynFxController.P_COMP_THRESHOLD, v * 100)
        update { it.copy(compThresholdDb = v) }
    }

    fun setCompRatio(ratio: Float) {
        val v = ratio.toInt().coerceIn(1, 20)
        controller.setParam(VeynFxController.P_COMP_RATIO, v * 100)
        update { it.copy(compRatio = v) }
    }

    fun setCompAttackMs(ms: Float) {
        val v = ms.toInt().coerceIn(1, 200)
        controller.setParam(VeynFxController.P_COMP_ATTACK, v * 10)
        update { it.copy(compAttackMs = v) }
    }

    fun setCompReleaseMs(ms: Float) {
        val v = ms.toInt().coerceIn(10, 1000)
        controller.setParam(VeynFxController.P_COMP_RELEASE, v)
        update { it.copy(compReleaseMs = v) }
    }

    fun setCompKneeDb(db: Float) {
        val v = db.toInt().coerceIn(0, 30)
        controller.setParam(VeynFxController.P_COMP_KNEE, v * 100)
        update { it.copy(compKneeDb = v) }
    }

    fun setCompMakeupDb(db: Float) {
        val v = db.toInt().coerceIn(0, 24)
        controller.setParam(VeynFxController.P_COMP_MAKEUP, v * 100)
        update { it.copy(compMakeupDb = v) }
    }

    fun setSurrEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_SURR_ENABLE, if (enabled) 1 else 0)
        update { it.copy(surrEnabled = enabled) }
    }

    fun setSurrDelay(delay: Float) {
        val v = delay.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_SURR_DELAY, v)
        update { it.copy(surrDelay = v) }
    }

    fun setSurrWidth(width: Float) {
        val v = width.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_SURR_WIDTH, v)
        update { it.copy(surrWidth = v) }
    }

    fun setSpatEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_SPAT_ENABLE, if (enabled) 1 else 0)
        update { it.copy(spatEnabled = enabled) }
    }

    fun setSpatWidth(width: Float) {
        val v = width.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_SPAT_WIDTH, v)
        update { it.copy(spatWidth = v) }
    }

    fun setSpatBlend(blend: Float) {
        val v = blend.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_SPAT_BLEND, v)
        update { it.copy(spatBlend = v) }
    }

    fun setSpatHrtf(profile: Int) {
        val v = profile.coerceIn(0, 2)
        controller.setParam(VeynFxController.P_SPAT_HRTF, v)
        update { it.copy(spatHrtf = v) }
    }

    fun setConvEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_CONV_ENABLE, if (enabled) 1 else 0)
        update { it.copy(convEnabled = enabled) }
    }

    fun setConvMix(mix: Float) {
        val v = mix.toInt().coerceIn(0, 100)
        controller.setParam(VeynFxController.P_CONV_MIX, v)
        update { it.copy(convMix = v) }
    }

    /**
     * Loads an IR wav picked by the user: bytes go straight to the
     * effect (LOAD_IR_DATA) and a private copy is kept so the IR
     * survives restarts without depending on the picked URI.
     */
    fun loadIr(uri: Uri) {
        val app = getApplication<Application>()
        val bytes = runCatching {
            app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (bytes == null || bytes.size < 44) {
            ToastHelper.showToast(app, "Couldn't read that IR file")
            return
        }
        if (bytes.size > VeynFxController.MAX_IR_BYTES) {
            ToastHelper.showToast(app, "IR too large (512 KB max)")
            return
        }
        val name = runCatching {
            app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "impulse.wav"
        runCatching { irFile().writeBytes(bytes) }
        controller.loadIrData(bytes)
        update { it.copy(convIrName = name) }
        ToastHelper.showToast(app, "IR loaded: $name")
    }

    fun clearIr() {
        runCatching { irFile().delete() }
        update { it.copy(convIrName = "") }
    }

    fun setMcompEnabled(enabled: Boolean) {
        controller.setParam(VeynFxController.P_MCOMP_ENABLE, if (enabled) 1 else 0)
        update { it.copy(mcompEnabled = enabled) }
    }

    private fun updateMcompBand(band: Int, transform: (VeynMcompBandUiState) -> VeynMcompBandUiState) {
        val bands = _uiState.value.mcompBands.toMutableList()
        if (band !in bands.indices) return
        bands[band] = transform(bands[band])
        val b = bands[band]
        controller.setBandParamSigned(
            VeynFxController.P_MCOMP_BAND_THRESHOLD, band, b.thresholdDb * 10
        )
        controller.setBandParam(VeynFxController.P_MCOMP_BAND_RATIO, band, b.ratio * 100)
        controller.setBandParam(VeynFxController.P_MCOMP_BAND_ATTACK, band, b.attackMs * 10)
        controller.setBandParam(VeynFxController.P_MCOMP_BAND_RELEASE, band, b.releaseMs * 10)
        controller.setBandParamSigned(
            VeynFxController.P_MCOMP_BAND_MAKEUP, band, b.makeupDb * 10
        )
        update { it.copy(mcompBands = bands) }
    }

    fun setMcompThresholdDb(band: Int, db: Float) {
        updateMcompBand(band) { it.copy(thresholdDb = db.toInt().coerceIn(-60, 0)) }
    }

    fun setMcompRatio(band: Int, ratio: Float) {
        updateMcompBand(band) { it.copy(ratio = ratio.toInt().coerceIn(1, 20)) }
    }

    fun setMcompAttackMs(band: Int, ms: Float) {
        updateMcompBand(band) { it.copy(attackMs = ms.toInt().coerceIn(1, 200)) }
    }

    fun setMcompReleaseMs(band: Int, ms: Float) {
        updateMcompBand(band) { it.copy(releaseMs = ms.toInt().coerceIn(10, 1000)) }
    }

    fun setMcompMakeupDb(band: Int, db: Float) {
        updateMcompBand(band) { it.copy(makeupDb = db.toInt().coerceIn(-12, 12)) }
    }

    fun setMcompXover(index: Int, hz: Float) {
        val xovers = _uiState.value.mcompXovers.toMutableList()
        if (index !in xovers.indices) return
        xovers[index] = hz.toInt().coerceIn(100, 12000)
        controller.setBandParam(
            VeynFxController.P_MCOMP_CROSSOVER, index, xovers[index]
        )
        update { it.copy(mcompXovers = xovers) }
    }

    fun resetAll() {
        val s = VeynFxUiState(available = _uiState.value.available)
        pushAll(s)
        _uiState.value = s
        persistState()
    }

    override fun onCleared() {
        controller.release()
        super.onCleared()
    }

    companion object {
        private const val IR_FILE_NAME = "veynfx_ir.wav"
    }
}
