package com.evenfall.app.service

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.evenfall.app.R
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.playback.PlaybackManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EvenfallTileService : TileService() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var bookDao: BookDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObserverJob: Job? = null

    companion object {
        fun requestTileUpdate(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, EvenfallTileService::class.java)
                )
            } catch (_: Exception) {}
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileUi()

        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            playbackManager.playbackState.collectLatest {
                updateTileUi()
            }
        }
    }

    override fun onStopListening() {
        stateObserverJob?.cancel()
        stateObserverJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val isPlaying = playbackManager.playbackState.value.isPlaying

        if (isPlaying) {
            playbackManager.pause()
        } else {
            val currentBook = playbackManager.playbackState.value.currentBook
            if (currentBook != null) {
                playbackManager.play()
            } else {
                // Cold start: resume last played book
                serviceScope.launch(Dispatchers.IO) {
                    val lastBook = bookDao.getMostRecentlyPlayedBookSync()
                        ?: bookDao.getAllBooksSync().firstOrNull()
                    if (lastBook != null) {
                        serviceScope.launch(Dispatchers.Main) {
                            playbackManager.loadBook(lastBook)
                            playbackManager.play()
                        }
                    }
                }
            }
        }
        updateTileUi()
    }

    private fun updateTileUi() {
        val tile = qsTile ?: return
        val state = playbackManager.playbackState.value
        val isPlaying = state.isPlaying
        val book = state.currentBook

        if (isPlaying) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.tile_pause)
            tile.subtitle = book?.displayTitle ?: getString(R.string.app_name)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.tile_resume)
            tile.subtitle = book?.displayTitle ?: getString(R.string.tile_no_book)
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
