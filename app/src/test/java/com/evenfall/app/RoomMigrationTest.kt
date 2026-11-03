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
}
