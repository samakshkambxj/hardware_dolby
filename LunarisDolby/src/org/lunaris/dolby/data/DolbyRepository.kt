/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.provider.Settings
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.DolbyConstants.DsParam
import org.lunaris.dolby.R
import org.lunaris.dolby.audio.DolbyAudioEffect
import org.lunaris.dolby.domain.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DolbyRepository(private val context: Context) : AutoCloseable {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var dolbyEffect = createDolbyEffect()
    
    private val defaultPrefs = context.getSharedPreferences("dolby_prefs", Context.MODE_PRIVATE)
    private val presetsPrefs = context.getSharedPreferences(DolbyConstants.PREF_FILE_PRESETS, Context.MODE_PRIVATE)
    
    private val deviceStateManager = DeviceStateManager(context)

    private val _activeAudioDevice = MutableStateFlow(resolveActiveAudioDevice())
    val activeAudioDevice: StateFlow<ActiveAudioDevice> = _activeAudioDevice.asStateFlow()

    private val _isOnSpeaker = MutableStateFlow(_activeAudioDevice.value.isOnSpeaker)
    val isOnSpeaker: StateFlow<Boolean> = _isOnSpeaker.asStateFlow()
    
    private val _currentProfile = MutableStateFlow(0)
    val currentProfile: StateFlow<Int> = _currentProfile.asStateFlow()

    val stereoWideningSupported = context.resources.getBoolean(R.bool.dolby_stereo_widening_supported)
    val volumeLevelerSupported = context.resources.getBoolean(R.bool.dolby_volume_leveler_supported)
    
    private var isReleased = false
    
    private var cachedPresets: List<EqualizerPreset>? = null
    private val presetCacheLock = Any()

    private fun createDolbyEffect(): DolbyAudioEffect {
        return try {
            DolbyAudioEffect(EFFECT_PRIORITY, audioSession = 0)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to create Dolby effect: ${e.message}")
            throw e
        }
    }

    private fun checkEffect() {
        if (isReleased) {
            DolbyConstants.dlog(TAG, "Repository released, skipping effect check")
            return
        }
        
        try {
            if (!dolbyEffect.hasControl()) {
                DolbyConstants.dlog(TAG, "Lost audio effect control, recreating")
                dolbyEffect.release()
                dolbyEffect = createDolbyEffect()
                restoreSavedProfileIfNeeded()
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error checking effect: ${e.message}")
        }
    }

    private fun readSavedProfile(): Int? {
        return defaultPrefs.getString(DolbyConstants.PREF_PROFILE, null)
            ?.toIntOrNull()
    }

    private fun restoreSavedProfileIfNeeded() {
        val savedProfile = readSavedProfile() ?: return
        if (dolbyEffect.profile != savedProfile) {
            dolbyEffect.profile = savedProfile
        }
        restoreProfilePreset(savedProfile)
        applyProfileSettings(savedProfile)
    }

    private fun applyProfileSettings(profile: Int) {
        try {
            val prefs = getProfilePrefs(profile)
            
            val ieqPreset = prefs.getString(DolbyConstants.PREF_IEQ, "0")?.toIntOrNull() ?: 0
            dolbyEffect.setDapParameter(DsParam.IEQ_PRESET, ieqPreset, profile)
            
            val hpVirtualizer = prefs.getBoolean(DolbyConstants.PREF_HP_VIRTUALIZER, false)
            dolbyEffect.setDapParameter(DsParam.HEADPHONE_VIRTUALIZER, hpVirtualizer, profile)
            
            val spkVirtualizer = prefs.getBoolean(DolbyConstants.PREF_SPK_VIRTUALIZER, false)
            dolbyEffect.setDapParameter(DsParam.SPEAKER_VIRTUALIZER, spkVirtualizer, profile)
            
            if (stereoWideningSupported) {
                val stereoWidening = prefs.getInt(DolbyConstants.PREF_STEREO_WIDENING, 32)
                dolbyEffect.setDapParameter(DsParam.STEREO_WIDENING_AMOUNT, stereoWidening, profile)
            }
            
            val dialogueEnabled = prefs.getBoolean(DolbyConstants.PREF_DIALOGUE, false)
            dolbyEffect.setDapParameter(DsParam.DIALOGUE_ENHANCER_ENABLE, dialogueEnabled, profile)
            
            val dialogueAmount = prefs.getInt(DolbyConstants.PREF_DIALOGUE_AMOUNT, 6)
            dolbyEffect.setDapParameter(DsParam.DIALOGUE_ENHANCER_AMOUNT, dialogueAmount, profile)
            
            val bassEnabled = prefs.getBoolean(DolbyConstants.PREF_BASS, false)
            dolbyEffect.setDapParameter(DsParam.BASS_ENHANCER_ENABLE, bassEnabled, profile)
            
            if (volumeLevelerSupported) {
                val volumeLeveler = prefs.getBoolean(DolbyConstants.PREF_VOLUME, false)
                dolbyEffect.setDapParameter(DsParam.VOLUME_LEVELER_ENABLE, volumeLeveler, profile)
                // Isolated: a HAL without param 116 must not break the restore.
                runCatching {
                    val levelerAmount = prefs.getInt(DolbyConstants.PREF_VOLUME_AMOUNT, LEVELER_AMOUNT_DEFAULT)
                    dolbyEffect.setDapParameter(DsParam.VOLUME_LEVELER_AMOUNT, levelerAmount, profile)
                }.onFailure {
                    DolbyConstants.dlog(TAG, "Leveler amount (116) unsupported: ${it.message}")
                }
            }

            restoreLabParams(profile)
            
            DolbyConstants.dlog(TAG, "Successfully restored all settings for profile $profile")
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to restore profile settings: ${e.message}")
        }
    }

    fun applySavedState() {
    checkEffect()
        val enabled = defaultPrefs.getBoolean(DolbyConstants.PREF_ENABLE, false)
        dolbyEffect.dsOn = enabled
        if (enabled) {
            restoreSavedProfileIfNeeded()
        }
    }

    fun getCurrentOutputDevice(): AudioDeviceInfo? {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (type in OUTPUT_DEVICE_PRIORITY) {
            val device = devices.firstOrNull { it.type == type }
            if (device != null) return device
        }
        return devices.firstOrNull()
    }

    /**
     * True media-route sink for USAGE_MEDIA — the same source
     * DolbyEffectService uses for per-device snapshots. Unlike the raw
     * connected-device list this follows forced routing (the speaker/BT
     * toggles in [selectOutputDevice]) where the platform honors it, so
     * the home card and picker badges reflect the switch instead of
     * always showing the highest-priority connected device.
     */
    private fun getRoutedMediaDevice(): AudioDeviceInfo? {
        return try {
            val routed = audioManager.getDevicesForAttributes(ATTRIBUTES_MEDIA)
                .firstOrNull() ?: return null
            val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val routedAddress = routed.address.orEmpty()
            outputs.firstOrNull { device ->
                device.isSink &&
                    device.type == routed.type &&
                    (routedAddress.isEmpty() || device.address == routedAddress)
            } ?: outputs.firstOrNull { device ->
                device.isSink && device.type == routed.type
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Routed media device query failed: ${e.message}")
            null
        }
    }

    /**
     * Connected user-meaningful sinks for the output picker, priority-ordered
     * and de-duplicated (e.g. a BT headset exposing A2DP + SCO collapses to
     * one row). Active state follows the live USAGE_MEDIA route.
     */
    fun getOutputDevices(): List<OutputDevice> {
        return try {
            val infos = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter { it.type in OUTPUT_DEVICE_PRIORITY }
            val activeKey = resolveRoutedOutputKey(infos)
            val seen = mutableSetOf<String>()
            OUTPUT_DEVICE_PRIORITY.flatMap { type ->
                infos.filter { it.type == type }
            }.mapNotNull { info ->
                val key = deviceStateManager.deviceKey(info)
                if (!seen.add(key)) return@mapNotNull null
                OutputDevice(
                    key = key,
                    name = deviceStateManager.deviceDisplayName(info),
                    category = info.toAudioCategory(),
                    isActive = key == activeKey
                )
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to list outputs: ${e.message}")
            emptyList()
        }
    }

    /**
     * Switch the media output route.
     *
     * The previous implementation only called
     * MediaRouter.selectRoute(ROUTE_TYPE_LIVE_AUDIO, …). That never moved
     * audio: the legacy MediaRouter only publishes non-default routes while
     * the app holds an active route-discovery callback (we hold none, so
     * routeCount is just the default route and selecting it is a silent
     * no-op), and wired/USB sinks have no MediaRouter device type at all,
     * so matching degenerated to typed[0]. Hence taps looked accepted but
     * nothing switched.
     *
     * This drives the global AudioManager force flags instead, which is
     * what actually steers routing between connected sinks:
     * - SPEAKER: drop SCO/A2DP, force speaker.
     * - BLUETOOTH: drop speaker force, force media to A2DP.
     * - WIRED/USB/OTHER: clear both forces so the policy auto-falls to the
     *   connected wired sink (a physically unplugged jack can't be forced).
     * A best-effort MediaRouter hop is kept afterwards for BT
     * disambiguation; it is harmless when undiscovered. Needs
     * MODIFY_AUDIO_SETTINGS (manifest, normal permission).
     */
    fun selectOutputDevice(key: String): Boolean {
        return try {
            val infos = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val target = infos.firstOrNull {
                it.type in OUTPUT_DEVICE_PRIORITY && deviceStateManager.deviceKey(it) == key
            } ?: return false
            when (target.toAudioCategory()) {
                AudioDeviceCategory.SPEAKER -> {
                    runCatching { audioManager.stopBluetoothSco() }
                    runCatching { audioManager.isBluetoothA2dpOn = false }
                    audioManager.isSpeakerphoneOn = true
                }
                AudioDeviceCategory.BLUETOOTH -> {
                    runCatching { audioManager.isSpeakerphoneOn = false }
                    audioManager.isBluetoothA2dpOn = true
                }
                AudioDeviceCategory.WIRED,
                AudioDeviceCategory.USB,
                AudioDeviceCategory.OTHER -> {
                    runCatching { audioManager.stopBluetoothSco() }
                    runCatching { audioManager.isBluetoothA2dpOn = false }
                    runCatching { audioManager.isSpeakerphoneOn = false }
                }
            }
            // Best-effort legacy hop; ignored when routes are undiscovered.
            runCatching { selectLiveRouteBestEffort(target) }
            // Flags are synchronous: re-resolve now so the active-device
            // flow + picker badges reflect the switch immediately instead
            // of waiting for a plug/unplug callback that never comes.
            updateSpeakerState()
            true
        } catch (e: SecurityException) {
            DolbyConstants.dlog(TAG, "Output switch denied: ${e.message}")
            false
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Output switch failed: ${e.message}")
            false
        }
    }

    /** Legacy MediaRouter hop, best-effort: false when routes undiscovered. */
    private fun selectLiveRouteBestEffort(target: AudioDeviceInfo): Boolean {
        return try {
            val router = context.getSystemService(android.media.MediaRouter::class.java)
                ?: return false
            val liveType = android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO
            val liveRoutes = (0 until router.routeCount).map { router.getRouteAt(it) }
                .filter { (it.supportedTypes and liveType) != 0 }
            // A lone default route means discovery is off; selecting it is a
            // no-op, so don't pretend we switched via this path.
            if (liveRoutes.size <= 1) return false
            val route = pickLiveRoute(liveRoutes, target) ?: return false
            router.selectRoute(liveType, route)
            true
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Route hop failed: ${e.message}")
            false
        }
    }

    /** Match an AudioDeviceInfo to a MediaRouter live-audio route. */
    private fun pickLiveRoute(
        liveRoutes: List<android.media.MediaRouter.RouteInfo>,
        target: AudioDeviceInfo
    ): android.media.MediaRouter.RouteInfo? {
        if (liveRoutes.isEmpty()) return null
        val wanted = routeDeviceTypes(target.toAudioCategory())
        val typed = if (wanted.isEmpty()) liveRoutes
            else liveRoutes.filter { it.deviceType in wanted }
        if (typed.isEmpty()) return null
        if (typed.size == 1) return typed[0]
        // Several devices of the same kind (e.g. two BT headsets): match by name.
        val targetName = deviceStateManager.deviceDisplayName(target).lowercase()
        return typed.firstOrNull { route ->
            val routeName = route.name?.toString()?.lowercase().orEmpty()
            routeName.isNotBlank() && targetName.isNotBlank() &&
                (routeName.contains(targetName) || targetName.contains(routeName))
        } ?: typed[0]
    }

    /**
     * Active output key. The live media route (USAGE_MEDIA) is read first
     * because it reflects reality — including forced routing from
     * [selectOutputDevice] — better than the raw connected-device list.
     * The AudioManager force flags stay as a fallback for builds where
     * the routed query is empty, with MediaRouter matching last.
     */
    private fun resolveRoutedOutputKey(
        infos: List<AudioDeviceInfo>
    ): String? {
        // Live media route first: follows forced routing where honored.
        runCatching {
            val routed = getRoutedMediaDevice()
            if (routed != null) {
                val routedKey = deviceStateManager.deviceKey(routed)
                if (infos.any { deviceStateManager.deviceKey(it) == routedKey }) {
                    return routedKey
                }
            }
        }
        // Explicit speaker force always wins for the badge.
        if (runCatching { audioManager.isSpeakerphoneOn }.getOrDefault(false)) {
            infos.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                ?.let { return deviceStateManager.deviceKey(it) }
        }
        // Connected wired sink captures media once forces are cleared.
        infos.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }?.let { return deviceStateManager.deviceKey(it) }
        // Connected BT sink, unless media was forced off BT.
        if (runCatching { audioManager.isBluetoothA2dpOn }.getOrDefault(true)) {
            infos.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                    it.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
            }?.let { return deviceStateManager.deviceKey(it) }
        }
        infos.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            ?.let { return deviceStateManager.deviceKey(it) }
        return try {
            val router = context.getSystemService(android.media.MediaRouter::class.java)
                ?: return currentDeviceKey()
            val selected =
                router.getSelectedRoute(android.media.MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
                    ?: return currentDeviceKey()
            val match = infos.firstOrNull { info ->
                info.toAudioCategory().let { cat ->
                    val wanted = routeDeviceTypes(cat)
                    (wanted.isEmpty() || selected.deviceType in wanted) &&
                        (selected.name?.toString()?.let { routeName ->
                            val devName = deviceStateManager.deviceDisplayName(info)
                            routeName.contains(devName, ignoreCase = true) ||
                                devName.contains(routeName, ignoreCase = true)
                        } ?: false)
                }
            } ?: infos.firstOrNull {
                // Single connected sink of the routed kind: name match is overkill.
                val wanted = routeDeviceTypes(it.toAudioCategory())
                wanted.isNotEmpty() && selected.deviceType in wanted &&
                    infos.count { other ->
                        routeDeviceTypes(other.toAudioCategory())
                            .any { t -> t in wanted }
                    } == 1
            }
            match?.let { deviceStateManager.deviceKey(it) } ?: currentDeviceKey()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Route resolve failed: ${e.message}")
            currentDeviceKey()
        }
    }

    private fun routeDeviceTypes(category: AudioDeviceCategory): Set<Int> {
        return when (category) {
            AudioDeviceCategory.SPEAKER ->
                setOf(android.media.MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER)
            AudioDeviceCategory.BLUETOOTH ->
                setOf(android.media.MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH)
            // android.media.MediaRouter.RouteInfo only defines UNKNOWN, TV,
            // SPEAKER and BLUETOOTH device types; wired/USB routes are
            // matched by name instead (wanted.isEmpty() path).
            AudioDeviceCategory.WIRED,
            AudioDeviceCategory.USB,
            AudioDeviceCategory.OTHER -> emptySet()
        }
    }

    /** Stable key for the current output device, matching DeviceStateManager keys. */
    fun currentDeviceKey(): String? {
        return try {
            getCurrentOutputDevice()?.let { deviceStateManager.deviceKey(it) }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting device key: ${e.message}")
            null
        }
    }

    private fun resolveActiveAudioDevice(): ActiveAudioDevice {
        val device = getRoutedMediaDevice() ?: getCurrentOutputDevice()
            ?: return ActiveAudioDevice.Unknown
        return ActiveAudioDevice(
            name = deviceStateManager.deviceDisplayName(device),
            category = device.toAudioCategory()
        )
    }

    private fun AudioDeviceInfo.toAudioCategory(): AudioDeviceCategory {
        return when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceCategory.SPEAKER
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> AudioDeviceCategory.WIRED
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST -> AudioDeviceCategory.BLUETOOTH
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE -> AudioDeviceCategory.USB
            else -> AudioDeviceCategory.OTHER
        }
    }

    fun updateSpeakerState() {
        if (!isReleased) {
            val activeDevice = resolveActiveAudioDevice()
            _activeAudioDevice.value = activeDevice
            _isOnSpeaker.value = activeDevice.isOnSpeaker
        }
    }

    fun getDolbyEnabled(): Boolean {
        return try {
            dolbyEffect.dsOn
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting Dolby enabled state: ${e.message}")
            false
        }
    }

    fun setDolbyEnabled(enabled: Boolean) {
        if (isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.dsOn = enabled
            defaultPrefs.edit().putBoolean(DolbyConstants.PREF_ENABLE, enabled).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting Dolby enabled: ${e.message}")
        }
    }

    fun getCurrentProfile(): Int {
        return try {
            checkEffect()
            restoreSavedProfileIfNeeded()
            dolbyEffect.profile
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting current profile: ${e.message}")
            0
        }
    }

    fun setCurrentProfile(profile: Int) {
        if (isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.profile = profile
            defaultPrefs.edit().putString(DolbyConstants.PREF_PROFILE, profile.toString()).apply()
            if (!verifyProfileSaved(profile)) {
                DolbyConstants.dlog(TAG, "WARNING: Profile may not have been saved correctly!")
            }
            restoreProfilePreset(profile)
            _currentProfile.value = profile
            DolbyConstants.dlog(TAG, "Profile set to: $profile")
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting current profile: ${e.message}")
        }
    }

    private fun restoreProfilePreset(profile: Int) {
        try {
            val prefs = getProfilePrefs(profile)
            val savedPresetGains = prefs.getString(DolbyConstants.PREF_PRESET, null)
            
            if (savedPresetGains != null) {
                val gains = savedPresetGains.split(",").mapNotNull { it.toIntOrNull() }.toIntArray()
                if (gains.size == 20) {
                    dolbyEffect.setDapParameter(DsParam.GEQ_BAND_GAINS, gains, profile)
                    DolbyConstants.dlog(TAG, "Restored preset for profile $profile")
                }
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Failed to restore preset for profile $profile: ${e.message}")
        }
    }

    fun verifyProfileSaved(profile: Int): Boolean {
        val prefs = defaultPrefs.getString(DolbyConstants.PREF_PROFILE, "0")?.toIntOrNull()
        val saved = prefs == profile
        DolbyConstants.dlog(TAG, "Profile verification: requested=$profile, saved=$prefs, match=$saved")
        return saved
    }

    private fun getProfilePrefs(profile: Int): SharedPreferences {
        return context.getSharedPreferences("profile_$profile", Context.MODE_PRIVATE)
    }

    fun getBandMode(): BandMode {
        val mode = defaultPrefs.getString(DolbyConstants.PREF_BAND_MODE, "10")
        return when (mode) {
            "10" -> BandMode.TEN_BAND
            "15" -> BandMode.FIFTEEN_BAND
            "20" -> BandMode.TWENTY_BAND
            else -> BandMode.TEN_BAND
        }
    }

    fun setBandMode(mode: BandMode) {
        defaultPrefs.edit().putString(DolbyConstants.PREF_BAND_MODE, mode.value).apply()
    }

    fun getBassEnhancerEnabled(profile: Int): Boolean {
        return try {
            dolbyEffect.getDapParameterBool(DsParam.BASS_ENHANCER_ENABLE, profile)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting bass enhancer: ${e.message}")
            false
        }
    }

    fun setBassEnhancerEnabled(profile: Int, enabled: Boolean) {
        if (isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.BASS_ENHANCER_ENABLE, enabled, profile)
            getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_BASS, enabled).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting bass enhancer: ${e.message}")
        }
    }

    fun getBassLevel(profile: Int): Int {
        val prefs = getProfilePrefs(profile)
        return prefs.getInt(DolbyConstants.PREF_BASS_LEVEL, 0)
    }

    fun getBassCurve(profile: Int): Int {
        val prefs = getProfilePrefs(profile)
        return prefs.getInt(DolbyConstants.PREF_BASS_CURVE, 0)
    }

    fun setBassCurve(profile: Int, curve: Int) {
        if (isReleased) return
        
        try {
            val prefs = getProfilePrefs(profile)
            val previousCurve = prefs.getInt(DolbyConstants.PREF_BASS_CURVE, 0)
            val level = prefs.getInt(DolbyConstants.PREF_BASS_LEVEL, 0)
            if (previousCurve == curve) return

            prefs.edit().putInt(DolbyConstants.PREF_BASS_CURVE, curve).apply()

            if (level <= 0) return
            checkEffect()
            val currentGains = dolbyEffect.getDapParameter(DsParam.GEQ_BAND_GAINS, profile)
            val modifiedGains = currentGains.copyOf()
            applyBassCurve(modifiedGains, level, previousCurve, -1)
            applyBassCurve(modifiedGains, level, curve, 1)
            dolbyEffect.setDapParameter(DsParam.GEQ_BAND_GAINS, modifiedGains, profile)
            
            val gainsString = modifiedGains.joinToString(",")
            prefs.edit().putString(DolbyConstants.PREF_PRESET, gainsString).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting bass curve: ${e.message}")
            throw e
        }
    }

    private fun applyBassCurve(gains: IntArray, level: Int, curve: Int, direction: Int) {
        val weights = BASS_CURVES.getOrElse(curve) { BASS_CURVES[0] }
        val baseGain = level * BASS_GAIN_MULTIPLIER
        for (i in weights.indices) {
            if (i >= gains.size) break
            val weightedGain = (baseGain * weights[i] * direction).toInt()
            gains[i] = (gains[i] + weightedGain).coerceIn(-150, 150)
        }
    }

    fun setBassLevel(profile: Int, level: Int) {
        if (isReleased) return
        
        DolbyConstants.dlog(TAG, "setBassLevel: profile=$profile level=$level")

        if (level !in 0..100) {
            DolbyConstants.dlog(TAG, "setBassLevel: invalid level $level")
            throw IllegalArgumentException("Bass level must be between 0 and 100")
        }
        
        try {
            val prefs = getProfilePrefs(profile)
            val previousLevel = prefs.getInt(DolbyConstants.PREF_BASS_LEVEL, 0)
            
            prefs.edit().putInt(DolbyConstants.PREF_BASS_LEVEL, level).apply()
            
            setBassEnhancerEnabled(profile, level > 0)
            
            checkEffect()
            val currentGains = dolbyEffect.getDapParameter(DsParam.GEQ_BAND_GAINS, profile)
            val modifiedGains = currentGains.copyOf()
            
            val curve = prefs.getInt(DolbyConstants.PREF_BASS_CURVE, 0)
            if (previousLevel > 0) {
                applyBassCurve(modifiedGains, previousLevel, curve, -1)
            }

            if (level > 0) {
                applyBassCurve(modifiedGains, level, curve, 1)
            }
            dolbyEffect.setDapParameter(DsParam.GEQ_BAND_GAINS, modifiedGains, profile)
            
            val gainsString = modifiedGains.joinToString(",")
            prefs.edit().putString(DolbyConstants.PREF_PRESET, gainsString).apply()
            
            DolbyConstants.dlog(TAG, "setBassLevel: success")
        } catch (e: IllegalArgumentException) {
            DolbyConstants.dlog(TAG, "setBassLevel: validation error - ${e.message}")
            val prefs = getProfilePrefs(profile)
            prefs.edit().putInt(DolbyConstants.PREF_BASS_LEVEL, 0).apply()
            throw e
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "setBassLevel: unexpected error - ${e.message}")
            throw e
        }
    }

    fun getSubBassLevel(profile: Int): Int {
        return getProfilePrefs(profile).getInt(DolbyConstants.PREF_SUB_BASS_LEVEL, 0)
    }

    fun getMidBassLevel(profile: Int): Int {
        return getProfilePrefs(profile).getInt(DolbyConstants.PREF_MID_BASS_LEVEL, 0)
    }

    fun getUpperBassLevel(profile: Int): Int {
        return getProfilePrefs(profile).getInt(DolbyConstants.PREF_UPPER_BASS_LEVEL, 0)
    }

    /**
     * Sub/mid/upper bass trims stack additively on top of the master
     * bass level + curve. Each trim applies a flat gain over its own
     * slice of the 20-band GEQ (20-band indices):
     * sub 0..1 (32-47 Hz), mid-bass 2..4 (141-328 Hz),
     * upper-bass 5..7 (469-844 Hz).
     * Upper-bass overlaps the mid enhancer (bands 5..13) by design —
     * deltas compose, same as the existing bass/mid overlap.
     */
    private fun setBassTrim(
        profile: Int,
        prefKey: String,
        label: String,
        level: Int,
        bands: IntRange
    ) {
        if (isReleased) return

        DolbyConstants.dlog(TAG, "set$label: profile=$profile level=$level")

        if (level !in 0..100) {
            DolbyConstants.dlog(TAG, "set$label: invalid level $level")
            throw IllegalArgumentException("$label level must be between 0 and 100")
        }

        try {
            val prefs = getProfilePrefs(profile)
            val previousLevel = prefs.getInt(prefKey, 0)

            prefs.edit().putInt(prefKey, level).apply()

            checkEffect()
            val currentGains = dolbyEffect.getDapParameter(DsParam.GEQ_BAND_GAINS, profile)
            val modifiedGains = currentGains.copyOf()

            if (previousLevel > 0) {
                val previousGain = (previousLevel * BASS_GAIN_MULTIPLIER).toInt()
                for (i in bands) {
                    if (i < modifiedGains.size) {
                        modifiedGains[i] = (modifiedGains[i] - previousGain).coerceIn(-150, 150)
                    }
                }
            }

            if (level > 0) {
                val trimGain = (level * BASS_GAIN_MULTIPLIER).toInt()
                for (i in bands) {
                    if (i < modifiedGains.size) {
                        modifiedGains[i] = (modifiedGains[i] + trimGain).coerceIn(-150, 150)
                    }
                }
            }

            dolbyEffect.setDapParameter(DsParam.GEQ_BAND_GAINS, modifiedGains, profile)

            val gainsString = modifiedGains.joinToString(",")
            prefs.edit().putString(DolbyConstants.PREF_PRESET, gainsString).apply()

            DolbyConstants.dlog(TAG, "set$label: success")
        } catch (e: IllegalArgumentException) {
            DolbyConstants.dlog(TAG, "set$label: validation error - ${e.message}")
            getProfilePrefs(profile).edit().putInt(prefKey, 0).apply()
            throw e
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "set$label: unexpected error - ${e.message}")
            throw e
        }
    }

    fun setSubBassLevel(profile: Int, level: Int) =
        setBassTrim(profile, DolbyConstants.PREF_SUB_BASS_LEVEL, "SubBassLevel", level, 0..1)

    fun setMidBassLevel(profile: Int, level: Int) =
        setBassTrim(profile, DolbyConstants.PREF_MID_BASS_LEVEL, "MidBassLevel", level, 2..4)

    fun setUpperBassLevel(profile: Int, level: Int) =
        setBassTrim(profile, DolbyConstants.PREF_UPPER_BASS_LEVEL, "UpperBassLevel", level, 5..7)

    fun getTrebleEnhancerEnabled(profile: Int): Boolean {
        val prefs = getProfilePrefs(profile)
        return prefs.getBoolean(DolbyConstants.PREF_TREBLE, false)
    }

    fun setTrebleEnhancerEnabled(profile: Int, enabled: Boolean) {
        getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_TREBLE, enabled).apply()
    }

    fun getTrebleLevel(profile: Int): Int {
        val prefs = getProfilePrefs(profile)
        return prefs.getInt(DolbyConstants.PREF_TREBLE_LEVEL, 0)
    }

    fun setTrebleLevel(profile: Int, level: Int) {
        if (isReleased) return
        
        DolbyConstants.dlog(TAG, "setTrebleLevel: profile=$profile level=$level")

        if (level !in 0..100) {
            DolbyConstants.dlog(TAG, "setTrebleLevel: invalid level $level")
            throw IllegalArgumentException("Treble level must be between 0 and 100")
        }

        try {
            val prefs = getProfilePrefs(profile)
            val previousLevel = prefs.getInt(DolbyConstants.PREF_TREBLE_LEVEL, 0)

            prefs.edit().putInt(DolbyConstants.PREF_TREBLE_LEVEL, level).apply()
            setTrebleEnhancerEnabled(profile, level > 0)

            checkEffect()
            val currentGains = dolbyEffect.getDapParameter(DsParam.GEQ_BAND_GAINS, profile)
            val modifiedGains = currentGains.copyOf()

            if (previousLevel > 0) {
                val previousGain = (previousLevel * TREBLE_GAIN_MULTIPLIER).toInt()
                for (i in 14..19) {
                    if (i < modifiedGains.size) {
                        modifiedGains[i] = (modifiedGains[i] - previousGain).coerceIn(-150, 150)
                    }
                }
            }

            if (level > 0) {
                val trebleGain = (level * TREBLE_GAIN_MULTIPLIER).toInt()
                for (i in 14..19) {
                    if (i < modifiedGains.size) {
                        modifiedGains[i] = (modifiedGains[i] + trebleGain).coerceIn(-150, 150)
                    }
                }
            }

            dolbyEffect.setDapParameter(DsParam.GEQ_BAND_GAINS, modifiedGains, profile)
            
            val gainsString = modifiedGains.joinToString(",")
            prefs.edit().putString(DolbyConstants.PREF_PRESET, gainsString).apply()
            
            DolbyConstants.dlog(TAG, "setTrebleLevel: success")
        } catch (e: IllegalArgumentException) {
            DolbyConstants.dlog(TAG, "setTrebleLevel: validation error - ${e.message}")
            val prefs = getProfilePrefs(profile)
            prefs.edit().putInt(DolbyConstants.PREF_TREBLE_LEVEL, 0).apply()
            throw e
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "setTrebleLevel: unexpected error - ${e.message}")
            throw e
        }
    }

    fun getVolumeLevelerEnabled(profile: Int): Boolean {
        if (!volumeLevelerSupported) return false
        return try {
            dolbyEffect.getDapParameterBool(DsParam.VOLUME_LEVELER_ENABLE, profile)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting volume leveler: ${e.message}")
            false
        }
    }

    fun setVolumeLevelerEnabled(profile: Int, enabled: Boolean) {
        if (!volumeLevelerSupported || isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.VOLUME_LEVELER_ENABLE, enabled, profile)
            getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_VOLUME, enabled).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting volume leveler: ${e.message}")
        }
    }

    fun getVolumeLevelerAmount(profile: Int): Int {
        if (!volumeLevelerSupported) return LEVELER_AMOUNT_DEFAULT
        val prefs = getProfilePrefs(profile)
        if (prefs.contains(DolbyConstants.PREF_VOLUME_AMOUNT)) {
            return prefs.getInt(DolbyConstants.PREF_VOLUME_AMOUNT, LEVELER_AMOUNT_DEFAULT)
        }
        return try {
            val amount = dolbyEffect.getDapParameterInt(DsParam.VOLUME_LEVELER_AMOUNT, profile)
            prefs.edit().putInt(DolbyConstants.PREF_VOLUME_AMOUNT, amount).apply()
            amount
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting volume leveler amount: ${e.message}")
            // Cache the default so a HAL without 116 doesn't spam every load.
            prefs.edit().putInt(DolbyConstants.PREF_VOLUME_AMOUNT, LEVELER_AMOUNT_DEFAULT).apply()
            LEVELER_AMOUNT_DEFAULT
        }
    }

    fun setVolumeLevelerAmount(profile: Int, amount: Int) {
        if (!volumeLevelerSupported || isReleased) return

        try {
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.VOLUME_LEVELER_AMOUNT, amount, profile)
            getProfilePrefs(profile).edit().putInt(DolbyConstants.PREF_VOLUME_AMOUNT, amount).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting volume leveler amount: ${e.message}")
        }
    }

    fun getIeqPreset(profile: Int): Int {
        return try {
            dolbyEffect.getDapParameterInt(DsParam.IEQ_PRESET, profile)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting IEQ preset: ${e.message}")
            0
        }
    }

    fun setIeqPreset(profile: Int, preset: Int) {
        if (isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.IEQ_PRESET, preset, profile)
            getProfilePrefs(profile).edit().putString(DolbyConstants.PREF_IEQ, preset.toString()).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting IEQ preset: ${e.message}")
        }
    }

    fun getHeadphoneVirtualizerEnabled(profile: Int): Boolean {
        return try {
            dolbyEffect.getDapParameterBool(DsParam.HEADPHONE_VIRTUALIZER, profile)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting headphone virtualizer: ${e.message}")
            false
        }
    }

    fun setHeadphoneVirtualizerEnabled(profile: Int, enabled: Boolean) {
        if (isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.HEADPHONE_VIRTUALIZER, enabled, profile)
            getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_HP_VIRTUALIZER, enabled).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting headphone virtualizer: ${e.message}")
        }
    }

    fun getSpeakerVirtualizerEnabled(profile: Int): Boolean {
        return try {
            dolbyEffect.getDapParameterBool(DsParam.SPEAKER_VIRTUALIZER, profile)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting speaker virtualizer: ${e.message}")
            false
        }
    }

    fun setSpeakerVirtualizerEnabled(profile: Int, enabled: Boolean) {
        if (isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.SPEAKER_VIRTUALIZER, enabled, profile)
            getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_SPK_VIRTUALIZER, enabled).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting speaker virtualizer: ${e.message}")
        }
    }

    fun getStereoWideningAmount(profile: Int): Int {
        if (!stereoWideningSupported) return 0
        val prefs = getProfilePrefs(profile)
        if (prefs.contains(DolbyConstants.PREF_STEREO_WIDENING)) {
            return prefs.getInt(DolbyConstants.PREF_STEREO_WIDENING, 32)
        }
        return try {
            val amount = dolbyEffect.getDapParameterInt(DsParam.STEREO_WIDENING_AMOUNT, profile)
            prefs.edit().putInt(DolbyConstants.PREF_STEREO_WIDENING, amount).apply()
            amount
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting stereo widening: ${e.message}")
            32
        }
    }

    fun setStereoWideningAmount(profile: Int, amount: Int) {
        if (!stereoWideningSupported || isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.STEREO_WIDENING_AMOUNT, amount, profile)
            getProfilePrefs(profile).edit().putInt(DolbyConstants.PREF_STEREO_WIDENING, amount).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting stereo widening: ${e.message}")
        }
    }

    fun getDialogueEnhancerEnabled(profile: Int): Boolean {
        return try {
            dolbyEffect.getDapParameterBool(DsParam.DIALOGUE_ENHANCER_ENABLE, profile)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting dialogue enhancer: ${e.message}")
            false
        }
    }

    fun setDialogueEnhancerEnabled(profile: Int, enabled: Boolean) {
        if (isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.DIALOGUE_ENHANCER_ENABLE, enabled, profile)
            getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_DIALOGUE, enabled).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting dialogue enhancer: ${e.message}")
        }
    }

    fun getDialogueEnhancerAmount(profile: Int): Int {
        return try {
            dolbyEffect.getDapParameterInt(DsParam.DIALOGUE_ENHANCER_AMOUNT, profile)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting dialogue enhancer amount: ${e.message}")
            6
        }
    }

    fun setDialogueEnhancerAmount(profile: Int, amount: Int) {
        if (isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.setDapParameter(DsParam.DIALOGUE_ENHANCER_AMOUNT, amount, profile)
            getProfilePrefs(profile).edit().putInt(DolbyConstants.PREF_DIALOGUE_AMOUNT, amount).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting dialogue enhancer amount: ${e.message}")
        }
    }

    /**
     * Tuning Lab: generic read/write for gap DAP IDs with no public
     * semantics. Support is probed live against the HAL — IDs the HAL
     * rejects are omitted from the result and hidden in the UI.
     * Must be called off the main thread (touches the audio effect).
     */
    private val labSupportCache = mutableMapOf<Int, Boolean>()
    private val labSupportLock = Any()
    /** Extra IDs found by the sweep fallback (outside the curated list). */
    private val labDiscoveredIds = mutableSetOf<Int>()

    private fun isLabSupported(paramId: Int, profile: Int): Boolean {
        synchronized(labSupportLock) {
            labSupportCache[paramId]?.let { return it }
        }
        val supported = runCatching {
            checkEffect()
            dolbyEffect.getRawDapParameter(paramId, profile)
        }.isSuccess
        synchronized(labSupportLock) {
            labSupportCache[paramId] = supported
        }
        if (!supported) {
            DolbyConstants.dlog(TAG, "Lab param $paramId unsupported on this HAL")
        }
        return supported
    }

    fun getLabParams(profile: Int): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        for (paramId in DolbyConstants.LAB_DAP_PARAM_IDS) {
            readLabParam(paramId, profile)?.let { result[paramId] = it }
        }
        if (result.isEmpty()) {
            // Curated gap list answered nothing — this HAL revision keeps
            // its extras elsewhere. Sweep 100–130 (minus known IDs) and
            // surface whatever answers as Tuning Lab sliders.
            for (paramId in DolbyConstants.LAB_SWEEP_RANGE) {
                if (paramId in DolbyConstants.LAB_DAP_PARAM_IDS) continue
                if (paramId in DolbyConstants.LAB_SWEEP_EXCLUDE) continue
                readLabParam(paramId, profile)?.let {
                    result[paramId] = it
                    synchronized(labSupportLock) {
                        labDiscoveredIds.add(paramId)
                    }
                }
            }
            if (result.isNotEmpty()) {
                DolbyConstants.dlog(
                    TAG,
                    "Lab sweep found params: ${result.keys.sorted()}"
                )
            }
        }
        return result
    }

    /** Reads one raw param; null when the HAL rejects the ID. */
    private fun readLabParam(paramId: Int, profile: Int): Int? {
        if (!isLabSupported(paramId, profile)) return null
        return try {
            checkEffect()
            dolbyEffect.getRawDapParameter(paramId, profile)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error reading lab param $paramId: ${e.message}")
            synchronized(labSupportLock) {
                labSupportCache[paramId] = false
            }
            null
        }
    }

    /** IDs currently writable: curated list plus sweep discoveries. */
    private fun isLabWritable(paramId: Int): Boolean {
        if (paramId in DolbyConstants.LAB_DAP_PARAM_IDS) return true
        synchronized(labSupportLock) {
            return paramId in labDiscoveredIds
        }
    }

    /** Curated + discovered IDs, for pref restore/reset loops. */
    private fun allLabIds(): List<Int> {
        synchronized(labSupportLock) {
            return DolbyConstants.LAB_DAP_PARAM_IDS + labDiscoveredIds.sorted()
        }
    }

    fun setLabParam(profile: Int, paramId: Int, value: Int) {
        if (isReleased) return
        if (!isLabWritable(paramId)) {
            throw IllegalArgumentException("Unknown lab param $paramId")
        }
        if (value !in DolbyConstants.LAB_PARAM_MIN..DolbyConstants.LAB_PARAM_MAX) {
            throw IllegalArgumentException(
                "Lab param must be between ${DolbyConstants.LAB_PARAM_MIN} " +
                    "and ${DolbyConstants.LAB_PARAM_MAX}"
            )
        }
        try {
            checkEffect()
            dolbyEffect.setRawDapParameter(paramId, value, profile)
            getProfilePrefs(profile).edit()
                .putInt(DolbyConstants.labParamPref(paramId), value).apply()
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting lab param $paramId: ${e.message}")
            synchronized(labSupportLock) {
                labSupportCache[paramId] = false
            }
            throw e
        }
    }

    fun resetLabParams(profile: Int) {
        if (isReleased) return
        try {
            val prefs = getProfilePrefs(profile)
            prefs.edit().apply {
                allLabIds().forEach {
                    remove(DolbyConstants.labParamPref(it))
                }
                apply()
            }
            synchronized(labSupportLock) {
                labSupportCache.clear()
                labDiscoveredIds.clear()
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error resetting lab params: ${e.message}")
        }
    }

    private fun restoreLabParams(profile: Int) {
        val prefs = getProfilePrefs(profile)
        for (paramId in allLabIds()) {
            if (!prefs.contains(DolbyConstants.labParamPref(paramId))) continue
            val value = prefs.getInt(DolbyConstants.labParamPref(paramId), 0)
            // Isolated: one rejected ID must not break the whole restore.
            runCatching {
                checkEffect()
                dolbyEffect.setRawDapParameter(paramId, value, profile)
            }.onFailure {
                DolbyConstants.dlog(TAG, "Lab param $paramId restore failed: ${it.message}")
            }
        }
    }

    fun getEqualizerGains(profile: Int, bandMode: BandMode): List<BandGain> {
        return try {
            val gains = dolbyEffect.getDapParameter(DsParam.GEQ_BAND_GAINS, profile)
            deserializeGains(gains, bandMode)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting equalizer gains: ${e.message}")
            val frequencies = when (bandMode) {
                BandMode.TEN_BAND -> BAND_FREQUENCIES_10
                BandMode.FIFTEEN_BAND -> BAND_FREQUENCIES_15
                BandMode.TWENTY_BAND -> BAND_FREQUENCIES_20
            }
            frequencies.map { BandGain(frequency = it, gain = 0) }
        }
    }

    fun setEqualizerGains(profile: Int, bandGains: List<BandGain>, bandMode: BandMode) {
        if (isReleased) return
        
        try {
            checkEffect()
            val gains = serializeGains(bandGains, bandMode)
            dolbyEffect.setDapParameter(DsParam.GEQ_BAND_GAINS, gains, profile)
            val gainsString = gains.joinToString(",")
            getProfilePrefs(profile).edit().putString(DolbyConstants.PREF_PRESET, gainsString).apply()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting equalizer gains: ${e.message}")
        }
    }

    fun getPresetName(profile: Int): String {
        return try {
            val gains = dolbyEffect.getDapParameter(DsParam.GEQ_BAND_GAINS, profile)
            
            val tenBandGains = gains.filterIndexed { index, _ -> index % 2 == 0 }
            val currentGainsString = tenBandGains.joinToString(",")
            
            val presetValues = context.resources.getStringArray(R.array.dolby_preset_values)
            val presetNames = context.resources.getStringArray(R.array.dolby_preset_entries)
            
            presetValues.forEachIndexed { index, preset ->
                val presetTenBand = convertTo10Band(preset)
                if (gainsMatch(presetTenBand, currentGainsString)) {
                    return presetNames[index]
                }
            }
            
            presetsPrefs.all.forEach { (name, value) ->
                val presetTenBand = convertTo10Band(value.toString())
                if (gainsMatch(presetTenBand, currentGainsString)) {
                    return name
                }
            }
            
            context.getString(R.string.dolby_preset_custom)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting preset name: ${e.message}")
            context.getString(R.string.dolby_preset_custom)
        }
    }

    private fun convertTo10Band(gainsString: String): String {
        val gains = gainsString.split(",").map { it.trim().toIntOrNull() ?: 0 }
        
        if (gains.size == 10) {
            return gains.joinToString(",")
        }
        
        if (gains.size == 20) {
            val tenBand = gains.filterIndexed { index, _ -> index % 2 == 0 }
            return tenBand.joinToString(",")
        }
        
        return gainsString
    }

    private fun gainsMatch(gains1: String, gains2: String): Boolean {
        val g1 = gains1.split(",").map { it.trim().toIntOrNull() ?: 0 }
        val g2 = gains2.split(",").map { it.trim().toIntOrNull() ?: 0 }
        
        if (g1.size != g2.size) return false
        
        return g1.zip(g2).all { (a, b) -> kotlin.math.abs(a - b) <= 1 }
    }

    fun getUserPresets(): List<EqualizerPreset> {
        synchronized(presetCacheLock) {
            cachedPresets?.let { return it }
            
            val bandMode = getBandMode()
            val presets = presetsPrefs.all.mapNotNull { (name, value) ->
                try {
                    val valueStr = value as? String ?: return@mapNotNull null
                    parsePreset(name, valueStr)
                } catch (e: Exception) {
                    DolbyConstants.dlog(TAG, "Error parsing preset $name: ${e.message}")
                    null
                }
            }
            
            cachedPresets = presets
            return presets
        }
    }

    private fun parsePreset(name: String, valueStr: String): EqualizerPreset? {
        return try {
            if (valueStr.contains("|")) {
                val parts = valueStr.split("|")
                val presetBandMode = BandMode.fromValue(parts[1])
                val gains = parts[0].split(",").map { it.toInt() }.toIntArray()
                EqualizerPreset(
                    name = name,
                    bandGains = deserializeGains(gains, presetBandMode),
                    isUserDefined = true,
                    bandMode = presetBandMode
                )
            } else {
                val gains = valueStr.split(",").map { it.toInt() }.toIntArray()
                EqualizerPreset(
                    name = name,
                    bandGains = deserializeGains(gains, BandMode.TEN_BAND),
                    isUserDefined = true,
                    bandMode = BandMode.TEN_BAND
                )
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error parsing preset value: ${e.message}")
            null
        }
    }

    fun addUserPreset(name: String, bandGains: List<BandGain>, bandMode: BandMode) {
        try {
            val gains = serializeGains(bandGains, bandMode).joinToString(",")
            val value = "$gains|${bandMode.value}"
            presetsPrefs.edit().putString(name, value).apply()
            
            synchronized(presetCacheLock) {
                cachedPresets = null
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error adding user preset: ${e.message}")
        }
    }

    fun deleteUserPreset(name: String) {
        try {
            presetsPrefs.edit().remove(name).apply()
            
            synchronized(presetCacheLock) {
                cachedPresets = null
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error deleting user preset: ${e.message}")
        }
    }

    /**
     * Read-only probe of an arbitrary DAP parameter ID. Success carries the
     * HAL-reported value (which still needs a sanity listen — some HALs
     * return 0 for unknown IDs instead of failing). Must be called off the
     * main thread.
     */
    fun probeDapParam(paramId: Int, profile: Int): Result<Int> = runCatching {
        checkEffect()
        dolbyEffect.getRawDapParameter(paramId, profile)
    }

    fun resetProfile(profile: Int) {
        if (isReleased) return
        
        try {
            checkEffect()
            dolbyEffect.resetProfileSpecificSettings(profile)
            context.deleteSharedPreferences("profile_$profile")
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error resetting profile: ${e.message}")
        }
    }

    fun resetAllProfiles() {
        if (isReleased) return
        
        try {
            checkEffect()
            val ids = getProfileIds()
            backupProfiles(ids)
            ids.forEach { resetProfile(it) }
            setCurrentProfile(0)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error resetting all profiles: ${e.message}")
        }
    }

    /** Profile ids from resources, falling back to 0..6. */
    fun getProfileIds(): List<Int> {
        return try {
            context.resources.getStringArray(R.array.dolby_profile_values)
                .map { it.toInt() }
        } catch (e: Exception) {
            (0..6).toList()
        }
    }

    /**
     * True when a profile holds any non-default setting (enhancer levels,
     * toggles, IEQ, leveler amount, lab prefs). GEQ gains are covered
     * indirectly — every enhancer writes levels alongside its deltas.
     * Prefs-only: safe to call anywhere.
     */
    fun isProfileCustomized(profile: Int): Boolean {
        return try {
            val prefs = getProfilePrefs(profile)
            if (prefs.all.isEmpty()) return false
            prefs.getString(DolbyConstants.PREF_IEQ, "0") != "0" ||
                prefs.getBoolean(DolbyConstants.PREF_HP_VIRTUALIZER, false) ||
                prefs.getBoolean(DolbyConstants.PREF_SPK_VIRTUALIZER, false) ||
                prefs.getBoolean(DolbyConstants.PREF_DIALOGUE, false) ||
                prefs.getBoolean(DolbyConstants.PREF_BASS, false) ||
                prefs.getBoolean(DolbyConstants.PREF_MID, false) ||
                prefs.getBoolean(DolbyConstants.PREF_TREBLE, false) ||
                prefs.getBoolean(DolbyConstants.PREF_VOLUME, false) ||
                prefs.getInt(DolbyConstants.PREF_BASS_LEVEL, 0) != 0 ||
                prefs.getInt(DolbyConstants.PREF_BASS_CURVE, 0) != 0 ||
                prefs.getInt(DolbyConstants.PREF_SUB_BASS_LEVEL, 0) != 0 ||
                prefs.getInt(DolbyConstants.PREF_MID_BASS_LEVEL, 0) != 0 ||
                prefs.getInt(DolbyConstants.PREF_UPPER_BASS_LEVEL, 0) != 0 ||
                prefs.getInt(DolbyConstants.PREF_MID_LEVEL, 0) != 0 ||
                prefs.getInt(DolbyConstants.PREF_TREBLE_LEVEL, 0) != 0 ||
                prefs.getInt(DolbyConstants.PREF_VOLUME_AMOUNT, LEVELER_AMOUNT_DEFAULT) !=
                    LEVELER_AMOUNT_DEFAULT ||
                prefs.all.keys.any { it.startsWith("dolby_lab_") }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error checking profile dirt: ${e.message}")
            false
        }
    }

    /**
     * Undo support for profile resets. Snapshots raw prefs maps (HAL GEQ
     * gains are re-derived: PREF_PRESET plus the enhancer levels restore
     * deterministically through [restoreProfilePreset]/[applyProfileSettings]).
     */
    private var profilesBackup: Map<Int, Map<String, Any?>>? = null

    fun backupProfiles(ids: List<Int>) {
        profilesBackup = try {
            ids.associateWith { id ->
                HashMap(
                    context.getSharedPreferences(
                        "profile_$id", Context.MODE_PRIVATE
                    ).all
                )
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error backing up profiles: ${e.message}")
            null
        }
    }

    fun restoreProfilesBackup(): Boolean {
        val backup = profilesBackup ?: return false
        if (isReleased) return false
        return try {
            checkEffect()
            backup.forEach { (id, values) ->
                val prefs = getProfilePrefs(id)
                prefs.edit().clear().apply()
                val ed = prefs.edit()
                values.forEach { (k, v) ->
                    when (v) {
                        is Boolean -> ed.putBoolean(k, v)
                        is Int -> ed.putInt(k, v)
                        is Long -> ed.putLong(k, v)
                        is Float -> ed.putFloat(k, v)
                        is String -> ed.putString(k, v)
                        is Set<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            ed.putStringSet(k, v as Set<String>)
                        }
                    }
                }
                ed.apply()
                restoreProfilePreset(id)
                applyProfileSettings(id)
            }
            profilesBackup = null
            true
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error restoring profiles backup: ${e.message}")
            false
        }
    }

    private fun deserializeGains(gains: IntArray, bandMode: BandMode): List<BandGain> {
        val frequencies = when (bandMode) {
            BandMode.TEN_BAND -> BAND_FREQUENCIES_10
            BandMode.FIFTEEN_BAND -> BAND_FREQUENCIES_15
            BandMode.TWENTY_BAND -> BAND_FREQUENCIES_20
        }
        
        val indices = when (bandMode) {
            BandMode.TEN_BAND -> TEN_BAND_INDICES
            BandMode.FIFTEEN_BAND -> FIFTEEN_BAND_INDICES
            BandMode.TWENTY_BAND -> (0..19).toList()
        }
        
        return frequencies.mapIndexed { index, freq ->
            val gainIndex = indices.getOrNull(index) ?: index
            BandGain(frequency = freq, gain = gains.getOrElse(gainIndex) { 0 })
        }
    }

    private fun serializeGains(bandGains: List<BandGain>, bandMode: BandMode): IntArray {
        val result = IntArray(20) { 0 }
        
        when (bandMode) {
            BandMode.TEN_BAND -> {
                TEN_BAND_INDICES.forEachIndexed { index, targetIndex ->
                    if (index < bandGains.size && targetIndex < 20) {
                        result[targetIndex] = bandGains[index].gain
                    }
                }
                for (i in 0 until 19 step 2) {
                    if (i + 2 < 20) {
                        result[i + 1] = (result[i] + result[i + 2]) / 2
                    }
                }   
                result[19] = result[18]
            }
            BandMode.FIFTEEN_BAND -> {
                FIFTEEN_BAND_INDICES.forEachIndexed { index, targetIndex ->
                    if (index < bandGains.size && targetIndex < 20) {
                        result[targetIndex] = bandGains[index].gain
                    }
                }
                val missing = (0..19).filter { it !in FIFTEEN_BAND_INDICES }
                missing.forEach { idx ->
                    val prev = FIFTEEN_BAND_INDICES.filter { it < idx }.maxOrNull() ?: 0
                    val next = FIFTEEN_BAND_INDICES.filter { it > idx }.minOrNull() ?: 19
                    
                    if (prev < idx && next > idx && prev < 20 && next < 20) {
                        val prevValue = result[prev]
                        val nextValue = result[next]
                        val ratio = (idx - prev).toFloat() / (next - prev)
                        result[idx] = (prevValue + ratio * (nextValue - prevValue)).toInt()
                    } else if (prev < 20) {
                        result[idx] = result[prev]
                    }
                }
            }
            BandMode.TWENTY_BAND -> {
                bandGains.forEachIndexed { index, bandGain ->
                    if (index < 20) {
                        result[index] = bandGain.gain
                    }
                }
            }
        }
        
        return result
    }

    fun getMidEnhancerEnabled(profile: Int): Boolean {
        val prefs = getProfilePrefs(profile)
        return prefs.getBoolean(DolbyConstants.PREF_MID, false)
    }

    fun setMidEnhancerEnabled(profile: Int, enabled: Boolean) {
        getProfilePrefs(profile).edit().putBoolean(DolbyConstants.PREF_MID, enabled).apply()
    }

    fun getMidLevel(profile: Int): Int {
        val prefs = getProfilePrefs(profile)
        return prefs.getInt(DolbyConstants.PREF_MID_LEVEL, 0)
    }

    /**
     * System-wide left/right balance in [-1f, 1f] (0 = center). Reads the
     * platform "master_balance" setting honored by the audio flinger.
     */
    fun getChannelBalance(): Float {
        return try {
            Settings.System.getFloat(
                context.contentResolver, MASTER_BALANCE_KEY, 0f
            ).coerceIn(-1f, 1f)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error reading channel balance: ${e.message}")
            0f
        }
    }

    /**
     * Applies the balance immediately through AudioManager when the (hidden)
     * setMasterBalance API is present, and always persists it to the
     * "master_balance" system setting so it survives. Returns false when
     * neither path is permitted on this device.
     */
    fun setChannelBalance(balance: Float): Boolean {
        if (isReleased) return false
        val value = balance.coerceIn(-1f, 1f)
        try {
            val method = AudioManager::class.java.getMethod(
                "setMasterBalance", Float::class.javaPrimitiveType
            )
            method.invoke(audioManager, value)
            persistBalanceSetting(value)
            return true
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "setMasterBalance API unavailable: ${e.message}")
        }
        return try {
            if (Settings.System.putFloat(context.contentResolver, MASTER_BALANCE_KEY, value)) {
                persistBalanceSetting(value)
                true
            } else {
                false
            }
        } catch (e: SecurityException) {
            DolbyConstants.dlog(TAG, "WRITE_SETTINGS denied for balance: ${e.message}")
            false
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error setting channel balance: ${e.message}")
            false
        }
    }

    private fun persistBalanceSetting(value: Float) {
        defaultPrefs.edit().putFloat(DolbyConstants.PREF_BALANCE, value).apply()
    }

    fun setMidLevel(profile: Int, level: Int) {
        if (isReleased) return
        
        DolbyConstants.dlog(TAG, "setMidLevel: profile=$profile level=$level")

        if (level !in 0..100) {
            DolbyConstants.dlog(TAG, "setMidLevel: invalid level $level")
            throw IllegalArgumentException("Mid level must be between 0 and 100")
        }

        try {
            val prefs = getProfilePrefs(profile)
            val previousLevel = prefs.getInt(DolbyConstants.PREF_MID_LEVEL, 0)

            prefs.edit().putInt(DolbyConstants.PREF_MID_LEVEL, level).apply()
            setMidEnhancerEnabled(profile, level > 0)

            checkEffect()
            val currentGains = dolbyEffect.getDapParameter(DsParam.GEQ_BAND_GAINS, profile)
            val modifiedGains = currentGains.copyOf()

            if (previousLevel > 0) {
                val previousGain = (previousLevel * MID_GAIN_MULTIPLIER).toInt()
                for (i in 5..13) {
                    if (i < modifiedGains.size) {
                        modifiedGains[i] = (modifiedGains[i] - previousGain).coerceIn(-150, 150)
                    }
                }
            }

            if (level > 0) {
                val midGain = (level * MID_GAIN_MULTIPLIER).toInt()
                for (i in 5..13) {
                    if (i < modifiedGains.size) {
                        modifiedGains[i] = (modifiedGains[i] + midGain).coerceIn(-150, 150)
                    }
                }
            }

            dolbyEffect.setDapParameter(DsParam.GEQ_BAND_GAINS, modifiedGains, profile)
            
            val gainsString = modifiedGains.joinToString(",")
            prefs.edit().putString(DolbyConstants.PREF_PRESET, gainsString).apply()
            
            DolbyConstants.dlog(TAG, "setMidLevel: success")
        } catch (e: IllegalArgumentException) {
            DolbyConstants.dlog(TAG, "setMidLevel: validation error - ${e.message}")
            val prefs = getProfilePrefs(profile)
            prefs.edit().putInt(DolbyConstants.PREF_MID_LEVEL, 0).apply()
            throw e
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "setMidLevel: unexpected error - ${e.message}")
            throw e
        }
    }
    
    private fun release() {
        if (!isReleased) {
            DolbyConstants.dlog(TAG, "Releasing repository resources")
            isReleased = true
            try {
                dolbyEffect.release()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error releasing effect: ${e.message}")
            }
        }
    }
    
    override fun close() {
        release()
    }

    companion object {
        private const val TAG = "DolbyRepository"
        private const val EFFECT_PRIORITY = 100
        /** Codec default for VOLUME_LEVELER_AMOUNT (param 116), range 0-10. */
        const val LEVELER_AMOUNT_DEFAULT = 7
        // Platform "master_balance" setting (use the literal: the SDK constant
        // is not guaranteed present on every target).
        private const val MASTER_BALANCE_KEY = "master_balance"

        private val OUTPUT_DEVICE_PRIORITY = listOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        )
        
        private const val BASS_GAIN_MULTIPLIER = 1.4f
        private const val MID_GAIN_MULTIPLIER = 1.3f
        private const val TREBLE_GAIN_MULTIPLIER = 1.5f
        
        private val ATTRIBUTES_MEDIA = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val BAND_FREQUENCIES_10 = listOf(32, 64, 125, 250, 500, 1000, 2250, 5000, 10000, 19688)
        
        val BAND_FREQUENCIES_15 = listOf(
            32, 47, 94, 141, 234, 469, 844, 1313, 2250, 3750, 5813, 9000, 11250, 13875, 19688
        )
        
        val BAND_FREQUENCIES_20 = listOf(
            32, 47, 141, 234, 328, 469, 656, 844, 1031, 1313,
            1688, 2250, 3000, 3750, 4688, 5813, 7125, 9000, 11250, 19688
        )
        
        private val TEN_BAND_INDICES = listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18)
        
        private val FIFTEEN_BAND_INDICES = listOf(0, 1, 2, 3, 4, 5, 6, 8, 11, 12, 14, 15, 17, 18, 19)
        
        private val BASS_CURVES = listOf(
            floatArrayOf(
                1.00f, 1.00f, 0.95f, 0.90f, 0.80f, 0.70f, 0.55f, 0.40f, 0.25f, 0.15f,
                0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f
            ),
            floatArrayOf(
                1.20f, 1.15f, 1.05f, 0.90f, 0.70f, 0.55f, 0.40f, 0.25f, 0.10f, 0.05f,
                0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f
            ),
            floatArrayOf(
                0.90f, 0.95f, 1.00f, 1.00f, 0.90f, 0.75f, 0.60f, 0.45f, 0.30f, 0.20f,
                0.10f, 0.05f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f
            )
        )
    }
}
