package com.saiyan.dragonballuniverse.quiz

import kotlin.random.Random

data class QuizQuestion(
    val id: Int,
    val text: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val difficulty: String // Easy, Medium, Hard, Insane
)

const val DIFF_EASY = "Easy"
const val DIFF_MEDIUM = "Medium"
const val DIFF_HARD = "Hard"
const val DIFF_INSANE = "Insane"

val dummyQuestions: List<QuizQuestion> =
    listOf(
        QuizQuestion(
            id = 1,
            text = "ما اسم جد غوكو الذي ربّاه في الجبال؟",
            options = listOf("سون غوهان", "باردوك", "ناميك", "كايو ساما"),
            correctAnswerIndex = 0,
            difficulty = DIFF_EASY
        ),
        QuizQuestion(
            id = 2,
            text = "ما اسم الهجوم الشهير الذي يستخدمه غوكو بإرسال موجة طاقة؟",
            options = listOf("كاميهاميها", "جاليك غان", "فاينال فلاش", "ديستركتو ديسك"),
            correctAnswerIndex = 0,
            difficulty = DIFF_EASY
        ),
        QuizQuestion(
            id = 3,
            text = "من هو أمير السايان الذي أصبح لاحقاً حليفاً لغوكو؟",
            options = listOf("فيجيتا", "فريزا", "سيل", "راديتز"),
            correctAnswerIndex = 0,
            difficulty = DIFF_EASY
        ),
        QuizQuestion(
            id = 4,
            text = "ما اسم كوكب بيكولو الأصلي؟",
            options = listOf("ناميك", "فيجيتا", "الأرض", "يا درات"),
            correctAnswerIndex = 0,
            difficulty = DIFF_MEDIUM
        ),
        QuizQuestion(
            id = 5,
            text = "أي تحول شهير يظهر لأول مرة ضد فريزا على كوكب ناميك؟",
            options = listOf("سوبر سايان", "سوبر سايان 2", "سوبر سايان 3", "غريزة فائقة"),
            correctAnswerIndex = 0,
            difficulty = DIFF_MEDIUM
        ),
        QuizQuestion(
            id = 6,
            text = "من هو مبتكر تقنية \"كيّوكن\"؟",
            options = listOf("كايو الشمالي", "بيروس", "ويتس", "مستر روشي"),
            correctAnswerIndex = 0,
            difficulty = DIFF_MEDIUM
        ),
        QuizQuestion(
            id = 7,
            text = "في أرك سيل، ما اسم البطولة التي أُقيمت؟",
            options = listOf("ألعاب سيل", "بطولة الكون", "مهرجان الكواكب", "حلبة الشياطين"),
            correctAnswerIndex = 0,
            difficulty = DIFF_HARD
        ),
        QuizQuestion(
            id = 8,
            text = "ما اسم اندماج غوتين و ترانكس؟",
            options = listOf("غوتينكس", "فيجيتو", "غوغيتا", "ترونتين"),
            correctAnswerIndex = 0,
            difficulty = DIFF_HARD
        ),
        QuizQuestion(
            id = 9,
            text = "من هو الإله المدمّر في الكون السابع؟",
            options = listOf("بيروس", "زينو", "كايوشين", "شامبا"),
            correctAnswerIndex = 0,
            difficulty = DIFF_HARD
        ),
        QuizQuestion(
            id = 10,
            text = "ما اسم التحول الذي يعتمد على التحكم بالغريزة والقتال بلا تفكير؟",
            options = listOf("الغريزة الفائقة", "سوبر سايان بلو", "سوبر سايان غود", "ألترا إيجو"),
            correctAnswerIndex = 0,
            difficulty = DIFF_INSANE
        )
    )

fun shuffleQuestion(
    question: QuizQuestion,
    random: Random = Random.Default
): QuizQuestion {
    val correctText = question.options[question.correctAnswerIndex]
    val shuffled = question.options.shuffled(random)
    val newIndex = shuffled.indexOf(correctText).coerceAtLeast(0)
    return question.copy(options = shuffled, correctAnswerIndex = newIndex)
}

fun buildSessionQuestions(
    allQuestions: List<QuizQuestion>,
    count: Int = 10,
    random: Random = Random.Default
): List<QuizQuestion> {
    val picked = if (allQuestions.size <= count) allQuestions else allQuestions.shuffled(random).take(count)
    return picked.shuffled(random).map { shuffleQuestion(it, random) }
}

fun powerRewardForDifficulty(difficulty: String): Long =
    when (difficulty) {
        DIFF_EASY -> 500L
        DIFF_MEDIUM -> 1000L
        DIFF_HARD -> 2000L
        DIFF_INSANE -> 5000L
        else -> 500L
    }
