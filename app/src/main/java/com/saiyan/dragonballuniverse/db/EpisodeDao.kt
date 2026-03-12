package com.saiyan.dragonballuniverse.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EpisodeDao {

    @Query("SELECT * FROM user_episodes WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getEpisode(episodeId: String): UserEpisodeEntity?

    @Query("SELECT isFavorite FROM user_episodes WHERE episodeId = :episodeId LIMIT 1")
    suspend fun isFavorite(episodeId: String): Boolean?

    @Query("SELECT watchProgress FROM user_episodes WHERE episodeId = :episodeId LIMIT 1")
    suspend fun getWatchProgress(episodeId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserEpisodeEntity)

    @Query(
        """
        UPDATE user_episodes 
        SET isFavorite = :isFavorite 
        WHERE episodeId = :episodeId
        """
    )
    suspend fun updateFavorite(episodeId: String, isFavorite: Boolean)

    @Query(
        """
        UPDATE user_episodes 
        SET watchProgress = :watchProgress 
        WHERE episodeId = :episodeId
        """
    )
    suspend fun updateWatchProgress(episodeId: String, watchProgress: Long)
}
