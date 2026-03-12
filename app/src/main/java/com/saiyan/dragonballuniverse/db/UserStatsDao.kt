package com.saiyan.dragonballuniverse.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserStatsDao {

    @Query("SELECT * FROM user_stats WHERE userId = :userId LIMIT 1")
    suspend fun getStats(userId: String = "main_user"): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: UserStatsEntity)

    @Query("UPDATE user_stats SET powerLevel = :powerLevel WHERE userId = :userId")
    suspend fun updatePowerLevel(
        userId: String = "main_user",
        powerLevel: Long
    )

    @Query("UPDATE user_stats SET senzuBeans = :senzuBeans WHERE userId = :userId")
    suspend fun updateSenzuBeans(
        userId: String = "main_user",
        senzuBeans: Int
    )

    @Query("UPDATE user_stats SET highestStreak = :highestStreak WHERE userId = :userId")
    suspend fun updateHighestStreak(
        userId: String = "main_user",
        highestStreak: Int
    )

    @Query("UPDATE user_stats SET lastPlayedTimestamp = :timestamp WHERE userId = :userId")
    suspend fun updateLastPlayedTimestamp(
        userId: String = "main_user",
        timestamp: Long
    )
}
