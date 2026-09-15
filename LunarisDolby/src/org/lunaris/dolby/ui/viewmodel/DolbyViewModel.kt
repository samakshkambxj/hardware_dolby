/*
 * Copyright (C) 2024-2025 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.R
import org.lunaris.dolby.data.DolbyRepository
import org.lunaris.dolby.data.DeviceStateManager
import org.lunaris.dolby.data.SceneRepository
import org.lunaris.dolby.data.SleepTimerManager
import org.lunaris.dolby.data.SleepTimerState
import org.lunaris.dolby.data.SpatializerManager
import org.lunaris.dolby.domain.models.*
import org.lunaris.dolby.service.DolbyEffectService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelChildren

class DolbyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DolbyRepository(application)
    private val sceneRepository = SceneRepository(application)
    private val deviceStateManager = DeviceStateManager(application)
    private val sleepTimer = SleepTimerManager(application)
    private val spatializerManager = SpatializerManager(application)

    val spatializerSupported: StateFlow<Boolean> = spatializerManager.isSupported
    val spatializerAvailable: StateFlow<Boolean> = spatializerManager.isAvailable
    val spatializerEnabled: StateFlow<Boolean> = spatializerManager.isEnabled
    val headTrackingAvailable: StateFlow<Boolean> = spatializerManager.isHeadTrackingAvailable
    val headTrackingEnabled: StateFlow<Boolean> = spatializerManager.isHeadTrackingEnabled

    private val _spatialError = MutableStateFlow<String?>(null)
    val spatialError: StateFlow<String?> = _spatialError.asStateFlow()

    private val _uiState = MutableStateFlow<DolbyUiState>(DolbyUiState.Loading)
    val uiState: StateFlow<DolbyUiState> = _uiState.asStateFlow()
    val currentProfile: StateFlow<Int> = repository.currentProfile

    private val _scenes = MutableStateFlow<List<Scene>>(emptyList())
    val scenes: StateFlow<List<Scene>> = _scenes.asStateFlow()

    private val _deviceScenes = MutableStateFlow<Map<String, String>>(emptyMap())
    val deviceScenes: StateFlow<Map<String, String>> = _deviceScenes.asStateFlow()

    private val _sleepState = MutableStateFlow(SleepTimerState())
    val sleepState: StateFlow<SleepTimerState> = _sleepState.asStateFlow()
    private var sleepTicker: Job? = null

    private val _channelBalance = MutableStateFlow(0f)
    val channelBalance: StateFlow<Float> = _channelBalance.asStateFlow()

    private val _balanceError = MutableStateFlow<String?>(null)
    val balanceError: StateFlow<String?> = _balanceError.asStateFlow()
    
    private var audioOutputStateJob: Job? = null
    private var profileChangeJob: Job? = null
    @Volatile private var isCleared = false

    init {
        DolbyConstants.dlog(TAG, "ViewModel initialized")
        loadSettings()
        refreshScenes()
        refreshDeviceScenes()
        refreshSleepState()
        refreshBalance()
        refreshSpatializer()
        observeAudioOutputState()
        observeProfileChanges()
    }
    
    private fun observeAudioOutputState() {
        audioOutputStateJob?.cancel()
        audioOutputStateJob = viewModelScope.launch {
            repository.activeAudioDevice.collect {
                if (!isCleared) {
                    DolbyConstants.dlog(TAG, "Audio output changed: ${it.name} (${it.category})")
                    loadSettings()
                    refreshSpatializer()
                }
            }
        }
    }

    fun refreshSpatializer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                spatializerManager.refresh()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error refreshing spatializer: ${e.message}")
            }
        }
    }

    fun setSpatialAudioEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ok = spatializerManager.setEnabled(enabled)
                spatializerManager.refresh()
                if (!ok) {
                    _spatialError.value =
                        getApplication<Application>().getString(R.string.spatial_change_failed)
                }
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting spatial audio: ${e.message}")
                _spatialError.value =
                    getApplication<Application>().getString(R.string.spatial_change_failed)
            }
        }
    }

    fun setHeadTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ok = spatializerManager.setHeadTrackingEnabled(enabled)
                spatializerManager.refresh()
                if (!ok) {
                    _spatialError.value =
                        getApplication<Application>().getString(R.string.spatial_change_failed)
                }
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting head tracking: ${e.message}")
                _spatialError.value =
                    getApplication<Application>().getString(R.string.spatial_change_failed)
            }
        }
    }

    fun clearSpatialError() {
        _spatialError.value = null
    }
    
    private fun observeProfileChanges() {
        profileChangeJob?.cancel()
        profileChangeJob = viewModelScope.launch {
            repository.currentProfile.collect {
                if (!isCleared) {
                    DolbyConstants.dlog(TAG, "Profile changed to: $it")
                    loadSettings()
                }
            }
        }
    }

    fun loadSettings() {
        if (isCleared) {
            DolbyConstants.dlog(TAG, "ViewModel cleared, skipping loadSettings")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val enabled = repository.getDolbyEnabled()
                val profile = repository.getCurrentProfile()
                val bandMode = repository.getBandMode()
                
                val settings = DolbySettings(
                    enabled = enabled,
                    currentProfile = profile,
                    bassEnhancerEnabled = repository.getBassEnhancerEnabled(profile),
                    volumeLevelerEnabled = repository.getVolumeLevelerEnabled(profile),
                    bandMode = bandMode
                )
                
                val profileSettings = ProfileSettings(
                    profile = profile,
                    ieqPreset = repository.getIeqPreset(profile),
                    headphoneVirtualizerEnabled = repository.getHeadphoneVirtualizerEnabled(profile),
                    speakerVirtualizerEnabled = repository.getSpeakerVirtualizerEnabled(profile),
                    stereoWideningAmount = repository.getStereoWideningAmount(profile),
                    dialogueEnhancerEnabled = repository.getDialogueEnhancerEnabled(profile),
                    dialogueEnhancerAmount = repository.getDialogueEnhancerAmount(profile),
                    bassLevel = repository.getBassLevel(profile),
                    midLevel = repository.getMidLevel(profile),
                    trebleLevel = repository.getTrebleLevel(profile),
                    bassCurve = repository.getBassCurve(profile)
                )
                
                if (!isCleared) {
                    _uiState.value = DolbyUiState.Success(
                        settings = settings,
                        profileSettings = profileSettings,
                        currentPresetName = repository.getPresetName(profile),
                        isOnSpeaker = repository.isOnSpeaker.value,
                        activeAudioDevice = repository.activeAudioDevice.value
                    )
                }
            } catch (e: Exception) {
                if (!isCleared) {
                    DolbyConstants.dlog(TAG, "Error loading settings: ${e.message}")
                    _uiState.value = DolbyUiState.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun setDolbyEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.setDolbyEnabled(enabled)
                if (enabled) {
                    DolbyEffectService.start(getApplication())
                } else {
                    DolbyEffectService.stop(getApplication())
                }
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting Dolby enabled: ${e.message}")
            }
        }
    }

    fun setProfile(profile: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.setCurrentProfile(profile)
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting profile: ${e.message}")
            }
        }
    }

    fun setBassEnhancer(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setBassEnhancerEnabled(profile, enabled)
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting bass enhancer: ${e.message}")
            }
        }
    }

    fun setBassLevel(level: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setBassLevel(profile, level)
                loadSettings()
            } catch (e: IllegalArgumentException) {
                DolbyConstants.dlog(TAG, "Invalid bass level: ${e.message}")
                _uiState.value = DolbyUiState.Error("Invalid bass level: ${e.message}")
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting bass level: ${e.message}")
                _uiState.value = DolbyUiState.Error("Failed to set bass level")
            }
        }
    }

    fun setBassCurve(curve: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setBassCurve(profile, curve)
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting bass curve: ${e.message}")
            }
        }
    }

    fun setMidLevel(level: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setMidLevel(profile, level)
                loadSettings()
            } catch (e: IllegalArgumentException) {
                DolbyConstants.dlog(TAG, "Invalid mid level: ${e.message}")
                _uiState.value = DolbyUiState.Error("Invalid mid level: ${e.message}")
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting mid level: ${e.message}")
                _uiState.value = DolbyUiState.Error("Failed to set mid level")
            }
        }
    }

    fun setTrebleLevel(level: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setTrebleLevel(profile, level)
                loadSettings()
            } catch (e: IllegalArgumentException) {
                DolbyConstants.dlog(TAG, "Invalid treble level: ${e.message}")
                _uiState.value = DolbyUiState.Error("Invalid treble level: ${e.message}")
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting treble level: ${e.message}")
                _uiState.value = DolbyUiState.Error("Failed to set treble level")
            }
        }
    }

    fun setVolumeLeveler(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setVolumeLevelerEnabled(profile, enabled)
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting volume leveler: ${e.message}")
            }
        }
    }

    fun setIeqPreset(preset: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setIeqPreset(profile, preset)
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting IEQ preset: ${e.message}")
            }
        }
    }

    fun setHeadphoneVirtualizer(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setHeadphoneVirtualizerEnabled(profile, enabled)
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting headphone virtualizer: ${e.message}")
            }
        }
    }

    fun setSpeakerVirtualizer(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setSpeakerVirtualizerEnabled(profile, enabled)
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting speaker virtualizer: ${e.message}")
            }
        }
    }

    fun setStereoWidening(amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setStereoWideningAmount(profile, amount)
                // Widening is inaudible unless the matching virtualizer path is
                // live — engage it so the slider always yields an actual effect.
                if (repository.isOnSpeaker.value) {
                    repository.setSpeakerVirtualizerEnabled(profile, true)
                } else {
                    repository.setHeadphoneVirtualizerEnabled(profile, true)
                }
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting stereo widening: ${e.message}")
            }
        }
    }

    fun setDialogueEnhancer(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setDialogueEnhancerEnabled(profile, enabled)
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting dialogue enhancer: ${e.message}")
            }
        }
    }

    fun setDialogueEnhancerAmount(amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profile = repository.getCurrentProfile()
                repository.setDialogueEnhancerAmount(profile, amount)
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting dialogue enhancer amount: ${e.message}")
            }
        }
    }

    fun resetAllProfiles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.resetAllProfiles()
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error resetting profiles: ${e.message}")
            }
        }
    }

    fun refreshScenes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _scenes.value = sceneRepository.getScenes()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error loading scenes: ${e.message}")
            }
        }
    }

    fun applyScene(scene: Scene) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sceneRepository.applyScene(scene, repository)
                loadSettings()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error applying scene: ${e.message}")
            }
        }
    }

    fun saveScene(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sceneRepository.saveCurrentAsScene(name, repository)
                refreshScenes()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error saving scene: ${e.message}")
            }
        }
    }

    fun deleteScene(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sceneRepository.deleteScene(id)
                refreshScenes()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error deleting scene: ${e.message}")
            }
        }
    }

    fun resetScenes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sceneRepository.deleteAllCustomScenes()
                refreshScenes()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error resetting scenes: ${e.message}")
            }
        }
    }

    fun refreshDeviceScenes() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _deviceScenes.value = deviceStateManager.getDeviceSceneMap()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error loading device scenes: ${e.message}")
            }
        }
    }

    fun currentDeviceKey(): String? {
        return try {
            repository.currentDeviceKey()
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Error getting device key: ${e.message}")
            null
        }
    }

    fun assignDeviceScene(sceneId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val key = repository.currentDeviceKey() ?: return@launch
                deviceStateManager.saveDeviceScene(key, sceneId)
                refreshDeviceScenes()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error assigning device scene: ${e.message}")
            }
        }
    }

    fun clearDeviceScene(deviceKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deviceStateManager.clearDeviceScene(deviceKey)
                refreshDeviceScenes()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error clearing device scene: ${e.message}")
            }
        }
    }

    fun startSleepTimer(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sleepTimer.start(minutes)
                refreshSleepState()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error starting sleep timer: ${e.message}")
            }
        }
    }

    fun cancelSleepTimer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                sleepTimer.cancel()
                _sleepState.value = SleepTimerState()
                sleepTicker?.cancel()
                sleepTicker = null
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error cancelling sleep timer: ${e.message}")
            }
        }
    }

    private fun refreshSleepState() {
        val remaining = SleepTimerManager.remainingMs(getApplication())
        if (remaining > 0L) {
            _sleepState.value = SleepTimerState(active = true, remainingMs = remaining)
            startSleepTicker()
        } else {
            _sleepState.value = SleepTimerState()
            sleepTicker?.cancel()
            sleepTicker = null
        }
    }

    private fun startSleepTicker() {
        sleepTicker?.cancel()
        sleepTicker = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val remaining = SleepTimerManager.remainingMs(getApplication())
                if (remaining <= 0L) {
                    _sleepState.value = SleepTimerState()
                    loadSettings()
                    break
                }
                _sleepState.value = SleepTimerState(active = true, remainingMs = remaining)
            }
        }
    }

    fun refreshBalance() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _channelBalance.value = repository.getChannelBalance()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error loading balance: ${e.message}")
            }
        }
    }

    fun setChannelBalance(balance: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ok = repository.setChannelBalance(balance)
                if (ok) {
                    _channelBalance.value = balance.coerceIn(-1f, 1f)
                } else {
                    _balanceError.value =
                        getApplication<Application>().getString(R.string.balance_unsupported)
                }
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error setting balance: ${e.message}")
                _balanceError.value =
                    getApplication<Application>().getString(R.string.balance_unsupported)
            }
        }
    }

    fun clearBalanceError() {
        _balanceError.value = null
    }

    fun updateSpeakerState() {
        if (!isCleared) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateSpeakerState()
            }
        }
    }
    
    override fun onCleared() {
        DolbyConstants.dlog(TAG, "ViewModel onCleared")
        isCleared = true
        spatializerManager.destroy()
        viewModelScope.coroutineContext.cancelChildren()
        audioOutputStateJob?.cancel()
        audioOutputStateJob = null
        profileChangeJob?.cancel()
        profileChangeJob = null
        sleepTicker?.cancel()
        sleepTicker = null
        repository.close()
        super.onCleared()
    }
    
    companion object {
        private const val TAG = "DolbyViewModel"
    }
}
