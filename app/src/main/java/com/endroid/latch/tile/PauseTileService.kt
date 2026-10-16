package com.endroid.latch.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.endroid.latch.data.SettingsStore

@RequiresApi(24)
class PauseTileService : TileService() {
    override fun onStartListening() {
        update()
    }

    override fun onClick() {
        val s = SettingsStore(this)
        s.automationPaused = !s.automationPaused
        update()
    }

    private fun update() {
        val paused = SettingsStore(this).automationPaused
        qsTile?.apply {
            state = if (paused) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            label = if (paused) "Latch paused" else "Latch active"
            updateTile()
        }
    }
}
