package com.saiyan.dragonballuniverse

import android.content.pm.ActivityInfo
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.composed
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.request.ImageRequest
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.saiyan.dragonballuniverse.network.ApiEpisode
import com.saiyan.dragonballuniverse.network.JikanRetrofitClient
import com.saiyan.dragonballuniverse.ui.theme.DarkBackground
import com.saiyan.dragonballuniverse.ui.theme.DragonBallUniverseTheme
import com.saiyan.dragonballuniverse.ui.theme.GokuOrange
import com.saiyan.dragonballuniverse.ui.theme.VegetaBlue
import android.util.Log
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DragonBallUniverseTheme {
                DragonBallScaffold()
            }
        }
    }
}

private enum class MainDestination(
    val label: String
) {
    Anime(label = "أنمي"),
    Manga(label = "مانغا")
}

private const val TAG_IMAGE: String = "DBU_Image"

private const val DEFAULT_DBZ_COVER_URL: String =
    "https://j.top4top.io/p_3722xahg41.jpg"

private data class Episode(
    val number: Int,
    val title: String,
    val duration: String,
    val imageUrl: String = DEFAULT_DBZ_COVER_URL,
    val progress: Float = 0f
)

private data class AnimeSeason(
    val title: String,
    val year: String,
    val description: String,
    val episodes: List<Episode>,
    val imageUrl: String = DEFAULT_DBZ_COVER_URL,
    val status: String? = null
)

private data class MangaChapter(
    val number: String,
    val title: String,
    val date: String,
    val isRead: Boolean = false
)

private data class Manga(
    val title: String,
    val description: String,
    val chapters: List<MangaChapter>
)

private val EpisodeSaver: Saver<Episode, List<Any?>> =
    Saver(
        save = { episode ->
            listOf(
                episode.number,
                episode.title,
                episode.duration,
                episode.imageUrl,
                episode.progress
            )
        },
        restore = { saved ->
            val number = saved.getOrElse(0) { null } as? Int ?: return@Saver null
            val title = saved.getOrElse(1) { null } as? String ?: return@Saver null
            val duration = saved.getOrElse(2) { null } as? String ?: return@Saver null
            val imageUrl = saved.getOrElse(3) { "" } as? String ?: ""
            val progress = saved.getOrElse(4) { 0f } as? Float ?: 0f
            Episode(
                number = number,
                title = title,
                duration = duration,
                imageUrl = imageUrl.ifBlank { DEFAULT_DBZ_COVER_URL },
                progress = progress.coerceIn(0f, 1f)
            )
        }
    )

private val AnimeSeasonSaver: Saver<AnimeSeason, List<Any?>> =
    Saver(
        save = { season ->
            val episodesSaved = ArrayList<List<Any?>>(season.episodes.size)
            season.episodes.forEach { ep ->
                episodesSaved.add(with(EpisodeSaver) { save(ep) } ?: return@Saver null)
            }
            listOf(
                season.title,
                season.year,
                season.description,
                episodesSaved,
                season.imageUrl,
                season.status
            )
        },
        restore = { saved ->
            val title = saved.getOrElse(0) { null } as? String ?: return@Saver null
            val year = saved.getOrElse(1) { null } as? String ?: return@Saver null
            val description = saved.getOrElse(2) { null } as? String ?: return@Saver null
            val episodesPayload = saved.getOrElse(3) { null } as? List<*> ?: return@Saver null
            val imageUrl = saved.getOrElse(4) { "" } as? String ?: ""
            val status = saved.getOrElse(5) { null } as? String

            val episodes = episodesPayload.mapNotNull { payload ->
                val listPayload = payload as? List<*> ?: return@mapNotNull null
                @Suppress("UNCHECKED_CAST")
                with(EpisodeSaver) { restore(listPayload as List<Any?> ?: return@mapNotNull null) }
            }

            AnimeSeason(
                title = title,
                year = year,
                description = description,
                episodes = episodes,
                imageUrl = imageUrl.ifBlank { DEFAULT_DBZ_COVER_URL },
                status = status
            )
        }
    )

private val NullableAnimeSeasonSaver: Saver<AnimeSeason?, Any> =
    Saver(
        save = { season ->
            season?.let { with(AnimeSeasonSaver) { save(it) } } ?: 0
        },
        restore = { saved ->
            when (saved) {
                0 -> null
                is List<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val payload = saved as List<Any?>
                    with(AnimeSeasonSaver) { restore(payload) }
                }
                else -> null
            }
        }
    )

