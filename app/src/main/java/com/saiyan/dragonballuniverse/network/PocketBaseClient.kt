package com.saiyan.dragonballuniverse.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * PocketBase API client.
 *
 * Emulator -> host machine uses 10.0.2.2
 * PocketBase base should end with '/' for Retrofit.
 *
 * Example endpoints:
 * - GET /api/collections/manga_chapters/records?filter=...
 * - GET /api/collections/quiz_questions/records?filter=...
 */
object PocketBaseClient {
    private const val BASE_URL = "http://10.0.2.2:8090/api/"

    fun baseUrl(): String = BASE_URL

    private val okHttp: OkHttpClient =
        OkHttpClient.Builder()
            .build()

    private val retrofit: Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val apiService: PocketBaseService = retrofit.create(PocketBaseService::class.java)
}
