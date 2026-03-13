package com.saiyan.dragonballuniverse.network

data class MangaChaptersResponseDto(
    val arc: String,
    val chapters: List<MangaChapterDto>,
)

data class MangaChapterDto(
    val chapterNumber: Int,
    val title: String,
    val releaseYear: String?,
    val pages: List<String>,
)
