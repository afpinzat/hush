package com.pinza.hush.domain.repository

import com.pinza.hush.data.local.model.ScanResult
import kotlinx.coroutines.flow.Flow

interface IScanResultRepository {

    fun history(): Flow<List<ScanResult>>

    suspend fun lastScan(): ScanResult?

    suspend fun insert(scan: ScanResult)

    suspend fun clear()
}