package com.saiyan.dragonballuniverse.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_episodes")
data class UserEpisodeEntity(
    @PrimaryKey val episodeId: String,
    val isFavorite: Boolean = false,
    val watchProgress: Long = 0L,
)
