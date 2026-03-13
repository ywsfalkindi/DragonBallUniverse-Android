package com.saiyan.dragonballuniverse.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * PocketBase public Records API.
 *
 * We use:
 * GET /api/collections/{collection}/records
 *
 * Common query params:
 * - filter: e.g. is_published=true && arc="super"
 * - sort: e.g. chapter_number
 * - perPage/page: pagination
 */
interface PocketBaseService {
    @GET("collections/manga_chapters/records")
    suspend fun listMangaChapters(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null,
        @Query("perPage") perPage: Int? = null,
    ): PocketBaseListResponse<PocketBaseMangaChapterRecord>
}
