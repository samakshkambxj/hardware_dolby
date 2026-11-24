/*
 * Copyright (C) 2026 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.ui

import androidx.annotation.DrawableRes

/** A selectable Dolby listening profile. */
data class DolbyProfile(
    val value: Int,
    val name: String,
    @param:DrawableRes val iconRes: Int,
)

/** One option of the intelligent equalizer. */
data class IeqChoice(
    val value: Int,
    val name: String,
    @param:DrawableRes val iconRes: Int,
)

/** A labelled stop of a discrete level control (dialogue enhancer, stereo widening, bass curve). */
data class LevelChoice(
    val value: Int,
    val label: String,
)

/** Number of bands shown in the equalizer. The DSP always exposes 20. */
enum class BandMode(val bands: Int, val label: String) {
    TEN(10, "10"),
    FIFTEEN(15, "15"),
    TWENTY(20, "20");

    /**
     * Indices into the 20-band DSP array that this mode exposes. 10-band keeps the historical
     * mapping used by the old equalizer screen (every even index).
     */
    val backendIndices: List<Int>
        get() =
            when (this) {
                TEN -> listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18)
                FIFTEEN -> listOf(0, 1, 2, 3, 4, 5, 6, 8, 11, 12, 14, 15, 17, 18, 19)
                TWENTY -> List(20) { it }
            }

    companion object {
        fun fromValue(value: String?): BandMode =
            when (value) {
                "15" -> FIFTEEN
                "20" -> TWENTY
                else -> TEN
            }
    }
}

/** Immutable snapshot of everything the Dolby UI renders. */
data class DolbyUiState(
    val enabled: Boolean = false,
    val profile: Int = 0,
    val profileKnown: Boolean = false,
    val profileName: String = "",
    val presetName: String = "",
    val ieq: Int = 0,
    val speakerVirtualizer: Boolean = false,
    val headphoneVirtualizer: Boolean = false,
    val stereoWidening: Int = 0,
    val dialogueEnhancer: Int = 0,
    val bassEnhancer: Boolean = false,
    val volumeLeveler: Boolean = false,
    val isOnSpeaker: Boolean = true,
    val stereoWideningSupported: Boolean = true,
    val bassLevel: Int = 50,
    val bassCurve: Int = 0,
    val midEnhancer: Boolean = false,
    val midLevel: Int = 40,
    val trebleEnhancer: Boolean = false,
    val trebleLevel: Int = 30,
    /** Raw 20-band DSP gains, in 0.1 dB units. */
    val bandGains: List<Int> = List(BACKEND_BANDS) { 0 },
    val bandMode: BandMode = BandMode.TEN,
    val profiles: List<DolbyProfile> = emptyList(),
    val ieqChoices: List<IeqChoice> = emptyList(),
    val dialogueChoices: List<LevelChoice> = emptyList(),
    val stereoChoices: List<LevelChoice> = emptyList(),
    val bassCurveChoices: List<LevelChoice> = emptyList(),
) {
    /** Per-profile controls are only meaningful while Dolby is on and the profile is known. */
    val controlsEnabled: Boolean
        get() = enabled && profileKnown

    val bandCount: Int
        get() = bandMode.bands

    /** Gains exposed by the current band mode, in 0.1 dB units. */
    val visibleGains: List<Int>
        get() = bandMode.backendIndices.map { bandGains.getOrElse(it) { 0 } }

    /** Frequencies in Hz for the current band mode, derived from the 20-band layout. */
    val visibleFrequencies: List<Int>
        get() = bandMode.backendIndices.map { BACKEND_FREQUENCIES[it] }

    companion object {
        const val BACKEND_BANDS = 20

        val BACKEND_FREQUENCIES =
            listOf(
                32,
                47,
                141,
                234,
                328,
                469,
                656,
                844,
                1031,
                1313,
                1688,
                2250,
                3000,
                3750,
                4688,
                5813,
                7125,
                9000,
                11250,
                19688,
            )

        fun formatFrequency(frequency: Int): String =
            if (frequency >= 1000) {
                val kilo = frequency / 1000f
                if (kilo % 1f == 0f) "${kilo.toInt()}k" else "%.1fk".format(kilo)
            } else {
                "$frequency"
            }

        fun formatGain(tenths: Int): String {
            val value = tenths / 10f
            val sign = if (value > 0) "+" else ""
            return "$sign%.1f".format(value)
        }
    }
}
