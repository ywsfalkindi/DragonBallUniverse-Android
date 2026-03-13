package com.saiyan.dragonballuniverse.manga

import com.saiyan.dragonballuniverse.db.MangaDownloadDao
import com.saiyan.dragonballuniverse.db.MangaProgressDao
import com.saiyan.dragonballuniverse.db.UserMangaProgressEntity
import com.saiyan.dragonballuniverse.network.MangaApiService
import com.saiyan.dragonballuniverse.network.MangaRetrofitClient
import com.saiyan.dragonballuniverse.network.PocketBaseClient
import com.saiyan.dragonballuniverse.network.PocketBaseMangaChapterRecord
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class MangaRepositoryImpl(
    // Kept for compatibility with existing DI/wiring, but no longer used for chapter/page listing.
    private val api: MangaApiService,
    private val progressDao: MangaProgressDao,
    private val downloadDao: MangaDownloadDao,
) : MangaRepository {

    // Basic in-memory cache for PocketBase metadata so the app can still show previously fetched
    // chapters if PocketBase is temporarily unreachable.
    private val chaptersCacheByArc: MutableMap<MangaArc, List<PocketBaseMangaChapterRecord>> = mutableMapOf()

    private suspend fun fetchChapterMetasFromPocketBase(arc: MangaArc): List<PocketBaseMangaChapterRecord> {
        // PocketBase filter syntax:
        // https://pocketbase.io/docs/api-records/#listrecords
        // Strings must be double-quoted.
        val filter = """is_published=true && arc="${arc.toApiArc()}""""

        val resp =
            PocketBaseClient.apiService.listMangaChapters(
                filter = filter,
                sort = "+chapter_number",
                page = 1,
                perPage = 200,
            )

        // Extra safety: server-side filter should already handle is_published.
        return resp.items
            .filter { it.isPublished }
            .filter { it.arc.equals(arc.name.lowercase(), ignoreCase = true) }
            .filter { it.chapterNumber > 0 }
    }

    private suspend fun getCachedOrFetchChapterMetas(arc: MangaArc): List<PocketBaseMangaChapterRecord> =
        try {
            val items = fetchChapterMetasFromPocketBase(arc)
            chaptersCacheByArc[arc] = items
            items
        } catch (_: Exception) {
            // Fallback to cache if available
            chaptersCacheByArc[arc].orEmpty()
        }

    private val probeClient: OkHttpClient =
        OkHttpClient.Builder()
            // HEAD requests are sometimes blocked/behave inconsistently behind CDNs.
            // Use GET with Range to avoid downloading full files while remaining widely supported.
            .build()

    override fun getChapters(arc: MangaArc): Flow<List<MangaRepository.MangaChapterInfoWithUserState>> =
        flow {
            val metas = getCachedOrFetchChapterMetas(arc)

            val chapterInfos =
                metas.map { meta ->
                    MangaChapterInfo(
                        arc = arc,
                        chapterNumber = meta.chapterNumber,
                        title = meta.title,
                        // PocketBase doesn't currently provide releaseYear; keep 0 for compatibility.
                        releaseYear = "0",
                        // Keep placeholder; page list is resolved when opening the chapter.
                        pageCount = 0,
                    )
                }

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

        val metaTotalPages =
            // Use PocketBase metadata (cached if PocketBase is down).
            getCachedOrFetchChapterMetas(arc)
                .firstOrNull { it.chapterNumber == chapterNumber }
                ?.totalPages
                ?: 0

        val pageUrls =
            if (metaTotalPages > 0) {
                // Guaranteed path: build deterministically from local catalog count.
                (1..metaTotalPages).map { pageNumber ->
                    val fileName = "${pageNumber.toString().padStart(3, '0')}.webp"
                    "$baseUrl/manga/${arc.toApiArc()}/$chapterFolder/$fileName"
                }
            } else {
                // Fallback: probe sequential pages (001.webp, 002.webp...) until 3 misses in a row.
                val urls = mutableListOf<String>()
                val maxPagesToProbe = 500 // safety cap; adjust if needed
                val maxConsecutiveMisses = 3

                var consecutiveMisses = 0

                for (pageNumber in 1..maxPagesToProbe) {
                    val fileName = "${pageNumber.toString().padStart(3, '0')}.webp"
                    val url = "$baseUrl/manga/${arc.toApiArc()}/$chapterFolder/$fileName"

                    if (remoteFileExists(url)) {
                        urls.add(url)
                        consecutiveMisses = 0
                    } else {
                        consecutiveMisses += 1
                        if (consecutiveMisses >= maxConsecutiveMisses) {
                            break
                        }
                    }
                }
                urls
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
