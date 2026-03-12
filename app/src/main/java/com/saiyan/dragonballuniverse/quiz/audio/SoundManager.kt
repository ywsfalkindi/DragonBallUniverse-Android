package com.saiyan.dragonballuniverse.quiz.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.RawRes

sealed class QuizSfx {
    data object Correct : QuizSfx()
    data object Wrong : QuizSfx()
}

interface SoundManager {
    fun play(sfx: QuizSfx)
    fun release()
}

class SoundPoolSoundManager(
    context: Context,
    @RawRes correctRes: Int,
    @RawRes wrongRes: Int,
) : SoundManager {

    private val soundPool: SoundPool
    private val correctSoundId: Int
    private val wrongSoundId: Int

    @Volatile
    private var correctLoaded: Boolean = false

    @Volatile
    private var wrongLoaded: Boolean = false

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            when (sampleId) {
                correctSoundId -> correctLoaded = true
                wrongSoundId -> wrongLoaded = true
            }
        }

        // Load right away (constructor should be called once per playing screen).
        correctSoundId = soundPool.load(context, correctRes, 1)
        wrongSoundId = soundPool.load(context, wrongRes, 1)
    }

    override fun play(sfx: QuizSfx) {
        try {
            when (sfx) {
                QuizSfx.Correct -> if (correctLoaded) soundPool.play(correctSoundId, 1f, 1f, 1, 0, 1f)
                QuizSfx.Wrong -> if (wrongLoaded) soundPool.play(wrongSoundId, 1f, 1f, 1, 0, 1f)
            }
        } catch (_: Throwable) {
            // Never crash quiz gameplay due to audio issues.
        }
    }

    override fun release() {
        try {
            soundPool.release()
        } catch (_: Throwable) {
        }
    }
}
