package com.pinza.hush.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_result")
data class ScanResult(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val scanDate: Long,

    val totalFound: Int,

    val status: String
)