private fun statusBadgeColor(
    status: String
): Color =
    when (status) {
        "مستمر" -> Color(0xFF2E7D32)
        "مكتمل" -> Color(0xFF1565C0)
        "قادم" -> Color(0xFFEF6C00)
        else -> Color(0xFF616161)
    }

private fun ratingBadgeColor(
    rating: String
): Color {
    val value = rating.toFloatOrNull() ?: 0f
    return when {
        value >= 8f -> Color(0xFF2E7D32)
        value >= 6.5f -> Color(0xFFEF6C00)
        else -> Color(0xFFB71C1C)
    }
}

internal fun resolveImageUrl(
    primary: String?,
    fallback: String = DEFAULT_DBZ_COVER_URL
): String {
    val trimmed = primary?.trim().orEmpty()
    val chosen = if (trimmed.isBlank()) fallback else trimmed
    return if (chosen.startsWith("http://")) {
        "https://${chosen.removePrefix("http://")}"
    } else {
        chosen
    }
}

@Composable
private fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    minimizedMaxLines: Int = 3
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val showToggle = text.length > 140

    Text(
        text = text,
        color = Color(0xFFDDDDDD),
        lineHeight = 24.sp,
        maxLines = if (expanded) Int.MAX_VALUE else minimizedMaxLines,
        modifier = modifier
    )

    if (showToggle) {
        Text(
            text = if (expanded) "إظهار أقل" else "إظهار المزيد",
            color = GokuOrange,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable { expanded = !expanded }
        )
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
            progress = ((index % 10) + 1) / 10f
        )
    }

private fun clampPanOffset(
    offset: Offset,
    scale: Float,
    container: IntSize
): Offset {
    if (container == IntSize.Zero || scale <= 1f) return Offset.Zero

    val maxTranslationX = (container.width * (scale - 1f)) / 2f
    val maxTranslationY = (container.height * (scale - 1f)) / 2f

    return Offset(
        x = offset.x.coerceIn(-maxTranslationX, maxTranslationX),
        y = offset.y.coerceIn(-maxTranslationY, maxTranslationY)
    )
}

/**
 * Bounce click + Haptic feedback.
 * - onPress: scale to 0.95f + haptic
 * - onRelease: scale back to 1f then trigger onClick
 */
private fun Modifier.bounceClick(
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 140, easing = LinearEasing),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    val released = tryAwaitRelease()
                    pressed = false

                    if (released) onClick()
                }
            )
        }
}

