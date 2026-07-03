package com.pinza.hush.data.local.dao

import androidx.room.*
import com.pinza.hush.data.local.model.ScanResult
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ScanResult)

    @Query("SELECT * FROM scan_result ORDER BY scanDate DESC")
    fun getScanHistory(): Flow<List<ScanResult>>

    @Query("SELECT * FROM scan_result ORDER BY scanDate DESC LIMIT 1")
    suspend fun getLastScan(): ScanResult?

    @Query("DELETE FROM scan_result")
    suspend fun clear()
}