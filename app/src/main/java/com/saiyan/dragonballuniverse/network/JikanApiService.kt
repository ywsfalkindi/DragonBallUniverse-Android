package com.saiyan.dragonballuniverse.network

import retrofit2.http.GET

interface JikanApiService {
    @GET("anime/813/episodes")
    suspend fun getDragonBallEpisodes(): JikanResponse
}
