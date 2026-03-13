# Implementation Plan

[Overview]
رفع جودة قسم المانجا ليصبح قارئ صفحات حقيقي سريع وسلس مع دعم أوفلاين كامل وحفظ تقدم القراءة محليًا.

حاليًا قسم المانجا في المشروع عبارة عن شاشة تفاصيل بسيطة (عنوان + وصف + قائمة فصول) مع قارئ تجريبي يعرض أيقونة ثابتة كصفحات (5 صفحات) داخل `HorizontalPager` مع تكبير/تحريك. هذا لا يحقق هدف “قارئ مانجا حقيقي” ولا يدعم: جلب صفحات حقيقية، إدارة أجزاء (Classic/Z/Super)، حفظ آخر صفحة، تعليم الفصول كمكتملة، تنزيل فصل بالكامل، أو كاش ذكي لاستكمال القراءة بدون إنترنت.

هذه الخطة تعيد تصميم القسم داخليًا بشكل تدريجي ومتوافق مع المشروع الحالي (Jetpack Compose + Room + Retrofit + Coil/Media3)، مع إبقاء التطبيق “دراغون بول فقط” لتفادي تعقيد دعم سلاسل متعددة. سنضيف طبقة بيانات للمانجا (Models/Network/Repository/DB) ونبني قارئ صفحات متقدم بميزات: RTL، حفظ تقدم مستمر، قائمة صفحات/فهرس، وضع ليلي، تحكم بالسطوع (اختياري إن أمكن)، إيماءات تكبير/سحب محسّنة، وتحميل صفحات مسبقًا (prefetch) لتجربة سلسة.

مصدر البيانات حسب توجيهك: API بسيط أو روابط مباشرة (Direct URLs) عبر JSON يربط رقم الفصل بروابط صور الصفحات. لا توجد متطلبات أمان معقدة الآن، الأولوية للأداء والسلاسة.

---

[Types]
سنضيف أنواع بيانات جديدة للمانجا (نماذج API/نماذج UI/كيانات قاعدة البيانات) مع قواعد تحقق بسيطة لضمان الاستقرار.

## Domain/UI Models (Kotlin)

```kotlin
enum class MangaArc { CLASSIC, Z, SUPER }

data class MangaChapterInfo(
  val arc: MangaArc,
  val chapterNumber: Int,          // رقم الفصل داخل القوس/الجزء
  val title: String,
  val releaseYear: String? = null,
  val pageCount: Int,
)

data class MangaPage(
  val pageIndex: Int,              // يبدأ من 0
  val imageUrl: String,            // https only (نقوم بتسوية http->https)
)

data class MangaChapterPages(
  val arc: MangaArc,
  val chapterNumber: Int,
  val pages: List<MangaPage>
)
```

### Validation Rules
- `chapterNumber > 0`
- `pageIndex >= 0` و `pages` مرتبة تصاعديًا.
- `imageUrl` غير فارغ، ونقوم بتطبيق `resolveImageUrl()` أو مكافئ خاص بالمانجا لمنع `http://` عند الإمكان.
- `pageCount == pages.size` (إذا كانت القيمة موجودة في الميتاداتا).

## Network DTOs (Retrofit/Gson)

نفترض JSON بسيط:
- Endpoint 1: ميتاداتا الفصول (لكل Arc)
- Endpoint 2: صفحات فصل محدد

```kotlin
data class MangaChaptersResponseDto(
  val arc: String,                 // "classic" | "z" | "super"
  val chapters: List<MangaChapterDto>
)

data class MangaChapterDto(
  val chapterNumber: Int,
  val title: String,
  val releaseYear: String?,
  val pages: List<String>          // روابط مباشرة لصور الصفحات
)
```

### Mapping
- `arc` -> `MangaArc` (تحويل آمن مع fallback).
- `pages` -> `List<MangaPage>` عبر `mapIndexed`.

## Room Entities (داخل user_db الحالي)

### 1) تقدم القراءة
```kotlin
@Entity(tableName = "user_manga_progress", primaryKeys = ["arc", "chapterNumber"])
data class UserMangaProgressEntity(
  val arc: String,                 // نخزن نصيًا لتجنب مشاكل Room مع enum بدون converters
  val chapterNumber: Int,
  val lastReadPageIndex: Int = 0,
  val isCompleted: Boolean = false,
  val updatedAtEpochMs: Long = 0L
)
```

