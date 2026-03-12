# Implementation Plan

[Overview]  
إضافة قسم “تحديات” (Quiz) داخل تطبيق DragonBallUniverse عبر Jetpack Compose + Room لتقديم تجربة أسئلة تفاعلية بأسلوب “مقياس الكشاف/غرفة الروح والزمن” مع حفظ تقدم اللاعب (مستوى الطاقة، حبات السنزو، الستريك) محلياً.

سيتم دمج وجهة ثالثة في التنقل السفلي (Bottom Bar) داخل `MainActivity.kt` دون حذف أي كود موجود (مشغل الفيديو/المانغا/الأنمي)، وإضافة شاشة Quiz منفصلة منظمة في ملف جديد (مثلاً `QuizScreen.kt`) بحيث تعمل ضمن نفس `Scaffold` الحالي.  
سيتم تحديث قاعدة بيانات Room الحالية (المستخدمة للمفضلة وتقدم المشاهدة) بإضافة كيان جديد `UserStatsEntity` و DAO جديد `UserStatsDao` مع رفع نسخة قاعدة البيانات إلى `version = 2` مع `fallbackToDestructiveMigration()` (كما هو أسلوب المشروع حالياً) لتفادي تعقيد Migration في هذه الإضافة الضخمة.

اللعبة ستكون “جلسة تحدّي” تسحب مجموعة أسئلة عشوائية من بنك أسئلة (نبدأ بـ 10 أسئلة) مع خلط ترتيب الأسئلة وخيارات الإجابة داخل كل سؤال. سيتم بناء منطق لعب واضح: مؤقت لكل سؤال، مكافآت طاقة حسب الصعوبة، خصم حبة سنزو عند الخطأ/انتهاء الوقت، شاشة Game Over عند نفاد السنزو، وشاشة انتصار عند إنهاء كل الأسئلة مع ملخص المكاسب.  
ستُستخدم ألوان الثيم الحالية (`GokuOrange`, `VegetaBlue`, `DarkBackground`) مع نصوص عربية بالكامل، وسيتم الاستفادة من `modifier.bounceClick` الموجود مسبقاً داخل `MainActivity.kt` لتوحيد تجربة اللمس.

[Types]  
إضافة أنواع جديدة (Room Entity/Dao + نماذج بيانات Quiz + حالات UI) لدعم تخزين إحصائيات المستخدم ومحرك الاختبار.

## Room Entities

### `UserStatsEntity`
**المسار:** `app/src/main/java/com/saiyan/dragonballuniverse/db/UserStatsEntity.kt`  
```kotlin
@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val userId: String = "main_user",
    val powerLevel: Long = 5L,
    val senzuBeans: Int = 3,
    val highestStreak: Int = 0,
    val lastPlayedTimestamp: Long = 0L
)
```

**قواعد تحقق/علاقات:**
- `userId` ثابت على `"main_user"` لأن التطبيق حالياً Single-user محلي.
- `powerLevel` لا يقل عن 0 (سيتم فرضه في منطق التحديث: `coerceAtLeast(0)`).
- `senzuBeans` بين 0 و 3 (فرض داخل منطق التحديث: `coerceIn(0, 3)`).
- `lastPlayedTimestamp` يمثل آخر وقت لعب/آخر وقت تم فيه إعادة تعبئة السنزو (سيُستخدم للمقارنة اليومية).

## Room DAO

### `UserStatsDao`
**المسار:** `app/src/main/java/com/saiyan/dragonballuniverse/db/UserStatsDao.kt`  
الدوال المطلوبة (سنعتمد `suspend` لأن المشروع يستخدم `Dispatchers.IO` داخل ViewModel):

- `suspend fun getStats(userId: String = "main_user"): UserStatsEntity?`
- `suspend fun upsert(stats: UserStatsEntity)`
- `suspend fun updatePowerLevel(userId: String = "main_user", powerLevel: Long)`
- `suspend fun updateSenzuBeans(userId: String = "main_user", senzuBeans: Int)`
- `suspend fun updateHighestStreak(userId: String = "main_user", highestStreak: Int)`
- `suspend fun updateLastPlayedTimestamp(userId: String = "main_user", timestamp: Long)`

ملاحظة: بدل “تحديث الـ Streak” الغامضة سنحفظ `highestStreak` (أعلى ستريك) في DB كما طلبت، بينما “الستريك الحالي” سيكون ضمن حالة الجلسة (in-memory) لأنه متعلق بجلسة اللعب الحالية.

## Quiz Models

