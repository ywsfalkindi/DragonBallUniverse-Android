package com.saiyan.dragonballuniverse.network

import com.google.gson.annotations.SerializedName

/**
 * PocketBase "List Records" response shape.
 * https://pocketbase.io/docs/api-records/#listrecords
 */
data class PocketBaseListResponse<T>(
    @SerializedName("page")
    val page: Int,
    @SerializedName("perPage")
    val perPage: Int,
    @SerializedName("totalPages")
    val totalPages: Int,
    @SerializedName("totalItems")
    val totalItems: Int,
    @SerializedName("items")
    val items: List<T>,
)

data class PocketBaseMangaChapterRecord(
    @SerializedName("id")
    val id: String,
    @SerializedName("arc")
    val arc: String,
    @SerializedName("chapter_number")
    val chapterNumber: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("total_pages")
    val totalPages: Int,
    @SerializedName("is_published")
    val isPublished: Boolean,
)
