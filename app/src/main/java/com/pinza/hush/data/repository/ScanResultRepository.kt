package com.pinza.hush.data.repository

import com.pinza.hush.data.local.dao.ScanResultDao
import com.pinza.hush.data.local.model.ScanResult
import com.pinza.hush.domain.repository.IScanResultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanResultRepository @Inject constructor(
    private val dao: ScanResultDao
) : IScanResultRepository {

    override fun history(): Flow<List<ScanResult>> =
        dao.getScanHistory()

    override suspend fun lastScan() =
        dao.getLastScan()

    override suspend fun insert(scan: ScanResult) =
        dao.insert(scan)

    override suspend fun clear() =
        dao.clear()
}