package com.evenfall.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.evenfall.app.data.db.dao.BookmarkDao
import com.evenfall.app.data.db.dao.BookDao
import com.evenfall.app.data.db.dao.ChapterDao
import com.evenfall.app.data.db.dao.CollectionDao
import com.evenfall.app.data.db.dao.HistoryDao
import com.evenfall.app.data.db.dao.ScanIssueDao
import com.evenfall.app.data.db.dao.SearchDao
import com.evenfall.app.data.db.entity.BookmarkEntity
import com.evenfall.app.data.db.entity.BookCollectionCrossRef
import com.evenfall.app.data.db.entity.BookEntity
import com.evenfall.app.data.db.entity.ChapterEntity
import com.evenfall.app.data.db.entity.CollectionEntity
import com.evenfall.app.data.db.entity.HistoryEntity
import com.evenfall.app.data.db.entity.RecentSearchEntity
import com.evenfall.app.data.db.entity.ScanIssueEntity
import com.evenfall.app.data.db.entity.SearchIndexFtsEntity

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        ScanIssueEntity::class,
        CollectionEntity::class,
        BookCollectionCrossRef::class,
        RecentSearchEntity::class,
        SearchIndexFtsEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class EvenfallDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun scanIssueDao(): ScanIssueDao
    abstract fun collectionDao(): CollectionDao
    abstract fun searchDao(): SearchDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scan_issues` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `details` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `books` ADD COLUMN `seriesNameOverride` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `seriesOrderOverride` REAL DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `collections` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `book_collection_cross_ref` (
                        `bookId` INTEGER NOT NULL,
                        `collectionId` INTEGER NOT NULL,
                        PRIMARY KEY(`bookId`, `collectionId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_collection_cross_ref_collectionId` ON `book_collection_cross_ref` (`collectionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_book_collection_cross_ref_bookId` ON `book_collection_cross_ref` (`bookId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recent_searches` (
                        `query` TEXT PRIMARY KEY NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS `search_index_fts` USING `FTS4`(
                        `entityType`,
                        `bookId`,
                        `targetId`,
                        `title`,
                        `subtitle`,
                        `content`,
                        `positionMs`,
                        `chapterIndex`
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `books` ADD COLUMN `speedOverride` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `skipBackSecondsOverride` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `skipForwardSecondsOverride` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `silenceSkippingOverride` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `volumeBoostOverride` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `voiceBoostPresetOverride` TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `monoDownmixOverride` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `books` ADD COLUMN `loudnessNormalizationOverride` INTEGER DEFAULT NULL")
            }
        }
    }
}
