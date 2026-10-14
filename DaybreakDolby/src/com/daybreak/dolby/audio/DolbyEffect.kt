/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby.audio

import android.media.audiofx.AudioEffect
import android.util.Log
import com.daybreak.dolby.audio.DspContract.EFFECT_TYPE_DAP
import com.daybreak.dolby.audio.DspContract.PARAM_CPDP_VALUES
import com.daybreak.dolby.audio.DspContract.PARAM_ENABLE
import com.daybreak.dolby.audio.DspContract.PARAM_GET_PROFILE_PARAMETER
import com.daybreak.dolby.audio.DspContract.PARAM_PROFILE
import com.daybreak.dolby.audio.DspContract.PARAM_RESET_PROFILE_SETTINGS
import com.daybreak.dolby.audio.DspContract.PARAM_SET_PROFILE_PARAMETER

/**
 * Thin [AudioEffect] wrapper speaking the DAX control protocol.
 * Stateless apart from the effect handle: no caching, no recovery, no
 * threads. Higher layers own lifecycle and retries.
 */
class DolbyEffect(priority: Int, audioSession: Int) : AudioEffect(
    EFFECT_TYPE_NULL, EFFECT_TYPE_DAP, priority, audioSession
) {

    var dsOn: Boolean
        get() = getIntParam(PARAM_ENABLE) == 1
        set(value) {
            setIntParam(PARAM_ENABLE, if (value) 1 else 0)
            enabled = value
        }

    var profile: Int
        get() = getIntParam(PARAM_PROFILE)
        set(value) {
            setIntParam(PARAM_PROFILE, value)
        }

    fun resetProfileSettings(profile: Int = this.profile) {
        dlog("resetProfileSettings: profile=$profile")
        setIntParam(PARAM_RESET_PROFILE_SETTINGS, profile)
    }

    fun setDapParameter(param: DsParam, values: IntArray, profile: Int = this.profile) {
        dlog("setDapParameter: profile=$profile param=$param")
        val length = values.size
        val buf = ByteArray((length + 4) * 4)
        int32ToByteArray(PARAM_SET_PROFILE_PARAMETER, buf, 0)
        int32ToByteArray(length + 1, buf, 4)
        int32ToByteArray(profile, buf, 8)
        int32ToByteArray(param.id, buf, 12)
        int32ArrayToByteArray(values, buf, 16)
        checkStatus(setParameter(PARAM_CPDP_VALUES, buf))
    }

    fun setDapParameter(param: DsParam, enable: Boolean, profile: Int = this.profile) =
        setDapParameter(param, intArrayOf(if (enable) 1 else 0), profile)

    fun setDapParameter(param: DsParam, value: Int, profile: Int = this.profile) =
        setDapParameter(param, intArrayOf(value), profile)

    fun getDapParameter(param: DsParam, profile: Int = this.profile): IntArray {
        dlog("getDapParameter: profile=$profile param=$param")
        val length = param.length
        val buf = ByteArray((length + 2) * 4)
        val p = (param.id shl 16) + (profile shl 8) + PARAM_GET_PROFILE_PARAMETER
        checkStatus(getParameter(p, buf))
        return byteArrayToInt32Array(buf, length)
    }

    fun getDapParameterBool(param: DsParam, profile: Int = this.profile): Boolean =
        getDapParameter(param, profile)[0] == 1

    fun getDapParameterInt(param: DsParam, profile: Int = this.profile): Int =
        getDapParameter(param, profile)[0]

    companion object {
        private const val TAG = "DaybreakEffect"

        private fun DolbyEffect.setIntParam(param: Int, value: Int) {
            dlog("setIntParam($param, $value)")
            val buf = ByteArray(12)
            int32ToByteArray(param, buf, 0)
            int32ToByteArray(1, buf, 4)
            int32ToByteArray(value, buf, 8)
            checkStatus(setParameter(PARAM_CPDP_VALUES, buf))
        }

        private fun DolbyEffect.getIntParam(param: Int): Int {
            val buf = ByteArray(12)
            int32ToByteArray(param, buf, 0)
            checkStatus(getParameter(PARAM_CPDP_VALUES + param, buf))
            return byteArrayToInt32(buf).also {
                dlog("getIntParam($param): $it")
            }
        }

        private fun checkStatus(status: Int) {
            if (status != AudioEffect.SUCCESS) {
                throw RuntimeException("DolbyEffect error: $status")
            }
        }

        private fun int32ToByteArray(value: Int, dst: ByteArray, index: Int) {
            var idx = index
            dst[idx++] = (value and 0xff).toByte()
            dst[idx++] = ((value ushr 8) and 0xff).toByte()
            dst[idx++] = ((value ushr 16) and 0xff).toByte()
            dst[idx] = ((value ushr 24) and 0xff).toByte()
        }

        private fun byteArrayToInt32(ba: ByteArray): Int {
            return ((ba[3].toInt() and 0xff) shl 24) or
                ((ba[2].toInt() and 0xff) shl 16) or
                ((ba[1].toInt() and 0xff) shl 8) or
                (ba[0].toInt() and 0xff)
        }

        private fun int32ArrayToByteArray(src: IntArray, dst: ByteArray, index: Int) {
            var idx = index
            for (x in src) {
                dst[idx++] = (x and 0xff).toByte()
                dst[idx++] = ((x ushr 8) and 0xff).toByte()
                dst[idx++] = ((x ushr 16) and 0xff).toByte()
                dst[idx++] = ((x ushr 24) and 0xff).toByte()
            }
        }

        private fun byteArrayToInt32Array(ba: ByteArray, length: Int): IntArray {
            return IntArray(length) { i ->
                byteArrayToInt32(ba.copyOfRange(i * 4, i * 4 + 4))
            }
        }

        private fun dlog(msg: String) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, msg)
            }
        }
    }
}
