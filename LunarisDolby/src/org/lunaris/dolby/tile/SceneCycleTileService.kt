/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.tile

import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.lunaris.dolby.R
import org.lunaris.dolby.data.DolbyRepository
import org.lunaris.dolby.data.SceneRepository

/**
 * Tap to cycle through scenes (built-in first, then custom). Subtitle shows
 * the last applied scene. Long-press opens the app via QS_TILE_PREFERENCES.
 */
class SceneCycleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        var repository: DolbyRepository? = null
        try {
            repository = DolbyRepository(applicationContext)
            val scenes = SceneRepository(applicationContext).getScenes()
            if (scenes.isEmpty()) return
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastId = prefs.getString(KEY_LAST_SCENE, null)
            val nextIndex = scenes.indexOfFirst { it.id == lastId }
                .let { if (it == -1) 0 else (it + 1) % scenes.size }
            val next = scenes[nextIndex]
            SceneRepository(applicationContext).applyScene(next, repository)
            prefs.edit().putString(KEY_LAST_SCENE, next.id).apply()
        } catch (_: Exception) {
        } finally {
            try {
                repository?.close()
            } catch (_: Exception) {
            }
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            subtitle = lastSceneName() ?: getString(R.string.scenes)
            updateTile()
        }
    }

    private fun lastSceneName(): String? {
        return try {
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastId = prefs.getString(KEY_LAST_SCENE, null) ?: return null
            SceneRepository(applicationContext).getScene(lastId)?.name
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS = "dolby_qs"
        private const val KEY_LAST_SCENE = "last_scene_id"
    }
}
