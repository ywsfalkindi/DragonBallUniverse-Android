package com.saiyan.dragonballuniverse.quiz

import android.app.Application
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saiyan.dragonballuniverse.db.UserDatabase
import com.saiyan.dragonballuniverse.db.UserStatsDao
import com.saiyan.dragonballuniverse.db.UserStatsEntity
import com.saiyan.dragonballuniverse.network.PocketBaseClient
import com.saiyan.dragonballuniverse.network.PocketBaseUpsertUserStatsBody
import com.saiyan.dragonballuniverse.network.PocketBaseUserStatsRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface QuizUiState {
    data object Home : QuizUiState
    data object Loading : QuizUiState
    data object Playing : QuizUiState
    data class GameOver(val earnedPower: Long) : QuizUiState
    data class Victory(val earnedPower: Long) : QuizUiState
    data class Error(val message: String, val details: String) : QuizUiState
}

data class QuizSessionState(
    val questions: List<QuizQuestion>,
    val currentIndex: Int,
    val currentStreak: Int,
    val earnedPower: Long,
    val isAnswered: Boolean
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val isLastQuestion: Boolean get() = currentIndex >= questions.lastIndex
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: UserStatsDao =
        UserDatabase.getInstance(application.applicationContext).userStatsDao()

    private val _uiState = MutableStateFlow<QuizUiState>(QuizUiState.Home)
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _stats = MutableStateFlow(UserStatsEntity())
    val stats: StateFlow<UserStatsEntity> = _stats.asStateFlow()

    private val _session = MutableStateFlow<QuizSessionState?>(null)
    val session: StateFlow<QuizSessionState?> = _session.asStateFlow()

    init {
        viewModelScope.launch {
            loadOrCreateStats()
        }
    }

    private fun deviceId(): String =
        Settings.Secure.getString(getApplication<Application>().contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_device"

    private suspend fun loadOrCreateStats() {
        val loaded =
            withContext(Dispatchers.IO) {
                dao.getStats() ?: UserStatsEntity().also { dao.upsert(it) }
            }
        _stats.value = loaded
    }

    /**
     * Sync stats from PocketBase (public user_stats collection).
     * If record doesn't exist yet, we keep local defaults and will create it on first update.
     */
    private suspend fun syncStatsFromPocketBase() {
        val did = deviceId()
        val filter = """device_id="${did}""""

        val resp =
            PocketBaseClient.apiService.listUserStats(
                filter = filter,
                page = 1,
                perPage = 1,
            )

        val record: PocketBaseUserStatsRecord? = resp.items.firstOrNull()
        if (record != null) {
            val merged =
                _stats.value.copy(
                    powerLevel = record.powerLevel,
                    senzuBeans = record.senzuBeans,
                    highestStreak = record.highestStreak,
                    lastPlayedTimestamp = record.lastPlayedTimestamp,
                )

            withContext(Dispatchers.IO) { dao.upsert(merged) }
            _stats.value = merged
        }
    }

    /**
     * Push current local stats to PocketBase (create if missing, otherwise patch).
     *
     * Collection is public (no auth) and keyed by device_id.
     */
    private suspend fun pushStatsToPocketBase() {
        val did = deviceId()
        val filter = """device_id="${did}""""

        val existing =
            PocketBaseClient.apiService
                .listUserStats(
                    filter = filter,
                    page = 1,
                    perPage = 1,
                )
                .items
                .firstOrNull()

        val body =
            PocketBaseUpsertUserStatsBody(
                deviceId = did,
                powerLevel = _stats.value.powerLevel,
                senzuBeans = _stats.value.senzuBeans,
                highestStreak = _stats.value.highestStreak,
                lastPlayedTimestamp = _stats.value.lastPlayedTimestamp,
            )

        if (existing == null) {
            PocketBaseClient.apiService.createUserStats(body)
            Log.d("PB_DEBUG", "Quiz: created user_stats for device_id=$did")
        } else {
            PocketBaseClient.apiService.updateUserStats(existing.id, body)
            Log.d("PB_DEBUG", "Quiz: updated user_stats id=${existing.id} for device_id=$did")
        }
    }

    fun backToHome() {
        _session.value = null
        _uiState.value = QuizUiState.Home
        viewModelScope.launch {
            loadOrCreateStats()
        }
    }

    /**
     * Refill rule: LocalDate (daily).
     * If today != lastPlayedDate => refill senzu to 3 and update lastPlayedTimestamp to now.
     */
    private suspend fun maybeRefillDailySenzuInternal() {
        val current = _stats.value
        val lastTs = current.lastPlayedTimestamp

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val lastDate =
            if (lastTs <= 0L) null
            else Instant.ofEpochMilli(lastTs).atZone(zone).toLocalDate()

        val shouldRefill = lastDate == null || lastDate != today
        if (!shouldRefill) return

        val now = System.currentTimeMillis()
        val updated =
            current.copy(
                senzuBeans = 3,
                lastPlayedTimestamp = now
            )

        withContext(Dispatchers.IO) {
            dao.upsert(updated)
        }
        _stats.value = updated
    }

    fun startGame() {
        viewModelScope.launch {
            _uiState.value = QuizUiState.Loading
            try {
                loadOrCreateStats()
                // Pull latest stats from PB (if exists)
                syncStatsFromPocketBase()

                maybeRefillDailySenzuInternal()

                val currentStats = _stats.value
                // 0 Senzu: Disable button (UI should handle); double-safety here:
                if (currentStats.senzuBeans <= 0) {
                    _uiState.value = QuizUiState.Home
                    return@launch
                }

                val pbQuestions =
                    PocketBaseClient.apiService
                        .listQuizQuestions(
                            page = 1,
                            perPage = 200,
                        )
                        .items

                Log.d("PB_DEBUG", "Raw JSON from PB: $pbQuestions")

                // Relaxed mapping: do NOT drop records aggressively; provide defaults.
                val mapped =
                    pbQuestions.mapIndexed { idx, r ->
                        // Treat missing is_published as published=true by default.
                        val published = r.isPublished ?: true

                        val questionText = r.questionText?.trim().takeUnless { it.isNullOrBlank() } ?: "Unknown Question"
                        val options =
                            r.answers
                                ?.filterNotNull()
                                ?.map { it.trim() }
                                ?.filter { it.isNotBlank() }
                                ?: emptyList()

                        val normalizedDifficulty =
                            when (r.difficulty?.trim()) {
                                DIFF_EASY, DIFF_MEDIUM, DIFF_HARD, DIFF_INSANE -> r.difficulty!!.trim()
                                else -> DIFF_EASY
                            }

                        val safeIndex =
                            if (options.isEmpty()) 0
                            else (r.correctAnswerIndex ?: 0).coerceIn(0, options.lastIndex)

                        QuizQuestion(
                            id = idx + 1, // local int id required by current UI
                            text = questionText,
                            options = options,
                            correctAnswerIndex = safeIndex,
                            difficulty = normalizedDifficulty,
                        )
                    }.filter { q ->
                        // Only filter out explicitly unpublished questions.
                        // Also require at least 2 options to be playable.
                        val src = pbQuestions.getOrNull(q.id - 1)
                        val published = src?.isPublished ?: true
                        published && q.options.size >= 2
                    }

                val sourceQuestions = if (mapped.isNotEmpty()) mapped else dummyQuestions
                val questions = buildSessionQuestions(sourceQuestions, count = 10)

                _session.value =
                    QuizSessionState(
                        questions = questions,
                        currentIndex = 0,
                        currentStreak = 0,
                        earnedPower = 0L,
                        isAnswered = false,
                    )
                _uiState.value = QuizUiState.Playing
                Log.d("PB_DEBUG", "Quiz: fetched ${pbQuestions.size} questions from PocketBase")
            } catch (e: Exception) {
                Log.e("PB_ERROR", "Quiz: failed to start game", e)
                val details = "${e::class.java.simpleName}: ${e.message}"
                _uiState.value = QuizUiState.Error(message = "Failed to load quiz", details = details)
            }
        }
    }

    fun answer(selectedIndex: Int) {
        val s = _session.value ?: return
        val q = s.currentQuestion ?: return
        if (s.isAnswered) return

        val isCorrect = selectedIndex == q.correctAnswerIndex
        if (isCorrect) {
            handleCorrectAnswer(s, q)
        } else {
            handleWrongOrExpired(s)
        }
    }

    fun onTimeExpired() {
        val s = _session.value ?: return
        if (s.isAnswered) return
        handleWrongOrExpired(s)
    }

    /**
     * On error: Next question.
     */
    private fun handleCorrectAnswer(
        session: QuizSessionState,
        question: QuizQuestion
    ) {
        viewModelScope.launch {
            val reward = powerRewardForDifficulty(question.difficulty)

            val newEarned = session.earnedPower + reward
            val newStreak = session.currentStreak + 1

            val currentStats = _stats.value
            val updatedPower = (currentStats.powerLevel + reward).coerceAtLeast(0L)
            val updatedHighest = maxOf(currentStats.highestStreak, newStreak)

            val updatedStats =
                currentStats.copy(
                    powerLevel = updatedPower,
                    highestStreak = updatedHighest
                )

            withContext(Dispatchers.IO) {
                dao.upsert(updatedStats)
            }
            _stats.value = updatedStats

            try {
                pushStatsToPocketBase()
            } catch (e: Exception) {
                Log.e("PB_ERROR", "Quiz: failed to persist stats (correct answer)", e)
            }

            goNextOrFinish(
                session = session.copy(
                    currentStreak = newStreak,
                    earnedPower = newEarned,
                    isAnswered = true
                )
            )
        }
    }

    /**
     * On error OR time expired:
     * - subtract one senzu (min 0)
     * - streak resets to 0
     * - if senzu reaches 0 => GameOver
     * - else => next question
     */
    private fun handleWrongOrExpired(session: QuizSessionState) {
        viewModelScope.launch {
            val currentStats = _stats.value
            val updatedSenzu = (currentStats.senzuBeans - 1).coerceIn(0, 3)

            val updatedStats =
                currentStats.copy(
                    senzuBeans = updatedSenzu
                )

            withContext(Dispatchers.IO) {
                dao.upsert(updatedStats)
            }
            _stats.value = updatedStats

            try {
                pushStatsToPocketBase()
            } catch (e: Exception) {
                Log.e("PB_ERROR", "Quiz: failed to persist stats (wrong/expired)", e)
            }

            val newSession =
                session.copy(
                    currentStreak = 0,
                    isAnswered = true
                )

            if (updatedSenzu <= 0) {
                _session.value = newSession
                _uiState.value = QuizUiState.GameOver(earnedPower = newSession.earnedPower)
                return@launch
            }

            goNextOrFinish(newSession)
        }
    }

    private fun goNextOrFinish(session: QuizSessionState) {
        val isLast = session.isLastQuestion
        if (isLast) {
            _session.value = session
            _uiState.value = QuizUiState.Victory(earnedPower = session.earnedPower)
            return
        }

        _session.value =
            session.copy(
                currentIndex = session.currentIndex + 1,
                isAnswered = false
            )
        _uiState.value = QuizUiState.Playing
    }
}
