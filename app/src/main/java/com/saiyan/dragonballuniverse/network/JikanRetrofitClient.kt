package com.saiyan.dragonballuniverse.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object JikanRetrofitClient {
    private val retrofit: Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.jikan.moe/v4/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val apiService: JikanApiService = retrofit.create(JikanApiService::class.java)
}
