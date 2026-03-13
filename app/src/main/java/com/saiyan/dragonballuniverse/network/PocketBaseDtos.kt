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

data class PocketBaseQuizQuestionRecord(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("question")
    val question: String? = null,
    @SerializedName("answers")
    val answers: List<String>? = null,
    @SerializedName("correct_answer_index")
    val correctAnswerIndex: Int? = null,
    @SerializedName("difficulty")
    val difficulty: String? = null,
    @SerializedName("is_published")
    val isPublished: Boolean? = null,
)

data class PocketBaseUserStatsRecord(
    @SerializedName("id")
    val id: String,
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("power_level")
    val powerLevel: Long,
    @SerializedName("senzu_beans")
    val senzuBeans: Int,
    @SerializedName("highest_streak")
    val highestStreak: Int,
    @SerializedName("last_played_timestamp")
    val lastPlayedTimestamp: Long,
)

data class PocketBaseUpsertUserStatsBody(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("power_level")
    val powerLevel: Long,
    @SerializedName("senzu_beans")
    val senzuBeans: Int,
    @SerializedName("highest_streak")
    val highestStreak: Int,
    @SerializedName("last_played_timestamp")
    val lastPlayedTimestamp: Long,
)
