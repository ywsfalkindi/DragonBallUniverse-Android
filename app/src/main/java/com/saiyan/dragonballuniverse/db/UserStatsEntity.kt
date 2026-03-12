package com.saiyan.dragonballuniverse.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val userId: String = "main_user",
    val powerLevel: Long = 5L,
    val senzuBeans: Int = 99,
    val highestStreak: Int = 0,
    val lastPlayedTimestamp: Long = 0L
)
