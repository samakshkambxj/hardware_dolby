/*
 * Copyright (C) 2023-24 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

private const val TAG = "DolbyTileService"

class DolbyTileService : TileService() {

    private val dolbyController by lazy { DolbyController.getInstance(applicationContext) }

    override fun onStartListening() {
        refreshTile()
        super.onStartListening()
    }

    override fun onClick() {
        val isDsOn = runCatching { dolbyController.dsOn }.getOrDefault(false)
        runCatching { dolbyController.setDsOnAndPersist(!isDsOn) }
            .onFailure { Log.e(TAG, "Failed to toggle Dolby", it) }
        refreshTile()
        super.onClick()
    }

    /**
     * The effect can temporarily lose control (for example right after boot). A failed read must
     * not take the tile service down, so fall back to the inactive/unknown state.
     */
    private fun refreshTile() {
        val enabled = runCatching { dolbyController.dsOn }.getOrDefault(false)
        val tileSubtitle =
            runCatching { dolbyController.getProfileName() }.getOrNull()
                ?: getString(R.string.dolby_unknown)

        qsTile?.apply {
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = tileSubtitle
            updateTile()
        }
    }
}
