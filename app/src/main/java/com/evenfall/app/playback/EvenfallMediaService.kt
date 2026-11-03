package com.evenfall.app.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.evenfall.app.MainActivity
import com.evenfall.app.data.datastore.PreferencesManager
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookmarkType
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EvenfallMediaService : MediaLibraryService() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var bookDao: BookDao

    @Inject
    lateinit var chapterDao: ChapterDao

    @Inject
    lateinit var bookmarkDao: BookmarkDao

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val gestureInterpreter by lazy {
        HeadsetGestureInterpreter(scope = serviceScope) { gesture ->
            handleHeadsetGesture(gesture)
        }
    }

    private fun handleHeadsetGesture(gesture: HeadsetGesture) {
        serviceScope.launch {
            when (gesture) {
                HeadsetGesture.SINGLE_PRESS -> {
                    playbackManager.togglePlayPause()
                }
                HeadsetGesture.DOUBLE_PRESS -> {
                    val actionName = preferencesManager.headsetDoublePressAction.first()
                    executeHeadsetAction(actionName)
                }
                HeadsetGesture.TRIPLE_PRESS -> {
                    val actionName = preferencesManager.headsetTriplePressAction.first()
                    executeHeadsetAction(actionName)
                }
                HeadsetGesture.LONG_PRESS -> {
                    val actionName = preferencesManager.headsetLongPressAction.first()
                    executeHeadsetAction(actionName)
                }
            }
        }
    }

    private suspend fun executeHeadsetAction(actionName: String) {
        when (actionName) {
            HeadsetAction.NEXT_CHAPTER.name -> playbackManager.nextChapter()
            HeadsetAction.PREVIOUS_CHAPTER.name -> playbackManager.previousChapter()
            HeadsetAction.SKIP_FORWARD.name -> playbackManager.skipForward()
            HeadsetAction.SKIP_BACK.name -> playbackManager.skipBackward()
            HeadsetAction.SAVE_BOOKMARK.name -> {
                val state = playbackManager.playbackState.value
                val book = state.currentBook ?: return
                val chapter = state.currentChapter ?: return
                val count = bookmarkDao.getBookmarkCountForBook(book.id).first()
                bookmarkDao.insert(
                    BookmarkEntity(
                        bookId = book.id,
                        chapterIndex = chapter.chapterIndex,
                        chapterTitle = chapter.title,
                        positionMs = state.chapterPositionMs,
                        bookProgressFraction = state.totalProgressFraction,
                        title = "Bookmark ${count + 1}",
                        type = BookmarkType.BOOKMARK
                    )
                )
            }
            HeadsetAction.TOGGLE_SLEEP_TIMER.name -> {
                val state = playbackManager.playbackState.value
                if (state.sleepTimerIsActive) {
                    playbackManager.cancelSleepTimer()
                } else {
                    playbackManager.startSleepTimer(30)
                }
            }
            HeadsetAction.CHANGE_SPEED.name -> {
                val current = playbackManager.playbackState.value.playbackSpeed
                val nextSpeed = if (current >= 2.5f) 1.0f else ((current + 0.1f) * 10).toInt() / 10f
                playbackManager.setPlaybackSpeed(nextSpeed)
            }
            HeadsetAction.NONE.name -> { /* no-op */ }
            else -> playbackManager.togglePlayPause()
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaLibrarySession: MediaLibrarySession? = null

    companion object {
        const val ROOT_ID = "evenfall_root"
        const val CONTINUING_ID = "evenfall_continuing"
        const val ALL_BOOKS_ID = "evenfall_all_books"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callback = object : MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setTitle("Evenfall Audiobooks")
                            .build()
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        val items = mutableListOf<MediaItem>()
                        when (parentId) {
                            ROOT_ID -> {
                                items.add(
                                    MediaItem.Builder()
                                        .setMediaId(CONTINUING_ID)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setIsBrowsable(true)
                                                .setIsPlayable(false)
                                                .setTitle("Continue Listening")
                                                .build()
                                        )
                                        .build()
                                )
                                items.add(
                                    MediaItem.Builder()
                                        .setMediaId(ALL_BOOKS_ID)
                                        .setMediaMetadata(
                                            MediaMetadata.Builder()
                                                .setIsBrowsable(true)
                                                .setIsPlayable(false)
                                                .setTitle("All Audiobooks")
                                                .build()
                                        )
                                        .build()
                                )
                            }
                            CONTINUING_ID -> {
                                val books = bookDao.getCurrentBooks().first()
                                books.forEach { book ->
                                    items.add(
                                        MediaItem.Builder()
                                            .setMediaId("book_${book.id}")
                                            .setMediaMetadata(
                                                MediaMetadata.Builder()
                                                    .setIsBrowsable(true)
                                                    .setIsPlayable(true)
                                                    .setTitle(book.title)
                                                    .setArtist(book.author)
                                                    .build()
                                            )
                                            .build()
                                    )
                                }
                            }
                            ALL_BOOKS_ID -> {
                                val books = bookDao.getAllBooks().first()
                                books.forEach { book ->
                                    items.add(
                                        MediaItem.Builder()
                                            .setMediaId("book_${book.id}")
                                            .setMediaMetadata(
                                                MediaMetadata.Builder()
                                                    .setIsBrowsable(true)
                                                    .setIsPlayable(true)
                                                    .setTitle(book.title)
                                                    .setArtist(book.author)
                                                    .build()
                                            )
                                            .build()
                                    )
                                }
                            }
                            else -> {
                                if (parentId.startsWith("book_")) {
                                    val bookId = parentId.removePrefix("book_").toLongOrNull()
                                    if (bookId != null) {
                                        val chapters = chapterDao.getChaptersForBookSync(bookId)
                                        chapters.forEach { ch ->
                                            items.add(
                                                MediaItem.Builder()
                                                    .setMediaId("chapter_${ch.id}")
                                                    .setMediaMetadata(
                                                        MediaMetadata.Builder()
                                                            .setIsBrowsable(false)
                                                            .setIsPlayable(true)
                                                            .setTitle(ch.title)
                                                            .build()
                                                    )
                                                    .build()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        future.set(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
                    } catch (e: Exception) {
                        future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE))
                    }
                }
                return future
            }

            override fun onSetMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
                startIndex: Int,
                startPositionMs: Long
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val first = mediaItems.firstOrNull()
                if (first != null && first.mediaId.startsWith("book_")) {
                    val bookId = first.mediaId.removePrefix("book_").toLongOrNull()
                    if (bookId != null) {
                        playbackManager.loadBook(bookId, autoPlay = true)
                    }
                }
                return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
            }

            override fun onMediaButtonEvent(
                session: MediaSession,
                controllerInfo: MediaSession.ControllerInfo,
                intent: Intent
            ): Boolean {
                val keyEvent: android.view.KeyEvent? = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                if (keyEvent != null) {
                    val keyCode = keyEvent.keyCode
                    if (keyCode == android.view.KeyEvent.KEYCODE_HEADSETHOOK ||
                        keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                        keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY ||
                        keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
                    ) {
                        if (keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                            gestureInterpreter.onKeyDown(keyEvent.eventTime)
                            return true
                        } else if (keyEvent.action == android.view.KeyEvent.ACTION_UP) {
                            gestureInterpreter.onKeyUp(keyEvent.eventTime)
                            return true
                        }
                    }
                }
                return super.onMediaButtonEvent(session, controllerInfo, intent)
            }
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, playbackManager.exoPlayer, callback)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            release()
            mediaLibrarySession = null
        }
        playbackManager.release()
        super.onDestroy()
    }
}
