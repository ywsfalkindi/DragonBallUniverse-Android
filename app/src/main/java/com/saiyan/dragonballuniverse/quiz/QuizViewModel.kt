package com.saiyan.dragonballuniverse.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saiyan.dragonballuniverse.db.UserDatabase
import com.saiyan.dragonballuniverse.db.UserStatsDao
import com.saiyan.dragonballuniverse.db.UserStatsEntity
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
    data object Playing : QuizUiState
    data class GameOver(val earnedPower: Long) : QuizUiState
    data class Victory(val earnedPower: Long) : QuizUiState
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

    private suspend fun loadOrCreateStats() {
        val loaded =
            withContext(Dispatchers.IO) {
                dao.getStats() ?: UserStatsEntity().also { dao.upsert(it) }
            }
        _stats.value = loaded
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
            loadOrCreateStats()
            maybeRefillDailySenzuInternal()

            val currentStats = _stats.value
            // 0 Senzu: Disable button (UI should handle); double-safety here:
            if (currentStats.senzuBeans <= 0) {
                _uiState.value = QuizUiState.Home
                return@launch
            }

            val questions = buildSessionQuestions(dummyQuestions, count = 10)

            _session.value =
                QuizSessionState(
                    questions = questions,
                    currentIndex = 0,
                    currentStreak = 0,
                    earnedPower = 0L,
                    isAnswered = false
                )
            _uiState.value = QuizUiState.Playing
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
