package com.saiyan.dragonballuniverse.manga.offline

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.saiyan.dragonballuniverse.db.MangaDownloadDao
import com.saiyan.dragonballuniverse.db.UserMangaDownloadEntity
import com.saiyan.dragonballuniverse.manga.MangaArc
import com.saiyan.dragonballuniverse.manga.MangaPage
import com.saiyan.dragonballuniverse.network.PocketBaseClient
import com.saiyan.dragonballuniverse.network.PocketBaseUpsertUserMangaDownloadBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

data class DownloadProgress(
    val totalPages: Int,
    val downloadedPages: Int,
    val bytesDownloaded: Long,
    val status: String,
    val errorMessage: String? = null,
)

class MangaOfflineManager(
    private val context: Context,
    private val fileStore: MangaFileStore,
    private val downloadDao: MangaDownloadDao,
) {
    private val client: OkHttpClient = OkHttpClient()

    private fun deviceId(): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_device"

    fun downloadChapter(
        arc: MangaArc,
        chapterNumber: Int,
        pages: List<MangaPage>,
    ): Flow<DownloadProgress> =
        flow {
            val total = pages.size
            var bytes = 0L
            var done = 0

            upsertStatus(
                arc = arc,
                chapterNumber = chapterNumber,
                status = "downloading",
                totalPages = total,
                downloadedPages = 0,
                bytesDownloaded = 0L,
                localFolder = fileStore.chapterFolder(arc, chapterNumber).absolutePath,
                errorMessage = null,
            )

            emit(
                DownloadProgress(
                    totalPages = total,
                    downloadedPages = 0,
                    bytesDownloaded = 0L,
                    status = "downloading",
                ),
            )

            try {
                for (page in pages) {
                    val already = fileStore.getExistingPageFileOrNull(arc, chapterNumber, page.pageIndex)
                    if (already != null) {
                        done++
                        continue
                    }

                    val extension = guessExtension(page.imageUrl)
                    val outFile = fileStore.pageFile(arc, chapterNumber, page.pageIndex, extension)

                    val downloadedBytes =
                        downloadToFile(
                            url = page.imageUrl,
                            outFile = outFile,
                        )

                    bytes += downloadedBytes
                    done++

                    upsertStatus(
                        arc = arc,
                        chapterNumber = chapterNumber,
                        status = "downloading",
                        totalPages = total,
                        downloadedPages = done,
                        bytesDownloaded = bytes,
                        localFolder = fileStore.chapterFolder(arc, chapterNumber).absolutePath,
                        errorMessage = null,
                    )

                    emit(
                        DownloadProgress(
                            totalPages = total,
                            downloadedPages = done,
                            bytesDownloaded = bytes,
                            status = "downloading",
                        ),
                    )
                }

                upsertStatus(
                    arc = arc,
                    chapterNumber = chapterNumber,
                    status = "completed",
                    totalPages = total,
                    downloadedPages = total,
                    bytesDownloaded = bytes,
                    localFolder = fileStore.chapterFolder(arc, chapterNumber).absolutePath,
                    errorMessage = null,
                )

                emit(
                    DownloadProgress(
                        totalPages = total,
                        downloadedPages = total,
                        bytesDownloaded = bytes,
                        status = "completed",
                    ),
                )
            } catch (e: Exception) {
                upsertStatus(
                    arc = arc,
                    chapterNumber = chapterNumber,
                    status = "failed",
                    totalPages = total,
                    downloadedPages = done,
                    bytesDownloaded = bytes,
                    localFolder = fileStore.chapterFolder(arc, chapterNumber).absolutePath,
                    errorMessage = e.message ?: "download failed",
                )

                emit(
                    DownloadProgress(
                        totalPages = total,
                        downloadedPages = done,
                        bytesDownloaded = bytes,
                        status = "failed",
                        errorMessage = e.message,
                    ),
                )
            }
        }

    private suspend fun upsertStatus(
        arc: MangaArc,
        chapterNumber: Int,
        status: String,
        totalPages: Int,
        downloadedPages: Int,
        bytesDownloaded: Long,
        localFolder: String?,
        errorMessage: String?,
    ) {
        // 1) Always update local Room cache (offline-first).
        withContext(Dispatchers.IO) {
            downloadDao.upsert(
                UserMangaDownloadEntity(
                    arc = arc.name.lowercase(),
                    chapterNumber = chapterNumber,
                    status = status,
                    totalPages = totalPages,
                    downloadedPages = downloadedPages,
                    bytesDownloaded = bytesDownloaded,
                    localFolder = localFolder,
                    errorMessage = errorMessage,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }

        // 2) Best-effort sync to PocketBase `user_manga_downloads`.
        // Collection is public (no auth) and keyed by (device_id, arc, chapter_number).
        try {
            val did = deviceId()
            val filter = """device_id="${did}" && arc="${arc.name.lowercase()}" && chapter_number=${chapterNumber}"""

            val existing =
                PocketBaseClient.apiService
                    .listUserMangaDownloads(
                        filter = filter,
                        page = 1,
                        perPage = 1,
                    )
                    .items
                    .firstOrNull()

            val body =
                PocketBaseUpsertUserMangaDownloadBody(
                    arc = arc.name.lowercase(),
                    chapterNumber = chapterNumber,
                    status = status,
                    totalPages = totalPages,
                    downloadedPages = downloadedPages,
                    localFolder = localFolder,
                    deviceId = did,
                )

            if (existing == null) {
                PocketBaseClient.apiService.createUserMangaDownload(body)
                Log.d("PB_DEBUG", "MangaOffline: created user_manga_downloads for $did ${arc.name}#$chapterNumber")
            } else {
                PocketBaseClient.apiService.updateUserMangaDownload(existing.id, body)
                Log.d("PB_DEBUG", "MangaOffline: updated user_manga_downloads id=${existing.id} for $did ${arc.name}#$chapterNumber")
            }
        } catch (e: Exception) {
            // Don't fail the download flow if PB is unreachable.
            Log.e("PB_ERROR", "MangaOffline: failed to sync user_manga_downloads", e)
        }
    }

    private suspend fun downloadToFile(
        url: String,
        outFile: File,
    ): Long =
        withContext(Dispatchers.IO) {
            val req =
                Request.Builder()
                    .url(url)
                    .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val body = resp.body ?: throw IOException("Empty body")
                outFile.outputStream().use { out ->
                    val bytes = body.bytes()
                    out.write(bytes)
                    bytes.size.toLong()
                }
            }
        }

    private fun guessExtension(url: String): String {
        val u = url.lowercase()
        return when {
            u.contains(".webp") -> "webp"
            u.contains(".png") -> "png"
            u.contains(".jpeg") -> "jpeg"
            u.contains(".jpg") -> "jpg"
            else -> "jpg"
        }
    }
}