### `QuizQuestion`
**المسار:** `app/src/main/java/com/saiyan/dragonballuniverse/quiz/QuizQuestion.kt` (أو داخل `QuizScreen.kt` إن رغبت بتجميع بسيط)
```kotlin
data class QuizQuestion(
    val id: Int,
    val text: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val difficulty: String // Easy, Medium, Hard, Insane
)
```

**قيود:**
- `options.size == 4` دائماً.
- `correctAnswerIndex` بين 0..3.
- `difficulty` ستكون String كما طلبت، لكن سنستخدم ثوابت داخلية لتفادي الأخطاء:
  - `const val DIFF_EASY = "Easy"`, ... إلخ.

### `dummyQuestions`
**المسار:** نفس ملف الموديل  
قائمة 10 أسئلة عربية “حقيقية” متدرجة الصعوبة. سيتم في منطق اللعبة:
- خلط ترتيب الأسئلة: `questions.shuffled()`
- خلط الخيارات لكل سؤال مع إعادة حساب `correctAnswerIndex` بعد الخلط.

## UI State Types

### `QuizUiState`
**المسار:** `app/src/main/java/com/saiyan/dragonballuniverse/quiz/QuizUiState.kt` (أو داخل `QuizViewModel.kt`)
```kotlin
sealed interface QuizUiState {
    data object Home : QuizUiState
    data object Playing : QuizUiState
    data class GameOver(val earnedPower: Long) : QuizUiState
    data class Victory(val earnedPower: Long) : QuizUiState
}
```

### `QuizSessionState` (حالة جلسة اللعب)
```kotlin
data class QuizSessionState(
    val questions: List<QuizQuestion>,
    val currentIndex: Int,
    val currentStreak: Int,
    val earnedPower: Long,
    val isAnswered: Boolean
)
```

[Files]  
تحديث ملفات التنقل الحالية + إضافة ملفات Quiz + تحديث Room DB إلى version 2 مع DAO/Entity جديدة.

## ملفات جديدة (New)
1. `app/src/main/java/com/saiyan/dragonballuniverse/db/UserStatsEntity.kt`  
   - كيان Room لإحصائيات اللاعب.

2. `app/src/main/java/com/saiyan/dragonballuniverse/db/UserStatsDao.kt`  
   - واجهة DAO لاسترجاع/تحديث الإحصائيات.

3. `app/src/main/java/com/saiyan/dragonballuniverse/quiz/QuizQuestion.kt`  
   - `QuizQuestion` + `dummyQuestions` + أدوات الخلط (shuffle).

4. `app/src/main/java/com/saiyan/dragonballuniverse/quiz/QuizScreen.kt`  
   - `@Composable fun QuizMainScreen(...)` وجميع واجهات Home/Playing/GameOver/Victory.
   - دالة `getRankName(powerLevel: Long): String`.
   - منطق المؤقت (15 ثانية) باستخدام `LaunchedEffect`.

5. (اختياري لكنه أنظف) `app/src/main/java/com/saiyan/dragonballuniverse/quiz/QuizViewModel.kt`  
   - ViewModel خاص بالـ Quiz لإدارة الحالة + Room IO.
   - إن لم نضفه سنستخدم `rememberCoroutineScope` داخل `QuizMainScreen`، لكن الأفضل ViewModel لتفادي فقدان الحالة عند إعادة التركيب/تغيير التوجيه.

## ملفات معدلة (Modify)
1. `app/src/main/java/com/saiyan/dragonballuniverse/db/UserDatabase.kt`
   - إضافة `UserStatsEntity` إلى `entities`.
   - رفع `version` من 1 إلى 2.
   - إضافة `abstract fun userStatsDao(): UserStatsDao`

2. `app/src/main/java/com/saiyan/dragonballuniverse/MainActivity.kt`
   - تعديل `MainDestination` لإضافة `Quiz`.
   - تعديل `DragonBallBottomBar` لإضافة `NavigationBarItem` ثالث بأيقونة `Icons.Filled.Star` (أو `EmojiEvents` إن لزم) مع Label "تحديات".
   - تعديل `DragonBallHomeContent` لإضافة فرع `MainDestination.Quiz` يعرض `QuizMainScreen(...)`.
   - تمرير `viewModel` أو DB instance/dao إلى `QuizMainScreen`.

3. `app/src/main/java/com/saiyan/dragonballuniverse/MainViewModel.kt` (اختياري)
   - إمّا تركه كما هو وإضافة `QuizViewModel` منفصل.
   - أو توسيعه ليحمل `UserStatsDao`، لكن ذلك سيخلط المسؤوليات. الخطة الافتراضية: `QuizViewModel` منفصل.

