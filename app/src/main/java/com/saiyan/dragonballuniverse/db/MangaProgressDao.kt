package com.saiyan.dragonballuniverse.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MangaProgressDao {
    @Upsert
    suspend fun upsertProgress(entity: UserMangaProgressEntity)

    @Query("SELECT * FROM user_manga_progress WHERE arc = :arc AND chapterNumber = :chapterNumber LIMIT 1")
    suspend fun getProgress(
        arc: String,
        chapterNumber: Int,
    ): UserMangaProgressEntity?

    @Query("SELECT * FROM user_manga_progress WHERE arc = :arc")
    suspend fun getAllByArc(arc: String): List<UserMangaProgressEntity>
}
