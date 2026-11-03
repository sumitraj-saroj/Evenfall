package com.evenfall.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.HistoryDao
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.db.entity.HistoryEntity

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class EvenfallDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `books` ADD COLUMN `titleOverride` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `authorOverride` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `narrator` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `narratorOverride` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `seriesName` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `seriesOrder` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `coverOverrideUri` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `finishedAt` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `isUnavailable` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `chapters` ADD COLUMN `titleOverride` TEXT DEFAULT NULL")
            }
        }
    }
}
