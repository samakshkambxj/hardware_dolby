/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.data

import android.content.Context

/** User EQ presets. Keyed by name; re-saving overwrites. */
class PresetStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        DolbyKeys.FILE_PRESETS,
        Context.MODE_PRIVATE,
    )

    fun names(): Set<String> = prefs.all.keys.toSet()

    fun gains(name: String, bandCount: Int): List<Int>? =
        GainsCodec.deserialize(prefs.getString(name, null), bandCount)

    fun save(name: String, gains: List<Int>) {
        prefs.edit().putString(name, GainsCodec.serialize(gains)).apply()
    }

    fun delete(name: String) {
        prefs.edit().remove(name).apply()
    }

    fun exists(name: String): Boolean = prefs.contains(name)
}
