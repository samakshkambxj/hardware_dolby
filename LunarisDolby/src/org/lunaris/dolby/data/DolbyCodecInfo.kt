/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.media.MediaCodecList
import org.lunaris.dolby.DolbyConstants

/**
 * Runtime query of the Dolby decoders wired through
 * media_codecs_dolby_audio.xml (AC-3 / E-AC-3 / E-AC-3 JOC for Atmos /
 * AC-4). Read-only: whether a decoder exists and its channel ceiling.
 * Must be called off the main thread.
 */
data class DolbyCodecSupport(
    val label: String,
    val mime: String,
    val supported: Boolean,
    val decoderName: String?,
    val maxChannels: Int?
)

object DolbyCodecInfo {

    private val DOLBY_CODECS = listOf(
        "Dolby Digital" to "audio/ac3",
        "Dolby Digital Plus" to "audio/eac3",
        "Dolby Atmos (E-AC-3 JOC)" to "audio/eac3-joc",
        "Dolby AC-4" to "audio/ac4"
    )

    fun query(): List<DolbyCodecSupport> {
        val infos = try {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "MediaCodecList unavailable: ${e.message}")
            return DOLBY_CODECS.map { (label, mime) ->
                DolbyCodecSupport(label, mime, false, null, null)
            }
        }
        return DOLBY_CODECS.map { (label, mime) ->
            var decoderName: String? = null
            var maxChannels: Int? = null
            for (info in infos) {
                if (info.isEncoder) continue
                if (!info.supportedTypes.contains(mime)) continue
                if (decoderName == null) decoderName = info.name
                val channels = runCatching {
                    info.getCapabilitiesForType(mime)
                        .audioCapabilities?.maxInputChannelCount
                }.getOrNull()
                if (channels != null && (maxChannels == null || channels > maxChannels)) {
                    maxChannels = channels
                }
            }
            DolbyCodecSupport(
                label = label,
                mime = mime,
                supported = decoderName != null,
                decoderName = decoderName,
                maxChannels = maxChannels
            )
        }
    }

    private const val TAG = "DolbyCodecInfo"
}