@Composable
private fun ZoomableImage(
    modifier: Modifier = Modifier
) {
    var scale by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val gestureModifier =
        modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 3f)
                    scale = newScale

                    if (newScale == 1f) {
                        offset = Offset.Zero
                    } else {
                        offset = clampPanOffset(offset + pan, newScale, containerSize)
                    }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }

    Image(
        imageVector = Icons.Filled.AccountBox,
        contentDescription = null,
        modifier = gestureModifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun MangaReaderScreen(
    onBack: () -> Unit
) {
    val pages = (1..5).toList()
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize()
        ) {
            ZoomableImage(
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${pages.size}",
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رجوع",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun VideoPlayerScreen(
    videoUrl: String,
    onBack: () -> Unit
) {
    if (videoUrl.isBlank()) {
        onBack()
        return
    }

    val context = LocalContext.current
    val activity = LocalActivity.current

    DisposableEffect(activity) {
        val job: Job? =
            if (activity == null) {
                null
            } else {
                kotlinx.coroutines.CoroutineScope(Dispatchers.Main.immediate).launch {
                    delay(200)
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                }
            }

        onDispose {
            job?.cancel()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val player = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "رجوع",
                tint = Color.White
            )
        }
    }
}

private val dragonBallManga = Manga(
    title = "دراغون بول",
    description = "وصف عربي مناسب يعرّف بعالم دراغون بول وبداية مغامرات غوكو ورحلة كرات التنين.",
    chapters = listOf(
        MangaChapter(number = "1", title = "لقاء غوكو وبولما", date = "1984", isRead = true),
        MangaChapter(number = "2", title = "رحلة البحث عن كرة التنين", date = "1984", isRead = true),
        MangaChapter(number = "3", title = "أول اختبار حقيقي", date = "1984", isRead = false),
        MangaChapter(number = "4", title = "مواجهة جديدة", date = "1984", isRead = false),
        MangaChapter(number = "5", title = "قوة السايان الأولى", date = "1984", isRead = false)
    )
)

private val animeSeasons = listOf(
    AnimeSeason(
        title = "دراغون بول",
        year = "1986",
        description = "بداية أسطورة غوكو ولقائه بـ بولما والبحث عن كرات التنين.",
        episodes = listOf(
            Episode(number = 1, title = "لقاء غوكو وبولما", duration = "24 دقيقة"),
            Episode(number = 2, title = "رحلة البحث عن كرة التنين", duration = "24 دقيقة"),
            Episode(number = 3, title = "أول اختبار حقيقي", duration = "24 دقيقة")
        )
    ),
    AnimeSeason(
        title = "دراغون بول Z",
        year = "1989",
        description = "وصول السايانز إلى الأرض ومعارك ملحمية لإنقاذ الكون.",
        episodes = listOf(
            Episode(number = 1, title = "وصول راديتز", duration = "24 دقيقة"),
            Episode(number = 2, title = "تدريب غوهان", duration = "24 دقيقة"),
            Episode(number = 3, title = "معركة على الأرض", duration = "24 دقيقة"),
            Episode(number = 4, title = "قوة السايانز", duration = "24 دقيقة")
        )
    ),
    AnimeSeason(
        title = "دراغون بول GT",
        year = "1996",
        description = "غوكو يعود صغيراً وينطلق في رحلة عبر الفضاء.",
        episodes = listOf(
            Episode(number = 1, title = "عودة غوكو صغيراً", duration = "24 دقيقة"),
            Episode(number = 2, title = "انطلاق الرحلة", duration = "24 دقيقة"),
            Episode(number = 3, title = "مفاجآت الفضاء", duration = "24 دقيقة")
        )
    ),
    AnimeSeason(
        title = "دراغون بول Super",
        year = "2015",
        description = "ظهور حكام الدمار وبطولة الأكوان المتعددة.",
        episodes = listOf(
            Episode(number = 1, title = "بداية عصر جديد", duration = "24 دقيقة"),
            Episode(number = 2, title = "مواجهة إله الدمار", duration = "24 دقيقة"),
            Episode(number = 3, title = "بطولة الأكوان", duration = "24 دقيقة"),
            Episode(number = 4, title = "تحدي جديد", duration = "24 دقيقة")
        )
    )
)

@Composable
private fun DragonBallScaffold() {
    var selectedDestination by rememberSaveable { mutableStateOf(MainDestination.Anime) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { DragonBallTopBar() },
        bottomBar = {
            DragonBallBottomBar(
                selected = selectedDestination,
                onSelect = { selectedDestination = it }
            )
        }
    ) { innerPadding ->
        DragonBallHomeContent(
            selectedDestination = selectedDestination,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DragonBallTopBar(
    title: String = "دراغون بول بالعربي"
) {
    TopAppBar(
        title = { Text(text = title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = VegetaBlue,
            titleContentColor = Color.White
        )
    )
}

@Composable
private fun DragonBallBottomBar(
    selected: MainDestination,
    onSelect: (MainDestination) -> Unit
) {
    NavigationBar(
        containerColor = VegetaBlue
    ) {
        NavigationBarItem(
            selected = selected == MainDestination.Anime,
            onClick = { onSelect(MainDestination.Anime) },
            icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "أنمي") },
            label = { Text("أنمي") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GokuOrange,
                selectedTextColor = GokuOrange,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = selected == MainDestination.Manga,
            onClick = { onSelect(MainDestination.Manga) },
            icon = { Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = "مانغا") },
            label = { Text("مانغا") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GokuOrange,
                selectedTextColor = GokuOrange,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun DragonBallHomeContent(
    selectedDestination: MainDestination,
    modifier: Modifier = Modifier
) {
    var selectedSeason by rememberSaveable(stateSaver = NullableAnimeSeasonSaver) { mutableStateOf<AnimeSeason?>(null) }
    var selectedVideoUrl by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedManga by rememberSaveable { mutableStateOf<Manga?>(null) }
    var selectedMangaChapterNumber by rememberSaveable { mutableStateOf<String?>(null) }

    var episodesList by remember { mutableStateOf<List<ApiEpisode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val fetchTrigger = rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(fetchTrigger.intValue) {
        isLoading = true
        errorMessage = null

        try {
            val response = JikanRetrofitClient.apiService.getDragonBallEpisodes()
            episodesList = response.data
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (selectedVideoUrl != null) {
        VideoPlayerScreen(
            videoUrl = selectedVideoUrl!!,
            onBack = { selectedVideoUrl = null }
        )
        return
    }

    if (selectedMangaChapterNumber != null) {
        MangaReaderScreen(
            onBack = { selectedMangaChapterNumber = null }
        )
        return
    }

    val sampleUrl =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

    val dbzSeason =
        AnimeSeason(
            title = "دراغون بول زد",
            year = "1989",
            description = "وصول السايانز إلى الأرض ومعارك ملحمية لإنقاذ الكون.",
            episodes = mapApiEpisodesToUiEpisodes(episodesList),
            imageUrl = DEFAULT_DBZ_COVER_URL,
            status = "مكتمل"
        )

    val seasonsToShow = listOf(dbzSeason)

    when (selectedDestination) {
        MainDestination.Anime -> {
            when {
                isLoading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = modifier
                            .fillMaxSize()
                            .background(DarkBackground),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(6) {
                            ShimmerPosterCard()
                        }
                    }
                }

                errorMessage != null -> {
                    NetworkErrorScreen(
                        errorMessage = errorMessage ?: "Unknown error",
                        onRetry = { fetchTrigger.intValue++ }
                    )
                }

                selectedSeason == null -> {
                    AnimeSeasonsScreen(
                        seasons = seasonsToShow,
                        onSeasonClick = { selectedSeason = it },
                        modifier = modifier
                    )
                }

                else -> {
                    SeasonDetailsScreen(
                        season = selectedSeason!!,
                        onBack = { selectedSeason = null },
                        onEpisodeClick = { selectedVideoUrl = sampleUrl },
                        modifier = modifier
                    )
                }
            }
        }

        MainDestination.Manga -> {
            if (selectedManga == null) {
                selectedManga = dragonBallManga
            }
            MangaDetailsScreen(
                manga = selectedManga ?: dragonBallManga,
                modifier = modifier,
                onChapterClick = { chapter ->
                    selectedMangaChapterNumber = chapter.number
                }
            )
        }
    }
}

@Composable
private fun MangaDetailsScreen(
    manga: Manga,
    modifier: Modifier = Modifier,
    onChapterClick: (MangaChapter) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2A2A2A))
                )

                Text(
                    text = manga.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Text(
                    text = manga.description,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 6.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp),
                    color = Color(0xFF2A2A2A),
                    thickness = 1.dp
                )
            }
        }

        items(manga.chapters) { chapter ->
            ChapterRowItem(
                chapter = chapter,
                onClick = { onChapterClick(chapter) }
            )
        }
    }
}

@Composable
private fun ChapterRowItem(
    chapter: MangaChapter,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isRead = chapter.isRead

    val chapterNumberColor = if (isRead) Color.Gray else GokuOrange
    val chapterTitleColor = if (isRead) Color.Gray else Color.White
    val chapterDateColor = Color.Gray

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        val clickableModifier =
            if (onClick != null) {
                Modifier.clickable { onClick() }
            } else {
                Modifier
            }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(clickableModifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "الفصل ${chapter.number}",
                    color = chapterNumberColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = chapter.title,
                    color = chapterTitleColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = chapter.date,
                    color = chapterDateColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "تحميل",
                tint = Color.Gray
            )
        }

        HorizontalDivider(
            color = Color(0xFF1E1E1E),
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun AnimeSeasonsScreen(
    seasons: List<AnimeSeason>,
    onSeasonClick: (AnimeSeason) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(seasons) { season ->
            PosterCard(
                season = season,
                onClick = { onSeasonClick(season) }
            )
        }
    }
}

@Composable
private fun PosterCard(
    season: AnimeSeason,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val imageUrl = resolveImageUrl(season.imageUrl)
    val statusLabel = season.status?.trim().orEmpty()

    Card(
        modifier = modifier
            .size(width = 160.dp, height = 240.dp)
            .clip(shape)
            .bounceClick { onClick() },
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(resolveImageUrl(imageUrl))
                        .setHeader("User-Agent", "Mozilla/5.0")
                        .crossfade(true)
                        .build(),
                    contentDescription = "غلاف ${season.title}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(Color(0xFF2A2A2A)),
                    error = ColorPainter(Color(0xFF2A2A2A))
                )

                if (statusLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusBadgeColor(statusLabel))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f)
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = season.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ShimmerPosterCard(
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)

    var size by remember { mutableStateOf(IntSize.Zero) }

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "shimmerProgress"
    )

    val startX = (-size.width).toFloat() + (size.width * 2f * progress)
    val shimmerBrush =
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF2A2A2A),
                Color(0xFF3A3A3A),
                Color(0xFF2A2A2A)
            ),
            start = Offset(startX, 0f),
            end = Offset(startX + size.width, size.height.toFloat())
        )

    Box(
        modifier = modifier
            .size(width = 160.dp, height = 240.dp)
            .clip(shape)
            .background(shimmerBrush)
            .onSizeChanged { size = it }
    )
}

