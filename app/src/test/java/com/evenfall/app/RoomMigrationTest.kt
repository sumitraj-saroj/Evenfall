package com.evenfall.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.evenfall.app.data.db.EvenfallDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class RoomMigrationTest {

    @Test
    fun testMigration1To2ExecutesAllSchemaAlterStatements() {
        val executedSql = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        EvenfallDatabase.MIGRATION_1_2.migrate(db)

        assertEquals(1, EvenfallDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, EvenfallDatabase.MIGRATION_1_2.endVersion)

        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `titleOverride` TEXT DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `authorOverride` TEXT DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `narrator` TEXT DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `narratorOverride` TEXT DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `seriesName` TEXT DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `seriesOrder` REAL DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `coverOverrideUri` TEXT DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `finishedAt` INTEGER DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `isUnavailable` INTEGER NOT NULL DEFAULT 0"))
        assertTrue(executedSql.contains("ALTER TABLE `chapters` ADD COLUMN `titleOverride` TEXT DEFAULT NULL"))
        assertEquals(10, executedSql.size)
    }

    @Test
    fun testMigration2To3CreatesScanIssuesTable() {
        val executedSql = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        EvenfallDatabase.MIGRATION_2_3.migrate(db)

        assertEquals(2, EvenfallDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, EvenfallDatabase.MIGRATION_2_3.endVersion)

        assertEquals(1, executedSql.size)
        assertTrue(executedSql[0].contains("CREATE TABLE IF NOT EXISTS `scan_issues`"))
        assertTrue(executedSql[0].contains("`filePath` TEXT NOT NULL"))
        assertTrue(executedSql[0].contains("`reason` TEXT NOT NULL"))
    }

    @Test
    fun testMigration3To4CreatesCollectionsFtsAndSeriesColumns() {
        val executedSql = mutableListOf<String>()
        val db = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        EvenfallDatabase.MIGRATION_3_4.migrate(db)

        assertEquals(3, EvenfallDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, EvenfallDatabase.MIGRATION_3_4.endVersion)

        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `seriesNameOverride` TEXT DEFAULT NULL"))
        assertTrue(executedSql.contains("ALTER TABLE `books` ADD COLUMN `seriesOrderOverride` REAL DEFAULT NULL"))
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `collections`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `book_collection_cross_ref`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX IF NOT EXISTS `index_book_collection_cross_ref_collectionId`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX IF NOT EXISTS `index_book_collection_cross_ref_bookId`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `recent_searches`") })
        assertTrue(executedSql.any { it.contains("CREATE VIRTUAL TABLE IF NOT EXISTS `search_index_fts` USING `FTS4`") })
    }
}
