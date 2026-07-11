package com.pinza.hush.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pinza.hush.data.local.model.PlayerState

@Dao
interface PlayerStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: PlayerState)

    @Query("SELECT * FROM player_state WHERE id = 1")
    suspend fun getPlayerState(): PlayerState?
}