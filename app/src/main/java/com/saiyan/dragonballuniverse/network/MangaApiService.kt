package com.saiyan.dragonballuniverse.network

import retrofit2.http.GET
import retrofit2.http.Path

interface MangaApiService {
    @GET("manga/dragonball/{arc}/chapters")
    suspend fun getChapters(
        @Path("arc") arc: String,
    ): MangaChaptersResponseDto

    @GET("manga/dragonball/{arc}/chapters/{chapterNumber}")
    suspend fun getChapter(
        @Path("arc") arc: String,
        @Path("chapterNumber") chapterNumber: Int,
    ): MangaChapterDto
}
