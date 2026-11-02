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
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
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
