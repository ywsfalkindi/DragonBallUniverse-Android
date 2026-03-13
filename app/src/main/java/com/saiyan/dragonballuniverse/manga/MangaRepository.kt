package com.saiyan.dragonballuniverse.manga

import kotlinx.coroutines.flow.Flow

interface MangaRepository {
    data class MangaChapterInfoWithUserState(
        val info: MangaChapterInfo,
        val lastReadPageIndex: Int,
        val isCompleted: Boolean,
        val isDownloaded: Boolean,
    )

    fun getChapters(arc: MangaArc): Flow<List<MangaChapterInfoWithUserState>>

    suspend fun getChapterPages(
        arc: MangaArc,
        chapterNumber: Int,
    ): MangaChapterPages

    suspend fun updateProgress(
        arc: MangaArc,
        chapterNumber: Int,
        lastReadPageIndex: Int,
        isCompleted: Boolean,
    )
}