@Composable
private fun SeasonDetailsScreen(
    season: AnimeSeason,
    onBack: () -> Unit,
    onEpisodeClick: (Episode) -> Unit,
    modifier: Modifier = Modifier
) {
    val bannerHeight = 280.dp
    val posterWidth = 150.dp
    val posterHeight = 225.dp // 2:3
    val posterShape = RoundedCornerShape(12.dp)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
    ) {
        item {
            // HEADER: Banner + Overlay 60% + (Blur-like via gradient) + Poster يمين + Meta يسار
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerHeight)
            ) {
                val imageUrl = resolveImageUrl(season.imageUrl)

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(resolveImageUrl(imageUrl))
                        .setHeader("User-Agent", "Mozilla/5.0")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Banner ${season.title}",
                    modifier = Modifier
                        .matchParentSize()
                        .blur(25.dp),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(Color(0xFF2A2A2A)),
                    error = ColorPainter(Color(0xFF2A2A2A))
                )

                // Glassmorphism overlay: تدرّج أسود فوق البلور لإبراز النصوص والبوستر
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.75f),
                                    Color.Black.copy(alpha = 0.60f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // زر الرجوع (RTL: أعلى اليسار مناسب كـ back)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Meta (يسار البوستر)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = season.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            maxLines = 2
                        )

                        val statusText = season.status?.trim().takeUnless { it.isNullOrBlank() } ?: "غير معروف"
                        val rating = "8.7"
                        val episodesCount = season.episodes.size

                        Text(
                            text = "${season.year}  •  $statusText  •  $rating  •  $episodesCount حلقة",
                            color = Color(0xFFBDBDBD),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GenreChip(text = "أكشن")
                            GenreChip(text = "مغامرة")
                            GenreChip(text = "شونين")
                        }

                        Card(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = GokuOrange)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* TODO: watch now */ }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                                Text(
                                    text = "مشاهدة الآن",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    // Poster Card (يمين)
                    Card(
                        modifier = Modifier
                            .size(width = posterWidth, height = posterHeight),
                        shape = posterShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(resolveImageUrl(season.imageUrl))
                                .setHeader("User-Agent", "Mozilla/5.0")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Poster ${season.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            placeholder = ColorPainter(Color(0xFF2A2A2A)),
                            error = ColorPainter(Color(0xFF2A2A2A))
                        )
                    }
                }
            }
        }

        item {
            // SYNOPSIS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "القصة",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                ExpandableText(
                    text = season.description.ifBlank { "لا توجد قصة متاحة حالياً." },
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        item {
            // EPISODES TITLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الحلقات",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${season.episodes.size}",
                    color = Color(0xFFBDBDBD)
                )
            }
        }

        items(season.episodes) { episode ->
            EpisodeRowCard(
                episode = episode,
                onClick = { onEpisodeClick(episode) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun GenreChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF2A2A2A))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFFE0E0E0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NetworkErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = "https://e.top4top.io/p_3722fwcuz1.jpg",
                contentDescription = "Network error",
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Text(
                text = errorMessage,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "حاول مجدداً",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EpisodeRowCard(
    episode: Episode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Texts (يسار)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "الحلقة ${episode.number}",
                        color = Color(0xFFBDBDBD),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = episode.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1
                    )
                    Text(
                        text = episode.duration,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // Thumbnail (يمين) + Play overlay
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 72.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    val imageUrl = resolveImageUrl(episode.imageUrl)

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(resolveImageUrl(imageUrl))
                            .setHeader("User-Agent", "Mozilla/5.0")
                            .crossfade(true)
                            .build(),
                        contentDescription = "صورة الحلقة ${episode.number}",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(Color(0xFF2A2A2A)),
                        error = ColorPainter(Color(0xFF2A2A2A))
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )

                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(34.dp)
                    )
                }
            }

            // Progress bar (أسفل الكارت)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = episode.progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(GokuOrange)
                )
            }
        }
    }
}
