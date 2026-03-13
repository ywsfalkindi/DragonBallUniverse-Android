package com.saiyan.dragonballuniverse.db

import androidx.room.Entity

@Entity(
    tableName = "user_manga_downloads",
    primaryKeys = ["arc", "chapterNumber"],
)
data class UserMangaDownloadEntity(
    val arc: String,
    val chapterNumber: Int,
    val status: String,
    val totalPages: Int,
    val downloadedPages: Int,
    val bytesDownloaded: Long = 0L,
    val localFolder: String?,
    val errorMessage: String? = null,
    val updatedAtEpochMs: Long = 0L,
)
