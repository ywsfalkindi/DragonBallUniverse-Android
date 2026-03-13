package com.saiyan.dragonballuniverse.manga

enum class MangaArc {
    CLASSIC,
    Z,
    SUPER,
}

data class MangaChapterInfo(
    val arc: MangaArc,
    val chapterNumber: Int,
    val title: String,
    val releaseYear: String? = null,
    val pageCount: Int,
)

data class MangaPage(
    val pageIndex: Int,
    val imageUrl: String,
)

data class MangaChapterPages(
    val arc: MangaArc,
    val chapterNumber: Int,
    val pages: List<MangaPage>,
)
