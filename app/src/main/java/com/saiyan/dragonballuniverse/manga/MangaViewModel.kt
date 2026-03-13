package com.saiyan.dragonballuniverse.manga

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saiyan.dragonballuniverse.db.UserDatabase
import com.saiyan.dragonballuniverse.manga.offline.MangaFileStore
import com.saiyan.dragonballuniverse.manga.offline.MangaOfflineManager
import com.saiyan.dragonballuniverse.network.MangaRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface MangaHomeUiState {
    data object Loading : MangaHomeUiState

    data class Success(
        val arc: MangaArc,
        val chapters: List<MangaRepository.MangaChapterInfoWithUserState>,
    ) : MangaHomeUiState

    data class Error(
        val message: String,
        val details: String,
    ) : MangaHomeUiState
}

sealed interface MangaReaderUiState {
    data object Idle : MangaReaderUiState
    data object Loading : MangaReaderUiState

    data class Success(
        val chapter: MangaChapterPages,
        val initialPageIndex: Int,
    ) : MangaReaderUiState

    data class Error(
        val message: String,
    ) : MangaReaderUiState
}

class MangaViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val applicationContext = application.applicationContext

    private val db by lazy { UserDatabase.getInstance(applicationContext) }

    private val repository: MangaRepository by lazy {
        MangaRepositoryImpl(
            api = MangaRetrofitClient.apiService,
            progressDao = db.mangaProgressDao(),
            downloadDao = db.mangaDownloadDao(),
        )
    }

    private val offlineManager: MangaOfflineManager by lazy {
        MangaOfflineManager(
            context = applicationContext,
            fileStore = MangaFileStore(applicationContext),
            downloadDao = db.mangaDownloadDao(),
        )
    }

    private val _homeUiState = MutableStateFlow<MangaHomeUiState>(MangaHomeUiState.Loading)
    val homeUiState: StateFlow<MangaHomeUiState> = _homeUiState.asStateFlow()

    private val _readerUiState = MutableStateFlow<MangaReaderUiState>(MangaReaderUiState.Idle)
    val readerUiState: StateFlow<MangaReaderUiState> = _readerUiState.asStateFlow()

    private var currentArc: MangaArc = MangaArc.CLASSIC

    init {
        loadChapters(MangaArc.CLASSIC)
    }

    fun loadChapters(arc: MangaArc) {
        currentArc = arc
        _homeUiState.value = MangaHomeUiState.Loading

        viewModelScope.launch {
            try {
                repository.getChapters(arc).collect { list ->
                    _homeUiState.value = MangaHomeUiState.Success(arc = arc, chapters = list)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val details =
                    buildString {
                        append(e::class.java.simpleName)
                        e.message?.let { append(": ").append(it) }
                    }
                _homeUiState.value =
                    MangaHomeUiState.Error(
                        message = e.message ?: "Unknown error",
                        details = details,
                    )
            }
        }
    }

    fun openChapter(
        arc: MangaArc,
        chapterNumber: Int,
    ) {
        _readerUiState.value = MangaReaderUiState.Loading

        viewModelScope.launch {
            try {
                val chapter =
                    withContext(Dispatchers.IO) {
                        repository.getChapterPages(arc, chapterNumber)
                    }

                val progress =
                    withContext(Dispatchers.IO) {
                        db.mangaProgressDao().getProgress(arc.name.lowercase(), chapterNumber)
                    }

                val initial = progress?.lastReadPageIndex ?: 0

                _readerUiState.value =
                    MangaReaderUiState.Success(
                        chapter = chapter,
                        initialPageIndex = initial.coerceIn(0, (chapter.pages.size - 1).coerceAtLeast(0)),
                    )
            } catch (e: Exception) {
                e.printStackTrace()
                _readerUiState.value = MangaReaderUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun saveProgress(
        arc: MangaArc,
        chapterNumber: Int,
        lastReadPageIndex: Int,
        isCompleted: Boolean,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateProgress(
                    arc = arc,
                    chapterNumber = chapterNumber,
                    lastReadPageIndex = lastReadPageIndex,
                    isCompleted = isCompleted,
                )
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    fun downloadChapter(
        arc: MangaArc,
        chapterNumber: Int,
        pages: List<MangaPage>,
        onProgress: (status: String, downloadedPages: Int, totalPages: Int) -> Unit = { _, _, _ -> },
    ) {
        viewModelScope.launch {
            offlineManager
                .downloadChapter(
                    arc = arc,
                    chapterNumber = chapterNumber,
                    pages = pages,
                )
                .collect { p ->
                    onProgress(p.status, p.downloadedPages, p.totalPages)
                }
        }
    }

    fun getCurrentArc(): MangaArc = currentArc
}
