/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby.geq.data

/**
 * Graphic-EQ precision. The DAP always stores 20 gains; each mode exposes a different subset and
 * fills the gaps the same way Lunaris does.
 */
enum class BandMode(
    val value: String,
    val displayName: String,
    val bandCount: Int,
    val frequencies: List<Int>,
) {
    TEN_BAND(
        "10",
        "10 Bands",
        10,
        listOf(32, 64, 125, 250, 500, 1_000, 2_250, 5_000, 10_000, 19_688),
    ),
    FIFTEEN_BAND(
        "15",
        "15 Bands",
        15,
        listOf(32, 47, 94, 141, 234, 469, 844, 1_313, 2_250, 3_750, 5_813, 9_000, 11_250, 13_875, 19_688),
    ),
    TWENTY_BAND(
        "20",
        "20 Bands",
        20,
        listOf(
            32,
            47,
            141,
            234,
            328,
            469,
            656,
            844,
            1_031,
            1_313,
            1_688,
            2_250,
            3_000,
            3_750,
            4_688,
            5_813,
            7_125,
            9_000,
            11_250,
            19_688,
        ),
    );

    /** Backend slot used by each visible band. */
    val indices: List<Int>
        get() =
            when (this) {
                TEN_BAND -> listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18)
                FIFTEEN_BAND -> listOf(0, 1, 2, 3, 4, 5, 6, 8, 11, 12, 14, 15, 17, 18, 19)
                TWENTY_BAND -> (0 until 20).toList()
            }

    /** Projects the stored 20 gains down to the bands this mode shows. */
    fun project(gains: List<BandGain>): List<BandGain> =
        indices.mapIndexed { index, rawIndex ->
            BandGain(
                band = frequencies.getOrElse(index) { rawIndex },
                gain = gains.getOrNull(rawIndex)?.gain ?: 0,
            )
        }

    /** Applies an edit made in this mode and returns the full 20 gains. */
    fun applyGain(gains: List<BandGain>, displayIndex: Int, gain: Int): List<BandGain> {
        val raw = MutableList(TWENTY_BAND.bandCount) { index -> gains.getOrNull(index)?.gain ?: 0 }
        val target = indices.getOrNull(displayIndex) ?: return gains
        raw[target] = gain.coerceIn(GAIN_MIN, GAIN_MAX)

        when (this) {
            TEN_BAND -> {
                for (index in 0 until TWENTY_BAND.bandCount - 1 step 2) {
                    if (index + 2 < TWENTY_BAND.bandCount) {
                        raw[index + 1] = (raw[index] + raw[index + 2]) / 2
                    }
                }
                raw[TWENTY_BAND.bandCount - 1] = raw[TWENTY_BAND.bandCount - 2]
            }
            FIFTEEN_BAND -> {
                val missing = (0 until TWENTY_BAND.bandCount).filter { it !in indices }
                missing.forEach { index ->
                    val previous = indices.filter { it < index }.maxOrNull() ?: 0
                    val next = indices.filter { it > index }.minOrNull() ?: 19
                    if (previous < index && next > index) {
                        val ratio = (index - previous).toFloat() / (next - previous)
                        raw[index] = (raw[previous] + ratio * (raw[next] - raw[previous])).toInt()
                    } else {
                        raw[index] = raw[previous]
                    }
                }
            }
            TWENTY_BAND -> Unit
        }

        return TWENTY_BAND.frequencies.mapIndexed { index, frequency ->
            BandGain(band = frequency, gain = raw[index])
        }
    }

    companion object {
        const val GAIN_MIN = -100
        const val GAIN_MAX = 100

        fun fromValue(value: Int): BandMode =
            values().firstOrNull { it.bandCount == value } ?: TEN_BAND

        fun fromString(value: String?): BandMode =
            values().firstOrNull { it.value == value } ?: TEN_BAND
    }
}
