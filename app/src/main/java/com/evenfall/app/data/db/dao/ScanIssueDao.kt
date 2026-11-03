package com.evenfall.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.evenfall.app.data.db.entity.ScanIssueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanIssueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(issue: ScanIssueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(issues: List<ScanIssueEntity>)

    @Query("SELECT * FROM scan_issues ORDER BY timestamp DESC")
    fun getAllScanIssues(): Flow<List<ScanIssueEntity>>

    @Query("SELECT * FROM scan_issues ORDER BY timestamp DESC")
    suspend fun getAllScanIssuesSync(): List<ScanIssueEntity>

    @Query("SELECT COUNT(*) FROM scan_issues")
    fun getIssueCount(): Flow<Int>

    @Query("DELETE FROM scan_issues")
    suspend fun clearAll()
}
