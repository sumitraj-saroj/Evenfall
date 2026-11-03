package com.evenfall.app.di

import android.content.Context
import androidx.room.Room
import com.evenfall.app.data.db.EvenfallDatabase
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.HistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EvenfallDatabase {
        return Room.databaseBuilder(
            context,
            EvenfallDatabase::class.java,
            "evenfall.db"
        )
            .addMigrations(EvenfallDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideBookDao(database: EvenfallDatabase): BookDao = database.bookDao()

    @Provides
    fun provideChapterDao(database: EvenfallDatabase): ChapterDao = database.chapterDao()

    @Provides
    fun provideBookmarkDao(database: EvenfallDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideHistoryDao(database: EvenfallDatabase): HistoryDao = database.historyDao()
}