### 2) تنزيلات الأوفلاين (الفصل كحزمة)
```kotlin
@Entity(tableName = "user_manga_downloads", primaryKeys = ["arc", "chapterNumber"])
data class UserMangaDownloadEntity(
  val arc: String,
  val chapterNumber: Int,
  val status: String,              // "queued" | "downloading" | "completed" | "failed"
  val totalPages: Int,
  val downloadedPages: Int,
  val bytesDownloaded: Long = 0L,
  val localFolder: String?,        // مسار مجلد التخزين (داخل app storage)
  val errorMessage: String? = null,
  val updatedAtEpochMs: Long = 0L
)
```

### 3) فهرس الصفحات المحلية (اختياري لكنه مفيد للكاش القابل للاستعلام)
```kotlin
@Entity(tableName = "user_manga_page_cache", primaryKeys = ["arc", "chapterNumber", "pageIndex"])
data class UserMangaPageCacheEntity(
  val arc: String,
  val chapterNumber: Int,
  val pageIndex: Int,
  val imageUrl: String,
  val localFilePath: String?,      // null إذا لم تُحمّل بعد
  val cachedAtEpochMs: Long = 0L
)
```

### ملاحظة مهمة عن القاعدة الحالية
قاعدة البيانات `UserDatabase` حالياً فيها:
- `UserEpisodeEntity` (مفضلة + watchProgress)
- `UserStatsEntity` (غير مُستعرض هنا)

ويتم بناء DB مع `fallbackToDestructiveMigration()`. هذا يعني أن أي زيادة جداول قد تمسح بيانات المستخدم عند تغيير version. ضمن الخطة سنزيل `fallbackToDestructiveMigration()` ونضيف `Migration` حتى لا نخسر بيانات المفضلة وتقدم الأنمي.

---

[Files]
سنضيف ملفات جديدة للمانجا (network/repository/db/ui) ونعدّل ملفات قائمة (MainActivity و Room DB و Gradle إن احتجنا).

## New Files (مع المسار والغرض)

### Network
- `app/src/main/java/com/saiyan/dragonballuniverse/network/MangaApiService.kt`  
  Retrofit endpoints للمانجا (chapters + chapter pages).
- `app/src/main/java/com/saiyan/dragonballuniverse/network/MangaDtos.kt`  
  DTOs الخاصة بـ JSON للمانجا.
- `app/src/main/java/com/saiyan/dragonballuniverse/network/MangaRetrofitClient.kt`  
  Retrofit client منفصل أو دمج داخل `JikanRetrofitClient` (الأفضل فصل لتفادي baseUrl مختلف).

### Data/Repository
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/MangaModels.kt`  
  `MangaArc`, `MangaChapterInfo`, `MangaPage`, `MangaChapterPages`.
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/MangaRepository.kt`  
  واجهة + تنفيذ يجلب من الشبكة + يدمج مع حالات Room.
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/MangaRepositoryImpl.kt`  
  Implementation.

### Room
- `app/src/main/java/com/saiyan/dragonballuniverse/db/MangaProgressDao.kt`
- `app/src/main/java/com/saiyan/dragonballuniverse/db/MangaDownloadDao.kt`
- `app/src/main/java/com/saiyan/dragonballuniverse/db/MangaPageCacheDao.kt` (اختياري)
- `app/src/main/java/com/saiyan/dragonballuniverse/db/UserMangaProgressEntity.kt`
- `app/src/main/java/com/saiyan/dragonballuniverse/db/UserMangaDownloadEntity.kt`
- `app/src/main/java/com/saiyan/dragonballuniverse/db/UserMangaPageCacheEntity.kt` (اختياري)
- `app/src/main/java/com/saiyan/dragonballuniverse/db/migrations/Migration_2_3.kt`  
  Migration يحافظ على بيانات الجداول الحالية ويضيف الجداول الجديدة.

### UI (Compose)
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/ui/MangaHomeScreen.kt`  
  شاشة: Tabs للأجزاء Classic/Z/Super + قائمة فصول مع بحث/فلترة + حالات التحميل.
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/ui/MangaChapterReaderScreen.kt`  
  قارئ صفحات حقيقي: Pager + حفظ الصفحة + تحميل/كاش.
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/ui/MangaReaderTopBar.kt`  
  شريط علوي: رجوع + مؤشر صفحة + زر فهرس + إعدادات القراءة.
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/ui/MangaDownloadBottomSheet.kt`  
  واجهة تنزيل فصل/إدارة أوفلاين.
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/ui/MangaComponents.kt`  
  عناصر UI مشتركة: Chapter card, progress chip, offline badge, shimmer.
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/ui/MangaUiState.kt`  
  sealed states للشاشات.

### ViewModel
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/MangaViewModel.kt`  
  يجمع بين API و Room، ويحرك UI states، ويطبق prefetch.

