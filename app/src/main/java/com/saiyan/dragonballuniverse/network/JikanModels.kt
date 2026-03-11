package com.saiyan.dragonballuniverse.network

data class JikanResponse(
    val data: List<ApiEpisode>
)

data class ApiEpisode(
    val mal_id: Int,
    val title: String,
    val aired: String?,
    val synopsis: String?
)
