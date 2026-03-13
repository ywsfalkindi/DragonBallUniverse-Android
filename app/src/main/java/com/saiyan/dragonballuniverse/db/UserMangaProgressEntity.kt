package com.saiyan.dragonballuniverse.db

import androidx.room.Entity

@Entity(
    tableName = "user_manga_progress",
    primaryKeys = ["arc", "chapterNumber"],
)
data class UserMangaProgressEntity(
    val arc: String,
    val chapterNumber: Int,
    val lastReadPageIndex: Int = 0,
    val isCompleted: Boolean = false,
    val updatedAtEpochMs: Long = 0L,
)
