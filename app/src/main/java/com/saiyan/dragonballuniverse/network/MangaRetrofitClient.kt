package com.saiyan.dragonballuniverse.network

import com.saiyan.dragonballuniverse.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MangaRetrofitClient {
    /**
     * TODO: set this to your Manga JSON API baseUrl.
     *
     * Example:
     * - https://example.com/api/
     *
     * Notes:
     * - Must end with '/' for Retrofit.
     */
    private const val BASE_URL = "https://pub-4c1d83bd6469447fa34dac35eb86824b.r2.dev/"

    fun baseUrl(): String = BASE_URL

    private val okHttp: OkHttpClient =
        if (BuildConfig.DEBUG) {
            // DEBUG-ONLY: bypass TLS validation (fixes CertPathValidatorException on some devices).
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

    val apiService: MangaApiService = retrofit.create(MangaApiService::class.java)
}
