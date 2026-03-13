package com.saiyan.dragonballuniverse.network

import com.saiyan.dragonballuniverse.BuildConfig
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
    /**
     * PocketBase base URL.
     *
     * IMPORTANT (physical device):
     * - 10.0.2.2 only works from the Android emulator.
     * - For a real phone, set this to your computer's LAN IP, e.g.:
     *   http://192.168.1.10:8090/api/
     *
     * Recommended: override via BuildConfig so it's easy to change per-machine without code edits:
     * - add `POCKETBASE_BASE_URL` buildConfigField in app/build.gradle.kts
     */
    private const val BASE_URL = BuildConfig.POCKETBASE_BASE_URL

    fun baseUrl(): String = BASE_URL

    private val okHttp: OkHttpClient =
        if (BuildConfig.DEBUG) {
            // DEBUG-ONLY: allow self-signed/local TLS setups and relaxed networking, matching MangaRetrofitClient.
            // If you're using plain http:// this won't be used for TLS, but still keeps behavior consistent.
            UnsafeOkHttp.create()
        } else {
            OkHttpClient.Builder().build()
        }

    private val retrofit: Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val apiService: PocketBaseService = retrofit.create(PocketBaseService::class.java)
}
