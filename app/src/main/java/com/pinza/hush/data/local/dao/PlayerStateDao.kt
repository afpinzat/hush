package com.pinza.hush.data.local.dao

import androidx.room.*
import com.pinza.hush.data.local.model.PlayerState
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: PlayerState)

    @Update
    suspend fun updateState(state: PlayerState)

    @Query("SELECT * FROM player_state WHERE id = 1")
    fun getPlayerState(): Flow<PlayerState?>

    @Query("DELETE FROM player_state")
    suspend fun clear()
}