package com.crylo.quickchat

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile: copy a number anywhere, pull down the shade, tap. The tile
 * itself never touches the clipboard — Android only lets the focused app read it — so
 * it just opens the main screen and asks it to fill the number in.
 */
class QuickChatTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            subtitle = getString(R.string.tile_subtitle)
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun(::launch) else launch()
    }

    // The Intent overload is the only one before API 34; it's used only below that.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launch() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_FROM_CLIPBOARD)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