### Offline/Cache
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/offline/MangaOfflineManager.kt`  
  مسؤول عن تنزيل صفحات فصل وحفظها في التخزين وإدارة الحالة.
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/offline/MangaFileStore.kt`  
  مسارات الملفات، أسماء ثابتة، تنظيف.
- `app/src/main/java/com/saiyan/dragonballuniverse/manga/offline/MangaImageLoader.kt`  
  منطق اختيار المصدر: local file إن وجد وإلا remote url + Coil.

## Existing Files to Modify

- `app/src/main/java/com/saiyan/dragonballuniverse/MainActivity.kt`
  - فصل كود المانجا الحالي من الملف (حاليًا ملف ضخم يحوي كل UI) إلى ملفات `manga/*`.
  - استبدال `MangaDetailsScreen` الحالية بمدخل `MangaHomeScreen` + `MangaChapterReaderScreen`.
  - إزالة/إهمال `dragonBallManga` و `MangaReaderScreen` التجريبي أو تحويله لنسخة جديدة.

- `app/src/main/java/com/saiyan/dragonballuniverse/db/UserDatabase.kt`
  - زيادة version من 2 إلى 3.
  - إزالة `fallbackToDestructiveMigration()`.
  - إضافة `.addMigrations(MIGRATION_2_3)`.

- `app/build.gradle.kts` (إن احتجنا)
  - تأكيد dependencies: Room KTX, Retrofit, OkHttp, Coil, WorkManager (اختياري للتنزيلات الخلفية).
  - إضافة `androidx.work:work-runtime-ktx` إذا قررنا تنزيل بالخلفية.

## Files to Delete / Move
- لا حذف إلزامي، لكن ننقل Compose الخاص بالمانجا من `MainActivity.kt` إلى `manga/ui/*` لتقليل التضخم.
- إبقاء أي كود أنمي/كويز كما هو.

---

[Functions]
سنضيف/نعدل دوال محددة لتغطية: جلب الفصول والصفحات، حفظ تقدم القراءة، تنزيل أوفلاين، اختيار مصدر الصورة، وتحسين الأداء عبر prefetch.

## Network

### `MangaApiService.getChapters`
```kotlin
@GET("manga/dragonball/{arc}/chapters")
suspend fun getChapters(@Path("arc") arc: String): MangaChaptersResponseDto
```
- Purpose: إرجاع قائمة فصول لقوس محدد.
- Errors: يعاد رمي exceptions؛ التعامل يكون في repository/viewmodel.

### `MangaApiService.getChapter`
```kotlin
@GET("manga/dragonball/{arc}/chapters/{chapterNumber}")
suspend fun getChapter(
  @Path("arc") arc: String,
  @Path("chapterNumber") chapterNumber: Int
): MangaChapterDto
```
- Purpose: إرجاع صفحات فصل محدد (روابط الصور).
- Validation: التأكد من pages ليست فارغة.

## Repository

### `MangaRepository.getChapters(arc: MangaArc): Flow<List<MangaChapterInfoWithUserState>>`
- يجلب من API + يدمج مع Room (lastReadPage, completed, downloaded badge).
- `MangaChapterInfoWithUserState`:
  ```kotlin
  data class MangaChapterInfoWithUserState(
    val info: MangaChapterInfo,
    val lastReadPageIndex: Int,
    val isCompleted: Boolean,
    val isDownloaded: Boolean
  )
  ```
- Errors: emits error state في UI، مع retry.

### `MangaRepository.getChapterPages(arc: MangaArc, chapterNumber: Int): MangaChapterPages`
- يجلب DTO ثم يحولها لـ `MangaChapterPages`.

### `MangaRepository.updateProgress(arc, chapterNumber, lastReadPageIndex, isCompleted)`
- يكتب إلى `user_manga_progress`.

## Room DAOs

### `MangaProgressDao.upsertProgress(entity: UserMangaProgressEntity)`
- replace on conflict.

### `MangaProgressDao.getProgress(arc: String, chapterNumber: Int): UserMangaProgressEntity?`

### `MangaDownloadDao.upsert(entity: UserMangaDownloadEntity)`
### `MangaDownloadDao.get(arc: String, chapterNumber: Int): UserMangaDownloadEntity?`

(وإذا أضفنا PageCacheDao: upsert/query by chapter).

