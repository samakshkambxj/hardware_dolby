/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.data

import android.content.Context
import org.json.JSONObject
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.R
import org.lunaris.dolby.domain.models.Scene

/**
 * Persists user scenes in the "dolby_scenes" preferences file (one JSON value
 * per scene, keyed by scene id) and applies them through [DolbyRepository].
 *
 * Built-in scenes are always present first; custom scenes follow in
 * creation order.
 */
class SceneRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(
        DolbyConstants.PREF_FILE_SCENES, Context.MODE_PRIVATE
    )

    fun getScenes(): List<Scene> {
        val customs = prefs.all.mapNotNull { (key, value) ->
            if (!key.startsWith(CUSTOM_ID_PREFIX)) return@mapNotNull null
            try {
                deserialize(key, value as? String ?: return@mapNotNull null)
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Dropping corrupt scene $key: ${e.message}")
                null
            }
        }.sortedBy { it.id }
        return builtInScenes() + customs
    }

    fun getScene(id: String): Scene? = getScenes().find { it.id == id }

    /**
     * Captures the current Dolby state as a new custom scene. Levels coming
     * from [DolbyRepository] getters are already validated ranges.
     */
    fun saveCurrentAsScene(name: String, dolby: DolbyRepository): Scene? {
        val trimmed = name.trim().take(MAX_NAME_LENGTH)
        if (trimmed.isEmpty()) return null
        val profile = dolby.getCurrentProfile()
        val scene = Scene(
            id = CUSTOM_ID_PREFIX + System.currentTimeMillis(),
            name = trimmed,
            isBuiltIn = false,
            enabled = dolby.getDolbyEnabled(),
            profile = profile,
            ieqPreset = dolby.getIeqPreset(profile),
            bassLevel = dolby.getBassLevel(profile),
            bassCurve = dolby.getBassCurve(profile),
            midLevel = dolby.getMidLevel(profile),
            trebleLevel = dolby.getTrebleLevel(profile),
            volumeLeveler = dolby.getVolumeLevelerEnabled(profile),
            dialogueEnabled = dolby.getDialogueEnhancerEnabled(profile),
            dialogueAmount = dolby.getDialogueEnhancerAmount(profile),
            hpVirtualizer = dolby.getHeadphoneVirtualizerEnabled(profile),
            spkVirtualizer = dolby.getSpeakerVirtualizerEnabled(profile),
            stereoWidening = dolby.getStereoWideningAmount(profile)
        )
        prefs.edit().putString(scene.id, serialize(scene)).apply()
        return scene
    }

    fun deleteScene(id: String): Boolean {
        if (!id.startsWith(CUSTOM_ID_PREFIX)) return false
        prefs.edit().remove(id).apply()
        return true
    }

    /**
     * Deletes all user-created scenes. Built-ins are hardcoded and unaffected.
     * Returns the number of scenes removed.
     */
    fun deleteAllCustomScenes(): Int {
        val customKeys = prefs.all.keys.filter { it.startsWith(CUSTOM_ID_PREFIX) }
        if (customKeys.isEmpty()) return 0
        prefs.edit().apply {
            customKeys.forEach { remove(it) }
            apply()
        }
        return customKeys.size
    }

    /**
     * Applies a scene. The profile is switched first so that all subsequent
     * per-profile settings land on the scene's profile. Must be called off the
     * main thread (touches the audio effect).
     */
    fun applyScene(scene: Scene, dolby: DolbyRepository) {
        dolby.setDolbyEnabled(scene.enabled)
        dolby.setCurrentProfile(scene.profile.coerceIn(0, MAX_PROFILE))
        dolby.setIeqPreset(scene.profile, scene.ieqPreset)
        dolby.setBassCurve(scene.profile, scene.bassCurve.coerceIn(0, MAX_BASS_CURVE))
        dolby.setBassLevel(scene.profile, scene.bassLevel.coerceIn(0, 100))
        dolby.setMidLevel(scene.profile, scene.midLevel.coerceIn(0, 100))
        dolby.setTrebleLevel(scene.profile, scene.trebleLevel.coerceIn(0, 100))
        dolby.setVolumeLevelerEnabled(scene.profile, scene.volumeLeveler)
        dolby.setDialogueEnhancerEnabled(scene.profile, scene.dialogueEnabled)
        dolby.setDialogueEnhancerAmount(
            scene.profile, scene.dialogueAmount.coerceIn(1, MAX_DIALOGUE_AMOUNT)
        )
        dolby.setHeadphoneVirtualizerEnabled(scene.profile, scene.hpVirtualizer)
        dolby.setSpeakerVirtualizerEnabled(scene.profile, scene.spkVirtualizer)
        dolby.setStereoWideningAmount(
            scene.profile, scene.stereoWidening.coerceIn(MIN_WIDENING, MAX_WIDENING)
        )
    }

    private fun builtInScenes(): List<Scene> = listOf(
        Scene(
            id = "builtin_movie_night",
            name = context.getString(R.string.scene_builtin_movie),
            isBuiltIn = true,
            enabled = true,
            profile = 1,
            ieqPreset = 0,
            bassLevel = 40,
            bassCurve = 0,
            midLevel = 0,
            trebleLevel = 0,
            volumeLeveler = true,
            dialogueEnabled = true,
            dialogueAmount = 8,
            hpVirtualizer = true,
            spkVirtualizer = true,
            stereoWidening = 48
        ),
        Scene(
            id = "builtin_bass_boost",
            name = context.getString(R.string.scene_builtin_bass),
            isBuiltIn = true,
            enabled = true,
            profile = 2,
            ieqPreset = 0,
            bassLevel = 70,
            bassCurve = 1,
            midLevel = 0,
            trebleLevel = 20,
            volumeLeveler = false,
            dialogueEnabled = false,
            dialogueAmount = 6,
            hpVirtualizer = false,
            spkVirtualizer = false,
            stereoWidening = 32
        ),
        Scene(
            id = "builtin_podcast",
            name = context.getString(R.string.scene_builtin_podcast),
            isBuiltIn = true,
            enabled = true,
            profile = 4,
            ieqPreset = 0,
            bassLevel = 0,
            bassCurve = 0,
            midLevel = 25,
            trebleLevel = 10,
            volumeLeveler = true,
            dialogueEnabled = true,
            dialogueAmount = 10,
            hpVirtualizer = false,
            spkVirtualizer = false,
            stereoWidening = 32
        ),
        Scene(
            id = "builtin_gaming",
            name = context.getString(R.string.scene_builtin_gaming),
            isBuiltIn = true,
            enabled = true,
            profile = 3,
            ieqPreset = 0,
            bassLevel = 30,
            bassCurve = 0,
            midLevel = 0,
            trebleLevel = 10,
            volumeLeveler = false,
            dialogueEnabled = false,
            dialogueAmount = 6,
            hpVirtualizer = true,
            spkVirtualizer = false,
            stereoWidening = 56
        ),
        Scene(
            id = "builtin_music",
            name = context.getString(R.string.scene_builtin_music),
            isBuiltIn = true,
            enabled = true,
            profile = 2,
            ieqPreset = 2,
            bassLevel = 35,
            bassCurve = 0,
            midLevel = 10,
            trebleLevel = 20,
            volumeLeveler = false,
            dialogueEnabled = false,
            dialogueAmount = 6,
            hpVirtualizer = true,
            spkVirtualizer = true,
            stereoWidening = 40
        ),
        Scene(
            id = "builtin_night",
            name = context.getString(R.string.scene_builtin_night),
            isBuiltIn = true,
            enabled = true,
            profile = 1,
            ieqPreset = 0,
            bassLevel = 15,
            bassCurve = 0,
            midLevel = 5,
            trebleLevel = 5,
            volumeLeveler = true,
            dialogueEnabled = true,
            dialogueAmount = 6,
            hpVirtualizer = false,
            spkVirtualizer = false,
            stereoWidening = 32
        ),
        Scene(
            id = "builtin_vocal",
            name = context.getString(R.string.scene_builtin_vocal),
            isBuiltIn = true,
            enabled = true,
            profile = 4,
            ieqPreset = 1,
            bassLevel = 0,
            bassCurve = 0,
            midLevel = 40,
            trebleLevel = 20,
            volumeLeveler = true,
            dialogueEnabled = true,
            dialogueAmount = 12,
            hpVirtualizer = false,
            spkVirtualizer = false,
            stereoWidening = 32
        ),
        Scene(
            id = "builtin_outdoor",
            name = context.getString(R.string.scene_builtin_outdoor),
            isBuiltIn = true,
            enabled = true,
            profile = 5,
            ieqPreset = 0,
            bassLevel = 50,
            bassCurve = 2,
            midLevel = 10,
            trebleLevel = 30,
            volumeLeveler = false,
            dialogueEnabled = false,
            dialogueAmount = 6,
            hpVirtualizer = false,
            spkVirtualizer = true,
            stereoWidening = 40
        )
    )

    private fun serialize(scene: Scene): String {
        return JSONObject()
            .put(KEY_VERSION, 1)
            .put("name", scene.name)
            .put("enabled", scene.enabled)
            .put("profile", scene.profile)
            .put("ieq", scene.ieqPreset)
            .put("bass", scene.bassLevel)
            .put("bassCurve", scene.bassCurve)
            .put("mid", scene.midLevel)
            .put("treble", scene.trebleLevel)
            .put("leveler", scene.volumeLeveler)
            .put("dialogue", scene.dialogueEnabled)
            .put("dialogueAmount", scene.dialogueAmount)
            .put("hpVirt", scene.hpVirtualizer)
            .put("spkVirt", scene.spkVirtualizer)
            .put("widening", scene.stereoWidening)
            .toString()
    }

    private fun deserialize(id: String, json: String): Scene {
        val o = JSONObject(json)
        return Scene(
            id = id,
            name = o.getString("name"),
            isBuiltIn = false,
            enabled = o.optBoolean("enabled", true),
            profile = o.optInt("profile", 0),
            ieqPreset = o.optInt("ieq", 0),
            bassLevel = o.optInt("bass", 0),
            bassCurve = o.optInt("bassCurve", 0),
            midLevel = o.optInt("mid", 0),
            trebleLevel = o.optInt("treble", 0),
            volumeLeveler = o.optBoolean("leveler", false),
            dialogueEnabled = o.optBoolean("dialogue", false),
            dialogueAmount = o.optInt("dialogueAmount", 6),
            hpVirtualizer = o.optBoolean("hpVirt", false),
            spkVirtualizer = o.optBoolean("spkVirt", false),
            stereoWidening = o.optInt("widening", 32)
        )
    }

    companion object {
        private const val TAG = "SceneRepository"
        private const val CUSTOM_ID_PREFIX = "custom_"
        private const val MAX_NAME_LENGTH = 40
        private const val MAX_PROFILE = 6
        private const val MAX_BASS_CURVE = 2
        private const val MAX_DIALOGUE_AMOUNT = 12
        private const val MIN_WIDENING = 4
        private const val MAX_WIDENING = 64
        private const val KEY_VERSION = "v"
    }
}
