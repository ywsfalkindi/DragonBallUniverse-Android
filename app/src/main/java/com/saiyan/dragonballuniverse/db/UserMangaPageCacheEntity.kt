package com.saiyan.dragonballuniverse.db

import androidx.room.Entity

@Entity(
    tableName = "user_manga_page_cache",
    primaryKeys = ["arc", "chapterNumber", "pageIndex"],
)
data class UserMangaPageCacheEntity(
    val arc: String,
    val chapterNumber: Int,
    val pageIndex: Int,
    val imageUrl: String,
    val localFilePath: String?,
    val cachedAtEpochMs: Long = 0L,
)
