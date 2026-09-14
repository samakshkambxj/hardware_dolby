/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.lunaris.dolby.DolbyConstants
import org.lunaris.dolby.data.DolbyRepository
import org.lunaris.dolby.data.SceneRepository

/**
 * Command interface for automation apps (Tasker, MacroDroid) and scripts.
 * Send an explicit broadcast to `org.lunaris.dolby.service.DolbyCommandReceiver`
 * (or any matching implicit broadcast) with one of the actions below:
 *
 * - [ACTION_TOGGLE]: flips the Dolby master switch. No extras.
 * - [ACTION_SET_ENABLED]: takes boolean extra [EXTRA_ENABLED].
 * - [ACTION_SET_PROFILE]: takes int extra [EXTRA_PROFILE] (0-6, see
 *   R.array.dolby_profile_values: 0 Dynamic, 1 Movie, 2 Music, 3 Game,
 *   4 Work, 5 Casual, 6 Mood).
 * - [ACTION_APPLY_SCENE]: takes string extra [EXTRA_SCENE_ID] with a scene id
 *   ("builtin_movie_night", "builtin_bass_boost", "builtin_podcast",
 *   "builtin_gaming", or a "custom_<...>" id).
 *
 * Example (adb):
 *   adb shell am broadcast -n org.lunaris.dolby/.service.DolbyCommandReceiver \
 *     -a org.lunaris.dolby.action.SET_PROFILE --ei profile 1
 */
class DolbyCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val appContext = context.applicationContext
        val repository = try {
            DolbyRepository(appContext)
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "No audio effect, ignoring $action")
            return
        }
        try {
            when (action) {
                ACTION_TOGGLE -> repository.setDolbyEnabled(!repository.getDolbyEnabled())
                ACTION_SET_ENABLED -> {
                    if (intent.hasExtra(EXTRA_ENABLED)) {
                        repository.setDolbyEnabled(
                            intent.getBooleanExtra(EXTRA_ENABLED, false)
                        )
                    }
                }
                ACTION_SET_PROFILE -> {
                    val profile = intent.getIntExtra(EXTRA_PROFILE, -1)
                    if (profile in MIN_PROFILE..MAX_PROFILE) {
                        repository.setCurrentProfile(profile)
                    }
                }
                ACTION_APPLY_SCENE -> {
                    val sceneId = intent.getStringExtra(EXTRA_SCENE_ID)
                    val scene = sceneId?.let { SceneRepository(appContext).getScene(it) }
                    if (scene != null) {
                        SceneRepository(appContext).applyScene(scene, repository)
                    }
                }
            }
        } catch (e: Exception) {
            DolbyConstants.dlog(TAG, "Command $action failed: ${e.message}")
        } finally {
            try {
                repository.close()
            } catch (e: Exception) {
                DolbyConstants.dlog(TAG, "Error closing repository: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "DolbyCommandReceiver"
        private const val MIN_PROFILE = 0
        private const val MAX_PROFILE = 6

        const val ACTION_TOGGLE = "org.lunaris.dolby.action.TOGGLE"
        const val ACTION_SET_ENABLED = "org.lunaris.dolby.action.SET_ENABLED"
        const val ACTION_SET_PROFILE = "org.lunaris.dolby.action.SET_PROFILE"
        const val ACTION_APPLY_SCENE = "org.lunaris.dolby.action.APPLY_SCENE"

        const val EXTRA_ENABLED = "enabled"
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_SCENE_ID = "scene_id"
    }
}
