package com.saiyan.dragonballuniverse.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MangaPageCacheDao {
    @Upsert
    suspend fun upsertAll(entities: List<UserMangaPageCacheEntity>)

    @Upsert
    suspend fun upsert(entity: UserMangaPageCacheEntity)

    @Query("SELECT * FROM user_manga_page_cache WHERE arc = :arc AND chapterNumber = :chapterNumber ORDER BY pageIndex ASC")
    suspend fun getChapterPages(
        arc: String,
        chapterNumber: Int,
    ): List<UserMangaPageCacheEntity>

    @Query("SELECT * FROM user_manga_page_cache WHERE arc = :arc AND chapterNumber = :chapterNumber AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getPage(
        arc: String,
        chapterNumber: Int,
        pageIndex: Int,
    ): UserMangaPageCacheEntity?
}
