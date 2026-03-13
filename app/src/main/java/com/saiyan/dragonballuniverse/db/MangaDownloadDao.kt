package com.saiyan.dragonballuniverse.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MangaDownloadDao {
    @Upsert
    suspend fun upsert(entity: UserMangaDownloadEntity)

    @Query("SELECT * FROM user_manga_downloads WHERE arc = :arc AND chapterNumber = :chapterNumber LIMIT 1")
    suspend fun get(
        arc: String,
        chapterNumber: Int,
    ): UserMangaDownloadEntity?

    @Query("SELECT * FROM user_manga_downloads WHERE arc = :arc")
    suspend fun getAllByArc(arc: String): List<UserMangaDownloadEntity>
}
