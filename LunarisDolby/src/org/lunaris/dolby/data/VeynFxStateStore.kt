/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the VeynFx Stage-A state. Value conventions match
 * [org.lunaris.dolby.audio.VeynFxController]: percent-style ints for
 * /100 engine params, device units (dB, ms, Hz) for raw params.
 * The convolver IR itself lives as a private file; only its display
 * name is stored here.
 */
data class VeynFxStateData(
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
    val mcompThresholds: List<Int> = List(4) { -20 },
    val mcompRatios: List<Int> = List(4) { 4 },
    val mcompAttacks: List<Int> = List(4) { 5 },
    val mcompReleases: List<Int> = List(4) { 50 },
    val mcompMakeups: List<Int> = List(4) { 0 },
    val mcompXovers: List<Int> = listOf(200, 1000, 5000)
)

class VeynFxStateStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): VeynFxStateData? {
        val raw = prefs.getString(KEY_STATE, null) ?: return null
        return try {
            val json = JSONObject(raw)

            fun intList(key: String, fallback: List<Int>): List<Int> {
                val arr = json.optJSONArray(key) ?: return fallback
                return (0 until arr.length()).map { arr.optInt(it, fallback.getOrElse(it) { 0 }) }
            }

            VeynFxStateData(
                masterEnabled = json.optBoolean("masterEnabled", false),
                outputGain = json.optInt("outputGain", 100),
                widenerEnabled = json.optBoolean("widenerEnabled", false),
                widenerWidth = json.optInt("widenerWidth", 100),
                tubeEnabled = json.optBoolean("tubeEnabled", false),
                tubeDrive = json.optInt("tubeDrive", 50),
                tubeMix = json.optInt("tubeMix", 100),
                exciterEnabled = json.optBoolean("exciterEnabled", false),
                exciterDrive = json.optInt("exciterDrive", 50),
                exciterBlend = json.optInt("exciterBlend", 50),
                exciterFreq = json.optInt("exciterFreq", 8000),
                xfeedEnabled = json.optBoolean("xfeedEnabled", false),
                xfeedLevel = json.optInt("xfeedLevel", 30),
                xfeedCutoff = json.optInt("xfeedCutoff", 700),
                agcEnabled = json.optBoolean("agcEnabled", false),
                agcTargetDb = json.optInt("agcTargetDb", -20),
                agcMaxGainDb = json.optInt("agcMaxGainDb", 10),
                agcSpeed = json.optInt("agcSpeed", 50),
                compEnabled = json.optBoolean("compEnabled", false),
                compThresholdDb = json.optInt("compThresholdDb", -20),
                compRatio = json.optInt("compRatio", 4),
                compAttackMs = json.optInt("compAttackMs", 10),
                compReleaseMs = json.optInt("compReleaseMs", 100),
                compKneeDb = json.optInt("compKneeDb", 5),
                compMakeupDb = json.optInt("compMakeupDb", 0),
                surrEnabled = json.optBoolean("surrEnabled", false),
                surrDelay = json.optInt("surrDelay", 20),
                surrWidth = json.optInt("surrWidth", 50),
                spatEnabled = json.optBoolean("spatEnabled", false),
                spatWidth = json.optInt("spatWidth", 50),
                spatBlend = json.optInt("spatBlend", 100),
                spatHrtf = json.optInt("spatHrtf", 0),
                convEnabled = json.optBoolean("convEnabled", false),
                convMix = json.optInt("convMix", 100),
                convIrName = json.optString("convIrName", ""),
                mcompEnabled = json.optBoolean("mcompEnabled", false),
                mcompThresholds = intList("mcompThresholds", List(4) { -20 }),
                mcompRatios = intList("mcompRatios", List(4) { 4 }),
                mcompAttacks = intList("mcompAttacks", List(4) { 5 }),
                mcompReleases = intList("mcompReleases", List(4) { 50 }),
                mcompMakeups = intList("mcompMakeups", List(4) { 0 }),
                mcompXovers = intList("mcompXovers", listOf(200, 1000, 5000))
            )
        } catch (e: Exception) {
            null
        }
    }

    fun save(state: VeynFxStateData) {
        fun jsonArray(values: List<Int>): JSONArray {
            val arr = JSONArray()
            values.forEach { arr.put(it) }
            return arr
        }

        val json = JSONObject().apply {
            put("masterEnabled", state.masterEnabled)
            put("outputGain", state.outputGain)
            put("widenerEnabled", state.widenerEnabled)
            put("widenerWidth", state.widenerWidth)
            put("tubeEnabled", state.tubeEnabled)
            put("tubeDrive", state.tubeDrive)
            put("tubeMix", state.tubeMix)
            put("exciterEnabled", state.exciterEnabled)
            put("exciterDrive", state.exciterDrive)
            put("exciterBlend", state.exciterBlend)
            put("exciterFreq", state.exciterFreq)
            put("xfeedEnabled", state.xfeedEnabled)
            put("xfeedLevel", state.xfeedLevel)
            put("xfeedCutoff", state.xfeedCutoff)
            put("agcEnabled", state.agcEnabled)
            put("agcTargetDb", state.agcTargetDb)
            put("agcMaxGainDb", state.agcMaxGainDb)
            put("agcSpeed", state.agcSpeed)
            put("compEnabled", state.compEnabled)
            put("compThresholdDb", state.compThresholdDb)
            put("compRatio", state.compRatio)
            put("compAttackMs", state.compAttackMs)
            put("compReleaseMs", state.compReleaseMs)
            put("compKneeDb", state.compKneeDb)
            put("compMakeupDb", state.compMakeupDb)
            put("surrEnabled", state.surrEnabled)
            put("surrDelay", state.surrDelay)
            put("surrWidth", state.surrWidth)
            put("spatEnabled", state.spatEnabled)
            put("spatWidth", state.spatWidth)
            put("spatBlend", state.spatBlend)
            put("spatHrtf", state.spatHrtf)
            put("convEnabled", state.convEnabled)
            put("convMix", state.convMix)
            put("convIrName", state.convIrName)
            put("mcompEnabled", state.mcompEnabled)
            put("mcompThresholds", jsonArray(state.mcompThresholds))
            put("mcompRatios", jsonArray(state.mcompRatios))
            put("mcompAttacks", jsonArray(state.mcompAttacks))
            put("mcompReleases", jsonArray(state.mcompReleases))
            put("mcompMakeups", jsonArray(state.mcompMakeups))
            put("mcompXovers", jsonArray(state.mcompXovers))
        }
        prefs.edit().putString(KEY_STATE, json.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_STATE).apply()
    }

    companion object {
        private const val PREFS_NAME = "veynfx_state"
        private const val KEY_STATE = "state"
    }
}