## ملفات تُحذف/تُنقل
- لا شيء.

## تحديثات إعدادات/Gradle
- لا يُتوقع إضافة dependencies جديدة: المشروع يحتوي Room + Compose + material-icons-extended بالفعل.
- فقط التأكد من وجود `material-icons-extended` (موجود) لأيقونة `Star`.

[Functions]  
إضافة/تعديل دوال Compose و Room DAO ودوال منطق لعبة لضبط المؤقت، تحديث الإحصائيات، خلط الأسئلة، وإدارة الانتقالات بين شاشات التحدي.

## Room / DB

### `UserDatabase.userStatsDao()`
**Signature**
```kotlin
abstract fun userStatsDao(): UserStatsDao
```
**Behavior**
- يوفّر DAO لإحصائيات اللاعب ضمن نفس قاعدة البيانات.

### `UserStatsDao.getStats`
```kotlin
@Query("SELECT * FROM user_stats WHERE userId = :userId LIMIT 1")
suspend fun getStats(userId: String = "main_user"): UserStatsEntity?
```
- يعيد null إن لم يتم إنشاء صف المستخدم بعد.

### `UserStatsDao.upsert`
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun upsert(stats: UserStatsEntity)
```

### `UserStatsDao.updatePowerLevel / updateSenzuBeans / updateHighestStreak / updateLastPlayedTimestamp`
- Queries `UPDATE ... WHERE userId = :userId`.

**Error handling**
- سيُحاط استدعاء DAO داخل ViewModel بـ try/catch ويعود لقيم افتراضية عند الفشل، على نمط `MainViewModel`.

## Quiz Core

### `getRankName(powerLevel: Long): String`
**Signature**
```kotlin
fun getRankName(powerLevel: Long): String
```
**Behavior**
- Mapping مقترح:
  - < 1_000 => "أرضي"
  - < 10_000 => "مقاتل"
  - < 100_000 => "مقاتل النخبة"
  - < 1_000_000 => "سوبر سايان"
  - < 10_000_000 => "سوبر سايان 2"
  - < 100_000_000 => "سوبر سايان 3"
  - else => "غريزة فائقة"
- تُستخدم في شاشة Home لإعطاء لقب.

### `shuffleQuestion(question: QuizQuestion, random: Random): QuizQuestion`
**Signature**
```kotlin
fun shuffleQuestion(question: QuizQuestion): QuizQuestion
```
**Behavior**
- يخلط `options` ويرجع سؤال جديد مع `correctAnswerIndex` الجديد.
**Key detail**
- نحتاج نقل الإجابة الصحيحة بالاعتماد على قيمة الخيار الصحيح الأصلية:
  - `val correctText = question.options[question.correctAnswerIndex]`
  - `val shuffled = question.options.shuffled(random)`
  - `val newIndex = shuffled.indexOf(correctText)`

### `buildSessionQuestions(allQuestions: List<QuizQuestion>, count: Int = 10): List<QuizQuestion>`
- يختار `count` عشوائياً (أو أقل لو القائمة أقل) ثم يطبق `shuffleQuestion` على كل سؤال.

### `powerRewardForDifficulty(difficulty: String): Long`
**Behavior**
- Easy: 500
- Medium: 1000
- Hard: 2000
- Insane: 5000
- يمكن تعديل الأرقام لاحقاً بسهولة.

## Quiz UI Composables

### `QuizMainScreen(...)`
**Signature (مقترح مع ViewModel)**
```kotlin
@Composable
fun QuizMainScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel
)
```
**Behavior**
- يعرض UI حسب `QuizUiState`:
  - Home: إظهار powerLevel + rank + senzuBeans + زر “ابدأ التحدي”.
  - Playing: السؤال الحالي + مؤقت + أزرار الخيارات.
  - GameOver/Victory: شاشات نهائية مع زر رجوع.
- يستعمل `bounceClick` للزر الرئيسي ولبطاقات/أزرار عند الحاجة.

### `QuizHomeContent(...)`
- يظهر:
  - “مستوى الطاقة” بارز جداً، لون `GokuOrange` وخط عريض.
  - لقب الرتبة عبر `getRankName`.
  - “حبات السنزو: X/3”.
  - زر “ابدأ التحدي”.
- عند الضغط:
  - يتحقق من إعادة تعبئة السنزو اليومية:
    - إن مرّ يوم (أو تغير التاريخ) منذ `lastPlayedTimestamp` => إعادة senzu إلى 3 وتحديث timestamp.
  - إن `senzuBeans == 0` يمنع البدء ويعرض رسالة واضحة (قرار معتمد).
  - يبدأ Session جديدة بأسئلة عشوائية.

### `QuizPlayingContent(...)`
- Timer:
  - مدة 15 ثانية لكل سؤال.
  - `LinearProgressIndicator` في الأعلى ينقص تدريجياً.
  - `LaunchedEffect(currentQuestionId)` يطلق مؤقت:
    - تحديث progress كل 100ms أو 250ms.
    - عند الوصول للصفر => اعتباره خطأ وتنفيذ نفس مسار “إجابة خاطئة”.
- Question card:
  - Card بخلفية `Color(0xFF1E1E1E)` ونص عربي واضح.
- Answer buttons:
  - 4 أزرار، كل واحد يمثل خيار.
- Haptics:
  - عند الصح: `HapticFeedbackType.TextHandleMove` (أو `HapticFeedbackType.LongPress` كاهتزاز واضح) — لكن بما أنك طلبت “عادي للصح” سنستخدم `HapticFeedbackType.TextHandleMove` أو `KeyboardTap` إن توفر.
  - عند الخطأ: `HapticFeedbackType.LongPress`.
- Game logic:
  - صح:
    - زيادة `earnedPower` حسب الصعوبة.
    - تحديث `powerLevel` في DB فوراً (IO).
    - زيادة `currentStreak`، وتحديث `highestStreak` في DB إن تجاوز.
    - الانتقال للسؤال التالي.
  - خطأ/انتهاء الوقت:
    - خصم `senzuBeans` بحد أدنى 0 مع تحديث DB.
    - تصفير `currentStreak`.
    - إن وصلت 0 => `GameOver`.
    - وإلا الانتقال للسؤال التالي مباشرة (قرار معتمد).
  - نهاية الأسئلة:
    - `Victory` مع `earnedPower`.

### `GameOverScreen(...)` و `VictoryScreen(...)`
- تعرض نص عربي:
  - Game Over: “انتهت حبات السنزو!” + “الطاقة المكتسبة: …”
  - Victory: “أحسنت! أنهيت التحدي” + “الطاقة المكتسبة: …”
- زر “العودة” لإرجاع الحالة إلى Home.

## Navigation integration

### Update `MainDestination`
**Current**
```kotlin
private enum class MainDestination { Anime, Manga }
```
**New**
```kotlin
private enum class MainDestination { Anime, Manga, Quiz }
```

### Update `DragonBallBottomBar`
- إضافة `NavigationBarItem` ثالث:
  - `icon = Icons.Filled.Star`
  - `label = "تحديات"`
  - ألوان مختارة مثل الموجود.

### Update `DragonBallHomeContent`
- فرع جديد:
```kotlin
MainDestination.Quiz -> { QuizMainScreen(viewModel = quizViewModel, modifier = modifier) }
```
- إذا تم إنشاء `QuizViewModel`:
  - في `DragonBallScaffold` أو `MainActivity` سننشئه بـ `viewModels()` أو `viewModel()` داخل Compose (مع Factory لأن يحتاج `Application` لقاعدة البيانات).

[Changes]  
التنفيذ سيتم بإضافة طبقة Quiz مستقلة قدر الإمكان (ملفات جديدة) ثم دمجها في التنقل الحالي، مع تحديث Room DB وتغذية الـ UI من ViewModel لضمان سلامة الاستدعاءات المعلّقة (suspend) وعدم تنفيذ IO داخل Composable مباشرة.

1. **تحديث Room Database**
   1. إنشاء `UserStatsEntity` و `UserStatsDao`.
   2. تعديل `UserDatabase.kt`:
      - `entities = [UserEpisodeEntity::class, UserStatsEntity::class]`
      - `version = 2`
      - إضافة `abstract fun userStatsDao(): UserStatsDao`
      - إبقاء `fallbackToDestructiveMigration()` كما هو (هذا سيعيد إنشاء DB عند ترقية النسخة).
   3. إضافة “تهيئة افتراضية” للسجل:
      - داخل `QuizViewModel.init` (أو عند فتح شاشة Quiz لأول مرة) ننفذ:
        - `if (dao.getStats() == null) dao.upsert(UserStatsEntity())`

2. **بناء بنك أسئلة + خلط**
   1. إنشاء `QuizQuestion` + `dummyQuestions` (10 أسئلة عربية).
   2. إضافة helpers:
      - `buildSessionQuestions` لاختيار/خلط.
      - `shuffleQuestion` لخلط الخيارات مع إعادة ضبط الإجابة.

3. **إضافة ViewModel للـ Quiz (مفضل)**
   1. `QuizViewModel(application: Application)` مشابه لـ `MainViewModel`:
      - الحصول على `UserStatsDao` من `UserDatabase.getInstance(...).userStatsDao()`.
      - `StateFlow<UserStatsEntity>` للإحصائيات (أو `MutableStateFlow`).
      - `StateFlow<QuizUiState>` لحالة الشاشة.
      - `MutableStateFlow<QuizSessionState?>` للجلسة.
   2. دوال:
      - `fun loadOrCreateStats()`
      - `fun startGame()`
      - `fun answer(selectedIndex: Int)`
      - `fun onTimeExpired()`
      - `fun backToHome()`
      - `fun maybeRefillDailySenzu()`:
        - مقارنة `lastPlayedTimestamp` مع تاريخ اليوم (LocalDate باستخدام `java.time` مع minSdk 24 متاح).
        - إذا تغير اليوم (تاريخ مختلف) => ضبط senzu=3 وتحديث timestamp.
   3. جميع تحديثات DB ضمن `viewModelScope.launch(Dispatchers.IO)`.

4. **تصميم واجهات Compose**
   1. إنشاء `QuizMainScreen` تقرأ flows عبر `collectAsStateWithLifecycle`.
   2. بناء Home UI حسب المتطلبات.
   3. بناء Playing UI:
      - `LinearProgressIndicator` مع progress state.
      - `LaunchedEffect(questionId)` لتشغيل مؤقت 15 ثانية.
      - عند الضغط على خيار:
        - تعطيل الأزرار فوراً لمنع الضغط المتعدد.
        - إرسال haptic حسب الصح/الخطأ.
        - تحديث الحالة والانتقال.
   4. بناء GameOver/Victory.

5. **دمج التنقل السفلي**
   1. تحديث enum `MainDestination`.
   2. تحديث `DragonBallBottomBar`.
   3. تحديث `DragonBallHomeContent` لإضافة الفرع الثالث.
   4. ضمان عدم التداخل مع منطق الفيديو/المانغا:
      - Quiz لا يلمس متغيرات `selectedVideoUrl`/`selectedMangaChapterNumber`.

6. **تحقق بصري ووظيفي**
   1. Build/Run.
   2. التحقق من:
      - الانتقال للـ Quiz من BottomBar.
      - إنشاء stats افتراضي.
      - مؤقت يعمل ويعتبر انتهاء الوقت “خطأ”.
      - تحديث الطاقة وحبات السنزو في DB (يظهر في Home بعد العودة).
      - GameOver عند وصول السنزو 0.
      - Victory عند نهاية الأسئلة.
      - خلط الأسئلة والخيارات بين كل جلسة.

[Tests]  
الاختبارات ستكون مزيج Unit + (اختياري) Instrumentation، مع تركيز على منطق الخلط والجوائز وتحديث الإحصائيات لأن Compose UI صعب اختباره بالكامل دون إعدادات إضافية.

- **Unit Tests (JVM)**
  - `shuffleQuestion`:
    - الحفاظ على نفس 4 خيارات لكن بتبديل ترتيب.
    - التأكد أن `correctAnswerIndex` يشير لنفس النص الصحيح بعد الخلط.
  - `powerRewardForDifficulty`:
    - التحقق من قيم الجوائز لكل صعوبة.
  - `getRankName`:
    - حدود (boundary values) عند 999/1000، 9999/10000، … إلخ.
  - `buildSessionQuestions`:
    - يعيد عدد صحيح من الأسئلة ويختلف ترتيبها بين تشغيلين (باستخدام seed للتحكم).

- **Integration / Instrumentation (اختياري)**
  - اختبار Room:
    - إنشاء DB in-memory وكتابة/قراءة `UserStatsEntity`.
    - التحقق من update queries.
  - (اختياري) Compose UI Test:
    - فتح Quiz screen والتحقق من ظهور “ابدأ التحدي”.

- **Edge Cases**
  - `senzuBeans` = 0: يمنع بدء التحدي أو يبدأ ويؤدي فوراً لـ GameOver (سنختار المنع + رسالة).
  - بنك أسئلة أقل من المطلوب: اختيار المتاح فقط.
  - إعادة تعبئة السنزو اليومية: تغير اليوم/المنطقة الزمنية.
  - تدوير الشاشة: بوجود ViewModel تبقى الحالة؛ مع remember فقط قد تفقد.
