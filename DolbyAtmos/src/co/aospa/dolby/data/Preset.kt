/*
 * Copyright (C) 2024 Paranoid Android
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.data

/**
 * A graphic-EQ preset. [gains] always holds the full 20 DAP bands so it can be
 * applied directly; the UI only edits the 10 even bands.
 */
data class Preset(
    val name: String,
    val gains: List<Int>,
    val isUserDefined: Boolean = false,
) {
    fun matches(other: List<Int>): Boolean = gains == other
}
