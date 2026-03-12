package com.saiyan.dragonballuniverse.quiz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.saiyan.dragonballuniverse.quiz.DIFF_EASY
import com.saiyan.dragonballuniverse.quiz.DIFF_HARD
import com.saiyan.dragonballuniverse.quiz.DIFF_INSANE
import com.saiyan.dragonballuniverse.quiz.DIFF_MEDIUM
import kotlin.math.max
import kotlin.math.min

enum class QuizDifficultyUiTier {
    EASY,
    MEDIUM,
    HARD,
    INSANE,
    UNKNOWN,
}

data class QuizBackgroundStyle(
    val colors: List<Color>,
    val accent: Color,
    val overlayAlpha: Float,
) {
    init {
        require(colors.isNotEmpty()) { "QuizBackgroundStyle.colors must not be empty" }
    }

    val overlayAlphaClamped: Float = max(0f, min(1f, overlayAlpha))
}

fun difficultyToUiTier(difficulty: String): QuizDifficultyUiTier {
    return when (difficulty) {
        DIFF_EASY -> QuizDifficultyUiTier.EASY
        DIFF_MEDIUM -> QuizDifficultyUiTier.MEDIUM
        DIFF_HARD -> QuizDifficultyUiTier.HARD
        DIFF_INSANE -> QuizDifficultyUiTier.INSANE
        else -> QuizDifficultyUiTier.UNKNOWN
    }
}

fun backgroundStyleFor(tier: QuizDifficultyUiTier): QuizBackgroundStyle {
    return when (tier) {
        QuizDifficultyUiTier.EASY -> QuizBackgroundStyle(
            colors = listOf(Color(0xFF0C3B2E), Color(0xFF1FA87A)),
            accent = Color(0xFF2CE3A8),
            overlayAlpha = 0.10f,
        )

        QuizDifficultyUiTier.MEDIUM -> QuizBackgroundStyle(
            colors = listOf(Color(0xFF0B1C3A), Color(0xFF3B2BA6)),
            accent = Color(0xFF6A5CFF),
            overlayAlpha = 0.12f,
        )

        QuizDifficultyUiTier.HARD -> QuizBackgroundStyle(
            colors = listOf(Color(0xFF3A1B0B), Color(0xFFB54A1A)),
            accent = Color(0xFFFF8A3D),
            overlayAlpha = 0.14f,
        )

        QuizDifficultyUiTier.INSANE -> QuizBackgroundStyle(
            colors = listOf(Color(0xFF0A0012), Color(0xFF3B0030), Color(0xFF8A0014)),
            accent = Color(0xFFFF2D55),
            overlayAlpha = 0.18f,
        )

        QuizDifficultyUiTier.UNKNOWN -> QuizBackgroundStyle(
            colors = listOf(Color(0xFF101010), Color(0xFF2B2B2B)),
            accent = Color(0xFF9E9E9E),
            overlayAlpha = 0.12f,
        )
    }
}

@Composable
fun QuizDifficultyBackground(
    tier: QuizDifficultyUiTier,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val style = backgroundStyleFor(tier)

    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(style.colors),
        ),
    ) {
        if (style.overlayAlphaClamped > 0f) {
            Box(
                modifier = Modifier.background(Color.Black.copy(alpha = style.overlayAlphaClamped)),
            )
        }
        content()
    }
}
