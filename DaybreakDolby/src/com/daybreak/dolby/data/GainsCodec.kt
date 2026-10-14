/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.data

/** Single home for GEQ gain list serialization. */
object GainsCodec {
    fun serialize(gains: List<Int>): String = gains.joinToString(",")

    fun deserialize(raw: String?, bandCount: Int): List<Int>? {
        if (raw.isNullOrEmpty()) return null
        val values = raw.split(",").mapNotNull { it.toIntOrNull() }
        if (values.size != bandCount) return null
        return values
    }

    /** Preset match with unit tolerance (HAL rounds trip values). */
    fun matches(a: List<Int>, b: List<Int>): Boolean {
        if (a.size != b.size) return false
        return a.indices.all { kotlin.math.abs(a[it] - b[it]) <= 1 }
    }
}