## Offline/Cache

### `MangaOfflineManager.downloadChapter(arc: MangaArc, chapterNumber: Int, pages: List<MangaPage>): Flow<DownloadProgress>`
- يقوم بتحميل الصفحات بشكل متسلسل/محدود التوازي (2-4) لتوازن الأداء.
- يحفظ كل صفحة كملف داخل:
  - `context.filesDir/manga/{arc}/{chapterNumber}/{pageIndex}.jpg`
- يحدث `UserMangaDownloadEntity` دوريًا.
- التعامل مع الأخطاء: mark failed مع errorMessage، وتترك الملفات الجزئية (أو تنظف حسب سياسة).

### `MangaImageLoader.resolvePageModel(arc, chapterNumber, pageIndex, remoteUrl): Any`
- يرجّع:
  - `File` لو متوفر محليًا
  - وإلا `String url`
- يستخدم في `AsyncImage`/Coil.

### `prefetchNextPages(currentPageIndex: Int)`
- عند انتقال صفحة، نطلب Coil fetch لصفحات قادمة (مثلا +2/+3) إذا ليست محليًا.

## UI

### `MangaChapterReaderScreen(...)`
- Signature مقترح:
```kotlin
@Composable
fun MangaChapterReaderScreen(
  arc: MangaArc,
  chapterNumber: Int,
  onBack: () -> Unit,
  viewModel: MangaViewModel
)
```
- Behavior:
  - تحميل صفحات الفصل (state loading/error/success).
  - Pager RTL (reverseLayout=true) + مؤشر.
  - حفظ progress عند تغيير الصفحة (debounce 300-800ms).
  - زر “تنزيل” يظهر bottom sheet.

---

[Changes]
سننفذ التحسينات كحزمة متكاملة لكن بخطوات صغيرة قابلة للتحقق.

1) **تفكيك قسم المانجا من `MainActivity.kt`**
   - إنشاء package `manga/` و `manga/ui/`.
   - نقل النماذج الحالية (`Manga`, `MangaChapter`, screens) أو استبدالها تدريجيًا.
   - إبقاء Navigation الداخلي بسيط داخل Compose state (كما المشروع حالياً) لتجنب إدخال NavHost كبير الآن.

2) **إضافة طبقة Network للمانجا**
   - إنشاء `MangaApiService` + DTOs + Retrofit client.
   - إضافة baseUrl خاص (حتى لو مؤقت) أو استخدام نفس Retrofit إذا نفس الدومين.
   - توحيد تسوية الروابط `http->https` (مثل `resolveImageUrl` الموجود) مع تخصيص لصور الصفحات.

3) **إضافة جداول Room وحفظ البيانات داخل `user_db`**
   - إضافة Entities + DAOs + تحديث `UserDatabase`.
   - كتابة Migration 2->3 لإضافة الجداول بدون مسح `user_episodes`.
   - إزالة destructive migration نهائيًا للحفاظ على بيانات المستخدم.

4) **Repository + ViewModel للمانجا**
   - `MangaRepositoryImpl` ينسّق:
     - جلب chapters من API
     - دمج progress/download من Room
     - جلب صفحات فصل
   - `MangaViewModel`:
     - `StateFlow<MangaHomeUiState>`
     - `StateFlow<MangaReaderUiState>`
     - وظائف: `loadChapters(arc)`, `openChapter(arc, chapterNumber)`, `saveProgress(...)`, `toggleCompleted(...)`, `downloadChapter(...)`

5) **UI: MangaHomeScreen (Classic/Z/Super)**
   - Tabs ثلاثية ثابتة.
   - قائمة فصول لكل تبويب مع:
     - شارة “مكتمل”
     - شريط تقدم (آخر صفحة / عدد الصفحات)
     - شارة “متاح أوفلاين”
     - بحث داخل الجزء (فلترة by title/chapter).
   - تحسين UX:
     - skeleton/shimmer أثناء التحميل
     - retry واضح عند الخطأ
     - empty state في حال API فاضي

6) **UI: قارئ صفحات حقيقي MangaChapterReaderScreen**
   - Pager RTL بتحميل صور حقيقية عبر Coil.
   - Gestures:
     - تكبير pinch + سحب داخل الصورة (إعادة استخدام منطقك الحالي clampPanOffset/transformGestures لكن مطبق على صورة الصفحة).
     - تبديل الصفحة بالسوَيب، مع تعطيل السحب أثناء تكبير (حتى لا يتعارض).
   - Top bar:
     - مؤشر (page / total)
     - زر فهرس صفحات (BottomSheet: Grid thumbnails) — *مرحلة ثانية إذا الوقت ضيق*.
     - زر تنزيل/إدارة أوفلاين.
   - Resume:
     - عند فتح الفصل، الانتقال تلقائياً لآخر صفحة محفوظة.
   - Completion:
     - عند الوصول لآخر صفحة وتجاوزها/تأكيد، تعليم الفصل مكتمل.

