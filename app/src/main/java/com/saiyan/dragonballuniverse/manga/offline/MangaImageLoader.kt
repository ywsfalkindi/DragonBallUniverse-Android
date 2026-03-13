package com.saiyan.dragonballuniverse.manga.offline

import com.saiyan.dragonballuniverse.manga.MangaArc
import java.io.File

class MangaImageLoader(
    private val fileStore: MangaFileStore,
) {
    /**
     * Returns a Coil model:
     * - File if it exists locally
     * - otherwise the remote URL String
     */
    fun resolvePageModel(
        arc: MangaArc,
        chapterNumber: Int,
        pageIndex: Int,
        remoteUrl: String,
    ): Any {
        val local: File? = fileStore.getExistingPageFileOrNull(arc, chapterNumber, pageIndex)
        return local ?: remoteUrl
    }
}
