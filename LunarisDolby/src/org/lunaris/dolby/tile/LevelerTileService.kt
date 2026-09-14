/*
 * Copyright (C) 2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lunaris.dolby.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.lunaris.dolby.R
import org.lunaris.dolby.data.DolbyRepository

/**
 * Toggles the volume leveler for the current profile. Long-press opens the
 * app via QS_TILE_PREFERENCES.
 */
class LevelerTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        var repository: DolbyRepository? = null
        try {
            repository = DolbyRepository(applicationContext)
            val profile = repository.getCurrentProfile()
            repository.setVolumeLevelerEnabled(
                profile, !repository.getVolumeLevelerEnabled(profile)
            )
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
            val enabled = try {
                val repository = DolbyRepository(applicationContext)
                try {
                    val profile = repository.getCurrentProfile()
                    repository.getVolumeLevelerEnabled(profile)
                } finally {
                    try {
                        repository.close()
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
                false
            }
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = if (enabled) getString(R.string.dolby_on)
                       else getString(R.string.dolby_off)
            updateTile()
        }
    }
}
