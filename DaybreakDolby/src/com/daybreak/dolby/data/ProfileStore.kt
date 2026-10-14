/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.data

import android.content.Context
import android.content.SharedPreferences
import com.daybreak.dolby.audio.DsParam

/**
 * Per-profile DSP values. Single-threaded by contract: only touched from
 * the controller dispatcher, so no locks and no torn multi-key writes are
 * possible by construction.
 */
class ProfileStore(context: Context) {

    private val appContext = context.applicationContext
    private val global: SharedPreferences by lazy {
        appContext.getSharedPreferences(DolbyKeys.FILE_GLOBAL, Context.MODE_PRIVATE)
    }

    var enabled: Boolean
        get() = global.getBoolean(DolbyKeys.ENABLED, true)
        set(value) = global.edit().putBoolean(DolbyKeys.ENABLED, value).apply()

    var profile: Int
        get() = global.getString(DolbyKeys.PROFILE, "0")?.toIntOrNull() ?: 0
        set(value) = global.edit().putString(DolbyKeys.PROFILE, value.toString()).apply()

    private fun prefs(profile: Int): SharedPreferences =
        appContext.getSharedPreferences(
            DolbyKeys.profileFile(profile),
            Context.MODE_PRIVATE,
        )

    fun clearProfile(profile: Int): Boolean =
        prefs(profile).edit().clear().commit()

    fun preset(profile: Int): String? =
        prefs(profile).getString(DolbyKeys.PRESET, null)

    fun setPreset(profile: Int, gains: String) {
        prefs(profile).edit().putString(DolbyKeys.PRESET, gains).apply()
    }

    fun int(profile: Int, key: String, default: Int): Int =
        prefs(profile).getString(key, null)?.toIntOrNull()
            ?: prefs(profile).getInt(key, default)

    fun setInt(profile: Int, key: String, value: Int) {
        prefs(profile).edit().putString(key, value.toString()).apply()
    }

    fun bool(profile: Int, key: String, default: Boolean): Boolean =
        prefs(profile).getBoolean(key, default)

    fun setBool(profile: Int, key: String, value: Boolean) {
        prefs(profile).edit().putBoolean(key, value).apply()
    }

    /** Snapshot of every known DSP value for one profile. */
    fun snapshot(profile: Int): ProfileSnapshot {
        val gains = GainsCodec.deserialize(
            preset(profile), DsParam.GEQ_BAND_GAINS.length,
        )
        return ProfileSnapshot(
            ieq = int(profile, DolbyKeys.IEQ, 0),
            hpVirt = bool(profile, DolbyKeys.HP_VIRTUALIZER, false),
            spkVirt = bool(profile, DolbyKeys.SPK_VIRTUALIZER, false),
            stereo = int(profile, DolbyKeys.STEREO, 0),
            dialogue = int(profile, DolbyKeys.DIALOGUE, 0),
            bass = bool(profile, DolbyKeys.BASS, false),
            leveler = bool(profile, DolbyKeys.LEVELER, false),
            gains = gains,
        )
    }

    fun restore(profile: Int, snapshot: ProfileSnapshot) {
        val editor = prefs(profile).edit()
        editor.putString(DolbyKeys.IEQ, snapshot.ieq.toString())
        editor.putBoolean(DolbyKeys.HP_VIRTUALIZER, snapshot.hpVirt)
        editor.putBoolean(DolbyKeys.SPK_VIRTUALIZER, snapshot.spkVirt)
        editor.putString(DolbyKeys.STEREO, snapshot.stereo.toString())
        editor.putString(DolbyKeys.DIALOGUE, snapshot.dialogue.toString())
        editor.putBoolean(DolbyKeys.BASS, snapshot.bass)
        editor.putBoolean(DolbyKeys.LEVELER, snapshot.leveler)
        snapshot.gains?.let {
            editor.putString(DolbyKeys.PRESET, GainsCodec.serialize(it))
        }
        editor.apply()
    }
}

/** Null gains = never tuned; the HAL default applies. */
data class ProfileSnapshot(
    val ieq: Int,
    val hpVirt: Boolean,
    val spkVirt: Boolean,
    val stereo: Int,
    val dialogue: Int,
    val bass: Boolean,
    val leveler: Boolean,
    val gains: List<Int>?,
)
