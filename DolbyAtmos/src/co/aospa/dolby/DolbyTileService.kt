/*
 * Copyright (C) 2023-24 Paranoid Android
 * Copyright (C) 2026 samakshkambxj
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package co.aospa.dolby

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

private const val TAG = "DolbyTileService"

class DolbyTileService : TileService() {

    private val dolbyController by lazy { DolbyController.getInstance(applicationContext) }

    override fun onStartListening() {
        super.onStartListening()
        dolbyController.ensureInitialized()
        qsTile?.apply {
            state = if (dolbyController.dsOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = dolbyController.getProfileName() ?: getString(R.string.dolby_unknown)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        dolbyController.ensureInitialized()
        val isDsOn = dolbyController.dsOn
        dolbyController.setDsOnAndPersist(!isDsOn) // toggle
        qsTile?.apply {
            state = if (isDsOn) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            subtitle = dolbyController.getProfileName() ?: getString(R.string.dolby_unknown)
            updateTile()
        }
    }
}
