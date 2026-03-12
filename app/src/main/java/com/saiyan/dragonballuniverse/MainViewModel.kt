package com.saiyan.dragonballuniverse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saiyan.dragonballuniverse.db.EpisodeDao
import com.saiyan.dragonballuniverse.db.UserDatabase
import com.saiyan.dragonballuniverse.db.UserEpisodeEntity
import com.saiyan.dragonballuniverse.network.ApiEpisode
import com.saiyan.dragonballuniverse.network.JikanRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState {
    data object Loading : UiState
    data class Success(
        val episodes: List<Episode>
    ) : UiState

    data class Error(
        val message: String
    ) : UiState
}

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dao: EpisodeDao by lazy {
        UserDatabase.getInstance(application.applicationContext).episodeDao()
    }

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        fetchEpisodes()
    }

    fun fetchEpisodes() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                val response = JikanRetrofitClient.apiService.getDragonBallEpisodes()

                val uiEpisodes =
                    withContext(Dispatchers.Default) {
                        mapApiEpisodesToUiEpisodes(response.data)
                    }

                _uiState.value = UiState.Success(uiEpisodes)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun mapApiEpisodesToUiEpisodes(
        apiEpisodes: List<ApiEpisode>
    ): List<Episode> =
        apiEpisodes.mapIndexed { index, apiEpisode ->
            Episode(
                number = index + 1,
                title = apiEpisode.title,
                duration = "24 دقيقة",
                imageUrl = DEFAULT_DBZ_COVER_URL,
                progress = ((index % 10) + 1) / 10f,
                id = (index + 1).toString()
            )
        }

    fun toggleFavorite(episodeId: String, makeFavorite: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getEpisode(episodeId)
            val updated =
                if (existing == null) {
                    UserEpisodeEntity(
                        episodeId = episodeId,
                        isFavorite = makeFavorite,
                        watchProgress = 0L
                    )
                } else {
                    existing.copy(isFavorite = makeFavorite)
                }
            dao.upsert(updated)
        }
    }

    fun saveWatchProgress(episodeId: String, progressMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getEpisode(episodeId)
            val updated =
                if (existing == null) {
                    UserEpisodeEntity(
                        episodeId = episodeId,
                        isFavorite = false,
                        watchProgress = progressMs
                    )
                } else {
                    existing.copy(watchProgress = progressMs)
                }
            dao.upsert(updated)
        }
    }

    fun isFavoriteFlow(
        episodeId: String
    ): StateFlow<Boolean> {
        val flow = MutableStateFlow(false)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                flow.value = dao.isFavorite(episodeId) ?: false
            } catch (_: Exception) {
                flow.value = false
            }
        }

        return flow.asStateFlow()
    }

    fun watchProgressMsFlow(
        episodeId: String
    ): StateFlow<Long?> {
        val flow = MutableStateFlow<Long?>(null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                flow.value = dao.getWatchProgress(episodeId)
            } catch (_: Exception) {
                flow.value = null
            }
        }

        return flow.asStateFlow()
    }

    fun refreshEpisodeDbState(episodeId: String) {
        // Optional hook if you want to force refresh flows later without turning DAO into Flow queries.
        // Intentionally left no-op for now.
    }
}
