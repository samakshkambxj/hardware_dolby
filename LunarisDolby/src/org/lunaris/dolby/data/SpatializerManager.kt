/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lunaris.dolby.DolbyConstants

/**
 * Runtime wrapper around the framework Spatializer (API 33+).
 *
 * This controls the AOSP spatial-audio pipeline (which routes through
 * libswspatializer on this device), NOT the Dolby DAP virtualizer params.
 * All calls are guarded so the UI can render an "unsupported" state on
 * devices/outputs without a spatializer.
 *
 * Writes ([setEnabled], head-tracker set, state listener) are SystemApi gated
 * by MODIFY_DEFAULT_AUDIO_EFFECTS — declared in the manifest and allowlisted
 * in privapp-permissions-dolby.xml. Without it the platform throws
 * SecurityException, the set silently no-ops, and the next [refresh]
 * re-reads the old value (toggle snapping back). Callers must surface a
 * `false` return to the user instead of assuming success.
 */
class SpatializerManager(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager: AudioManager? =
        runCatching { appContext.getSystemService(AudioManager::class.java) }.getOrNull()

    private val _isSupported = MutableStateFlow(isSpatializerApiPresent())
    val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _isHeadTrackingAvailable = MutableStateFlow(false)
    val isHeadTrackingAvailable: StateFlow<Boolean> = _isHeadTrackingAvailable.asStateFlow()

    private val _isHeadTrackingEnabled = MutableStateFlow(false)
    val isHeadTrackingEnabled: StateFlow<Boolean> = _isHeadTrackingEnabled.asStateFlow()

    /**
     * System-side callback so external changes (Settings toggle, output
     * switch) reflect here live instead of only on next resume/refresh.
     * Same permission gate as the setters, so registration is a no-op
     * without MODIFY_DEFAULT_AUDIO_EFFECTS.
     */
    private val stateListener =
        object : android.media.Spatializer.OnSpatializerStateChangedListener {
            override fun onSpatializerAvailableChanged(
                spat: android.media.Spatializer,
                available: Boolean
            ) {
                refresh()
            }

            override fun onSpatializerEnabledChanged(
                spat: android.media.Spatializer,
                enabled: Boolean
            ) {
                refresh()
            }
        }
    private var listenerRegistered = false

    init {
        refresh()
        registerStateListener()
    }

    private fun registerStateListener() {
        if (listenerRegistered || !_isSupported.value) return
        listenerRegistered = runCatching {
            val spatializer = audioManager?.spatializer ?: return@runCatching false
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@runCatching false
            spatializer.addOnSpatializerStateChangedListener(
                appContext.mainExecutor,
                stateListener
            )
            true
        }.getOrDefault(false)
    }

    /** Unregister the system callback. Call from ViewModel.onCleared(). */
    fun destroy() {
        if (!listenerRegistered) return
        listenerRegistered = false
        runCatching {
            audioManager?.spatializer
                ?.removeOnSpatializerStateChangedListener(stateListener)
        }
    }

    private fun isSpatializerApiPresent(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val am = audioManager ?: return false
        return runCatching { am.spatializer != null }.getOrDefault(false)
    }

    /** Re-query framework state. Call on resume / output-device change. */
    fun refresh() {
        if (!_isSupported.value) {
            _isAvailable.value = false
            _isEnabled.value = false
            _isHeadTrackingAvailable.value = false
            _isHeadTrackingEnabled.value = false
            return
        }
        try {
            val spatializer = audioManager?.spatializer ?: run {
                _isSupported.value = false
                return
            }
            _isAvailable.value = runCatching { spatializer.isAvailable }.getOrDefault(false)
            _isEnabled.value = runCatching { spatializer.isEnabled }.getOrDefault(false)
            _isHeadTrackingAvailable.value =
                runCatching { spatializer.isHeadTrackerAvailable }.getOrDefault(false)
            _isHeadTrackingEnabled.value = queryHeadTrackerEnabled(spatializer)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Spatializer refresh failed: ${e.message}")
        }
    }

    /** Returns false when the platform rejects the call. */
    fun setEnabled(enabled: Boolean): Boolean {
        if (!_isSupported.value) return false
        return try {
            val spatializer = audioManager?.spatializer ?: return false
            spatializer.isEnabled = enabled
            _isEnabled.value = runCatching { spatializer.isEnabled }.getOrDefault(enabled)
            true
        } catch (e: SecurityException) {
            DolbyConstants.dlog(TAG, "Spatializer setEnabled denied: ${e.message}")
            false
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Spatializer setEnabled failed: ${e.message}")
            false
        }
    }

    fun setHeadTrackingEnabled(enabled: Boolean): Boolean {
        if (!_isSupported.value || !_isHeadTrackingAvailable.value) return false
        return try {
            val spatializer = audioManager?.spatializer ?: return false
            if (!applyHeadTrackerEnabled(spatializer, enabled)) return false
            // Optimistic: per-device query can lag behind the set call;
            // refresh() reconciles on next resume/device change.
            _isHeadTrackingEnabled.value = enabled
            true
        } catch (e: SecurityException) {
            DolbyConstants.dlog(TAG, "HeadTracking set denied: ${e.message}")
            false
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "HeadTracking set failed: ${e.message}")
            false
        }
    }

    /**
     * Current platform (API 35+) scopes head-tracker state per output device:
     * `isHeadTrackerEnabled(AudioDeviceAttributes)` /
     * `setHeadTrackerEnabled(Boolean, AudioDeviceAttributes)`.
     * The old no-arg property no longer exists, so query/set across all
     * compatible devices instead.
     */
    private fun queryHeadTrackerEnabled(
        spatializer: android.media.Spatializer
    ): Boolean {
        return runCatching {
            val devices = spatializer.compatibleAudioDevices
            devices.any { device ->
                runCatching { spatializer.isHeadTrackerEnabled(device) }.getOrDefault(false)
            }
        }.getOrDefault(false)
    }

    /** Returns false when there is no device to apply to. */
    private fun applyHeadTrackerEnabled(
        spatializer: android.media.Spatializer,
        enabled: Boolean
    ): Boolean {
        val devices = runCatching { spatializer.compatibleAudioDevices }
            .getOrDefault(emptyList())
        if (devices.isEmpty()) return false
        var applied = false
        devices.forEach { device ->
            val hasTracker =
                runCatching { spatializer.hasHeadTracker(device) }.getOrDefault(true)
            if (hasTracker) {
                spatializer.setHeadTrackerEnabled(enabled, device)
                applied = true
            }
        }
        return applied
    }

    companion object {
        private const val TAG = "SpatializerManager"
    }
}
