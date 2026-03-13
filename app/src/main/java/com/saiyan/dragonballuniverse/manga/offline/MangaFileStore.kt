package com.saiyan.dragonballuniverse.manga.offline

import android.content.Context
import com.saiyan.dragonballuniverse.manga.MangaArc
import java.io.File

class MangaFileStore(
    private val context: Context,
) {
    fun chapterFolder(
        arc: MangaArc,
        chapterNumber: Int,
    ): File = File(File(context.filesDir, "manga/${arc.name.lowercase()}/$chapterNumber"), "")

    fun pageFile(
        arc: MangaArc,
        chapterNumber: Int,
        pageIndex: Int,
        extension: String = "jpg",
    ): File {
        val folder = chapterFolder(arc, chapterNumber)
        if (!folder.exists()) folder.mkdirs()
        return File(folder, "${pageIndex}.$extension")
    }

    fun hasDownloadedPage(
        arc: MangaArc,
        chapterNumber: Int,
        pageIndex: Int,
    ): Boolean {
        val folder = chapterFolder(arc, chapterNumber)
        if (!folder.exists()) return false
        // We don't know the extension; check common ones.
        val candidates =
            listOf(
                File(folder, "$pageIndex.jpg"),
                File(folder, "$pageIndex.jpeg"),
                File(folder, "$pageIndex.png"),
                File(folder, "$pageIndex.webp"),
            )
        return candidates.any { it.exists() }
    }

    fun getExistingPageFileOrNull(
        arc: MangaArc,
        chapterNumber: Int,
        pageIndex: Int,
    ): File? {
        val folder = chapterFolder(arc, chapterNumber)
        if (!folder.exists()) return null
        val candidates =
            listOf(
                File(folder, "$pageIndex.jpg"),
                File(folder, "$pageIndex.jpeg"),
                File(folder, "$pageIndex.png"),
                File(folder, "$pageIndex.webp"),
            )
        return candidates.firstOrNull { it.exists() }
    }
}