7) **Offline: تنزيل فصل + كاش ذكي**
   - سياسة الكاش:
     - الاعتماد على Coil DiskCache للعرض السريع.
     - بالإضافة لتنزيل “ملفات” داخل app storage لفصل كامل لتضمن العمل بدون إنترنت حتى لو Coil مسح الكاش.
   - تنزيل:
     - زر “تنزيل الفصل”.
     - عرض تقدم: صفحات محمّلة/المتبقي.
     - إمكانية إلغاء (اختياري).
   - أثناء القراءة:
     - إذا الصفحة غير محلية والإنترنت مقطوع: عرض رسالة + زر retry + إمكانية الانتقال للصفحات المحفوظة.
   - تنظيف:
     - صفحة إعدادات بسيطة: “مسح تنزيلات المانجا” أو “مسح فصل” (مرحلة ثانية).

8) **تحسين الأداء**
   - Prefetch صفحات قادمة.
   - تقليل إعادة التركيب:
     - استعمال `remember` و `derivedStateOf` للأشياء الثقيلة.
   - تحميل الصور:
     - استخدام `ImageRequest` مع `diskCacheKey` ثابت (arc/chapter/page).
     - تعيين `size` مناسب (match device width) لتقليل memory.
   - Network:
     - إضافة OkHttp cache headers إن أمكن (مع Direct URLs غالبًا لا).
     - Timeouts/retry بسيط في Repository.

9) **الأمان (بالحد الأدنى المطلوب)**
   - إجبار https عند الإمكان.
   - رفض الروابط الفارغة.
   - تجنب logging لروابط حساسة (ليس مطلوب لكن جيد).
   - Network Security Config إن كانت الصور http (إذا اضطررنا) — لكن الأفضل تحويلها لـ https.

10) **تحسينات واجهة “هائلة” (من جميع النواحي)**
   - UX:
     - “استكمال القراءة” زر في MangaHome يفتح آخر فصل/صفحة مباشرة.
     - “آخر ما قرأت” Section.
     - شريط تحكم سريع داخل القارئ: تغيير اتجاه القراءة (RTL/LTR)، تبديل وضع single/continuous (مرحلة ثانية).
   - Visual:
     - غلاف/بنر لكل جزء (Classic/Z/Super).
     - لقطات صغيرة للفصول (thumbnails) إن توفرت.
     - micro-interactions: bounceClick موجود — نعيد استخدامه.
   - Accessibility:
     - تكبير النص في قائمة الفصول، تباين ألوان، contentDescriptions.
   - Reliability:
     - حفظ progress باستمرار مع debounce لتقليل writes.
     - مقاومة انقطاع الشبكة.
   - Analytics (اختياري):
     - counts محلية (بدون خدمات خارجية): عدد الفصول المكتملة، إجمالي صفحات مقروءة.

---

[Tests]
سنركز على اختبارات وحدات للـ mapping والـ repository والمنطق غير UI، مع اختبارات بسيطة للـ Room migrations.

- Unit Tests
  - Mapping: `MangaChapterDto -> MangaChapterPages` يحول الصفحات correctly ويفرض validation.
  - `resolveImageUrl`/normalizeUrl للصفحات (http->https).
  - Repository: دمج progress/download مع chapters.
  - Offline: `MangaFileStore` يبني مسارات صحيحة، و`downloadChapter` يحدث progress.

- Integration Tests (Android instrumented)
  - Room Migration test: 2->3 بدون فقد بيانات `user_episodes`.
  - DAO tests: upsert/get progress, downloads.

- Edge Cases
  - فصل بصفحات فارغة (API خطأ) -> Error UI.
  - روابط صور بامتدادات مختلفة (jpg/png/webp).
  - انقطاع الإنترنت أثناء منتصف الفصل مع وجود صفحات محلية جزئية.
  - فصل كبير (100+ صفحة) -> التأكد من paging/perf.

- Performance Considerations
  - قياس memory أثناء التمرير/التكبير.
  - التأكد من عدم تحميل الصور بجودة أعلى من الحاجة (size override).
