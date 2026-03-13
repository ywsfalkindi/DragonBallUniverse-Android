package com.saiyan.dragonballuniverse.manga

import com.saiyan.dragonballuniverse.db.MangaDownloadDao
import com.saiyan.dragonballuniverse.db.MangaProgressDao
import com.saiyan.dragonballuniverse.db.UserMangaProgressEntity
import com.saiyan.dragonballuniverse.network.MangaApiService
import com.saiyan.dragonballuniverse.network.MangaRetrofitClient
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class MangaRepositoryImpl(
    // Kept for compatibility with existing DI/wiring, but no longer used for chapter/page listing (Option B).
    private val api: MangaApiService,
    private val progressDao: MangaProgressDao,
    private val downloadDao: MangaDownloadDao,
) : MangaRepository {

    private val probeClient: OkHttpClient =
        OkHttpClient.Builder()
            // HEAD requests are sometimes blocked/behave inconsistently behind CDNs.
            // Use GET with Range to avoid downloading full files while remaining widely supported.
            .build()

    override fun getChapters(arc: MangaArc): Flow<List<MangaRepository.MangaChapterInfoWithUserState>> =
        flow {
            // Local catalog (Option B): no JSON endpoint.
            val metas = MangaLocalCatalog.chaptersForArc(arc)

            val chapterInfos =
                metas.map { meta ->
                    MangaChapterInfo(
                        arc = arc,
                        chapterNumber = meta.chapterNumber,
                        title = meta.title,
                        releaseYear = meta.releaseYear,
                        // Unknown until opened; keep a placeholder.
                        pageCount = 0,
                    )
                }.filter { it.chapterNumber > 0 }

            emit(chapterInfos)
        }.map { chapters ->
            val progressByChapter =
                progressDao.getAllByArc(arc.toDbArc())
                    .associateBy { it.chapterNumber }

            val downloadsByChapter =
                downloadDao.getAllByArc(arc.toDbArc())
                    .associateBy { it.chapterNumber }

            chapters.map { info ->
                val progress = progressByChapter[info.chapterNumber]
                val download = downloadsByChapter[info.chapterNumber]
                MangaRepository.MangaChapterInfoWithUserState(
                    info = info,
                    lastReadPageIndex = progress?.lastReadPageIndex ?: 0,
                    isCompleted = progress?.isCompleted ?: false,
                    isDownloaded = (download?.status == "completed"),
                )
            }
        }

    override suspend fun getChapterPages(
        arc: MangaArc,
        chapterNumber: Int,
    ): MangaChapterPages {
        require(chapterNumber > 0) { "chapterNumber must be > 0" }

        // Cloudflare R2 static structure:
        // {baseUrl}/manga/{arc}/{chapter}/{page}.webp
        // Example: manga/super/chapter-001/001.webp
        val chapterFolder = "chapter-${chapterNumber.toString().padStart(3, '0')}"

        val baseUrl = MangaRetrofitClient.baseUrl().trimEnd('/')

        // Probe sequential pages (001.webp, 002.webp...) until first 404.
        // This allows “images-only” R2 hosting.
        val pageUrls = mutableListOf<String>()
        val maxPagesToProbe = 500 // safety cap; adjust if needed

        for (pageNumber in 1..maxPagesToProbe) {
            val fileName = "${pageNumber.toString().padStart(3, '0')}.webp"
            val url = "$baseUrl/manga/${arc.toApiArc()}/$chapterFolder/$fileName"

            if (remoteFileExists(url)) {
                pageUrls.add(url)
            } else {
                break
            }
        }

        require(pageUrls.isNotEmpty()) { "Chapter pages are empty (no images found on R2)" }

        val pages =
            pageUrls.mapIndexed { idx, url ->
                MangaPage(
                    pageIndex = idx,
                    imageUrl = url,
                )
            }

        return MangaChapterPages(
            arc = arc,
            chapterNumber = chapterNumber,
            pages = pages,
        )
    }

    override suspend fun updateProgress(
        arc: MangaArc,
        chapterNumber: Int,
        lastReadPageIndex: Int,
        isCompleted: Boolean,
    ) {
        val entity =
            UserMangaProgressEntity(
                arc = arc.toDbArc(),
                chapterNumber = chapterNumber,
                lastReadPageIndex = lastReadPageIndex.coerceAtLeast(0),
                isCompleted = isCompleted,
                updatedAtEpochMs = System.currentTimeMillis(),
            )

        progressDao.upsertProgress(entity)
    }

    private fun MangaArc.toApiArc(): String =
        when (this) {
            MangaArc.CLASSIC -> "classic"
            MangaArc.Z -> "z"
            MangaArc.SUPER -> "super"
        }

    private fun MangaArc.toDbArc(): String = name.lowercase()

    private fun remoteFileExists(url: String): Boolean {
        val req =
            Request.Builder()
                .url(url)
                // More reliable than HEAD on some Cloudflare configurations.
                // Range avoids fetching the entire image.
                .get()
                .header("Range", "bytes=0-0")
                .build()

        return try {
            probeClient.newCall(req).execute().use { resp ->
                // 200 OK or 206 Partial Content both mean the object exists.
                resp.code == 200 || resp.code == 206
            }
        } catch (_: Exception) {
            false
        }
    }
}
