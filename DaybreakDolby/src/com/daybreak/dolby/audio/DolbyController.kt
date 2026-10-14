/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.audio

import android.content.Context
import android.util.Log
import com.daybreak.dolby.audio.DspContract.EFFECT_PRIORITY
import com.daybreak.dolby.data.DolbyKeys
import com.daybreak.dolby.data.ProfileStore
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Sole owner of the DAX effect handle. Every HAL call runs on one serial
 * dispatcher, so concurrent UI writes, boot restore, and device callbacks
 * can never interleave mid-sequence. All functions are safe to call from
 * anywhere; all blocking work stays off the caller.
 */
class DolbyController private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dispatcher = Executors.newSingleThreadExecutor {
        Thread(it, "daybreak-dsp")
    }.asCoroutineDispatcher()

    val store = ProfileStore(appContext)

    private var effect: DolbyEffect? = null

    private suspend fun <T> onDsp(block: suspend (DolbyEffect) -> T): T =
        withContext(dispatcher) {
            var handle = effect
            if (handle == null || !safeHasControl(handle)) {
                try {
                    handle?.release()
                } catch (_: Exception) {
                }
                handle = DolbyEffect(EFFECT_PRIORITY, 0)
                effect = handle
            }
            block(handle!!)
        }

    private fun safeHasControl(handle: DolbyEffect): Boolean {
        return try {
            handle.hasControl()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun setEnabled(on: Boolean) = onDsp { handle ->
        runCatching { handle.dsOn = on }
        store.enabled = on
        if (on) applyProfileLocked(handle, store.profile)
    }

    suspend fun isEnabled(): Boolean = onDsp { handle ->
        runCatching { handle.dsOn }.getOrDefault(store.enabled)
    }

    suspend fun setProfile(id: Int) = onDsp { handle ->
        runCatching { handle.profile = id }
        store.profile = id
        applyProfileLocked(handle, id)
    }

    suspend fun currentProfile(): Int = onDsp { handle ->
        runCatching { handle.profile }.getOrDefault(store.profile)
    }

    suspend fun setBool(param: DsParam, key: String, value: Boolean, profile: Int? = null) =
        onDsp { handle ->
            val target = profile ?: store.profile
            runCatching { handle.setDapParameter(param, value, target) }
            store.setBool(target, key, value)
        }

    suspend fun getBool(param: DsParam, key: String, default: Boolean, profile: Int? = null): Boolean =
        onDsp { handle ->
            val target = profile ?: store.profile
            runCatching { handle.getDapParameterBool(param, target) }
                .getOrDefault(store.bool(target, key, default))
        }

    suspend fun setAmount(param: DsParam, key: String, value: Int, profile: Int? = null) =
        onDsp { handle ->
            val target = profile ?: store.profile
            runCatching { handle.setDapParameter(param, value, target) }
            store.setInt(target, key, value)
        }

    suspend fun getAmount(param: DsParam, key: String, default: Int, profile: Int? = null): Int =
        onDsp { handle ->
            val target = profile ?: store.profile
            runCatching { handle.getDapParameterInt(param, target) }
                .getOrDefault(store.int(target, key, default))
        }

    suspend fun setGains(gains: List<Int>, profile: Int? = null) = onDsp { handle ->
        val target = profile ?: store.profile
        runCatching {
            handle.setDapParameter(
                DsParam.GEQ_BAND_GAINS,
                gains.toIntArray(),
                target,
            )
        }
        store.setPreset(target, gains.joinToString(","))
    }

    suspend fun getGains(profile: Int? = null): List<Int>? = onDsp { handle ->
        val target = profile ?: store.profile
        val live = runCatching { handle.getDapParameter(DsParam.GEQ_BAND_GAINS, target).toList() }
            .getOrNull()
        live ?: store.preset(target)?.let {
            com.daybreak.dolby.data.GainsCodec.deserialize(it, DsParam.GEQ_BAND_GAINS.length)
        }
    }

    suspend fun resetProfile(profile: Int? = null) = onDsp { handle ->
        val target = profile ?: store.profile
        runCatching { handle.resetProfileSettings(target) }
        store.clearProfile(target)
        applyProfileLocked(handle, target)
    }

    /** Boot/death restore: enable flag, current profile, all its values. */
    suspend fun bootRestore() = onDsp { handle ->
        runCatching { handle.dsOn = store.enabled }
        val profile = store.profile
        runCatching { handle.profile = profile }
        applyProfileLocked(handle, profile)
    }

    private fun applyProfileLocked(handle: DolbyEffect, profile: Int) {
        val snapshot = store.snapshot(profile)
        val failures = mutableListOf<String>()
        fun attempt(name: String, block: () -> Unit) {
            runCatching(block).onFailure {
                failures += name
                Log.w(TAG, "restore $name failed: ${it.message}")
            }
        }
        attempt("gains") {
            snapshot.gains?.let {
                handle.setDapParameter(DsParam.GEQ_BAND_GAINS, it.toIntArray(), profile)
            }
        }
        attempt("ieq") { handle.setDapParameter(DsParam.IEQ_PRESET, snapshot.ieq, profile) }
        attempt("hpVirt") {
            handle.setDapParameter(DsParam.HEADPHONE_VIRTUALIZER, snapshot.hpVirt, profile)
        }
        attempt("spkVirt") {
            handle.setDapParameter(DsParam.SPEAKER_VIRTUALIZER, snapshot.spkVirt, profile)
        }
        attempt("stereo") {
            handle.setDapParameter(DsParam.STEREO_WIDENING_AMOUNT, snapshot.stereo, profile)
        }
        attempt("dialogue") {
            handle.setDapParameter(DsParam.DIALOGUE_ENHANCER_ENABLE, snapshot.dialogue > 0, profile)
            handle.setDapParameter(DsParam.DIALOGUE_ENHANCER_AMOUNT, snapshot.dialogue, profile)
        }
        attempt("bass") {
            handle.setDapParameter(DsParam.BASS_ENHANCER_ENABLE, snapshot.bass, profile)
        }
        attempt("leveler") {
            handle.setDapParameter(DsParam.VOLUME_LEVELER_ENABLE, snapshot.leveler, profile)
        }
        if (failures.isNotEmpty()) {
            Log.w(TAG, "profile $profile restored with failures: $failures")
        }
    }

    companion object {
        private const val TAG = "DaybreakDsp"

        @Volatile
        private var instance: DolbyController? = null

        fun get(context: Context): DolbyController =
            instance ?: synchronized(this) {
                instance ?: DolbyController(context).also { instance = it }
            }
    }
}
