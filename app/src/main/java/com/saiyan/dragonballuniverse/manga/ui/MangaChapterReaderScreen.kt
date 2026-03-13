package com.saiyan.dragonballuniverse.manga.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.saiyan.dragonballuniverse.manga.offline.MangaCoil
import com.saiyan.dragonballuniverse.manga.MangaArc
import com.saiyan.dragonballuniverse.manga.MangaReaderUiState
import com.saiyan.dragonballuniverse.manga.MangaViewModel
import com.saiyan.dragonballuniverse.manga.offline.MangaFileStore
import com.saiyan.dragonballuniverse.manga.offline.MangaImageLoader

@Composable
fun MangaChapterReaderScreen(
    arc: MangaArc,
    chapterNumber: Int,
    onBack: () -> Unit,
    viewModel: MangaViewModel,
) {
    val state by viewModel.readerUiState.collectAsState()

    LaunchedEffect(arc, chapterNumber) {
        viewModel.openChapter(arc, chapterNumber)
    }

    when (val s = state) {
        is MangaReaderUiState.Idle,
        is MangaReaderUiState.Loading,
        -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is MangaReaderUiState.Error -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "خطأ: ${s.message}",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        is MangaReaderUiState.Success -> {
            val chapter = s.chapter
            val pages = chapter.pages
            val initial = s.initialPageIndex

            val context = LocalContext.current

            val imageLoader =
                remember(context.applicationContext) {
                    MangaImageLoader(
                        fileStore = MangaFileStore(context.applicationContext),
                    )
                }

            val pagerState =
                rememberPagerState(
                    initialPage = initial,
                    pageCount = { pages.size },
                )

            LaunchedEffect(pagerState.currentPage) {
                val isLastPage = pagerState.currentPage == pages.lastIndex
                viewModel.saveProgress(
                    arc = arc,
                    chapterNumber = chapterNumber,
                    lastReadPageIndex = pagerState.currentPage,
                    isCompleted = isLastPage,
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
            ) {
                HorizontalPager(
                    state = pagerState,
                    reverseLayout = true, // RTL reading
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    val model =
                        imageLoader.resolvePageModel(
                            arc = arc,
                            chapterNumber = chapterNumber,
                            pageIndex = pageIndex,
                            remoteUrl = pages[pageIndex].imageUrl,
                        )

                    ZoomablePageImage(
                        model = model,
                    )
                }

                val currentPageLabel = (pagerState.currentPage + 1).toString().padStart(3, '0')
                val totalLabel = pages.size.toString().padStart(3, '0')

                Text(
                    text = "$currentPageLabel / $totalLabel",
                    color = Color.White.copy(alpha = 0.75f),
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                )

                IconButton(
                    onClick = onBack,
                    modifier =
                        Modifier
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomablePageImage(
    model: Any,
) {
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun clampPanOffset(
        inOffset: Offset,
        inScale: Float,
        container: IntSize,
    ): Offset {
        if (container == IntSize.Zero || inScale <= 1f) return Offset.Zero

        val maxTranslationX = (container.width * (inScale - 1f)) / 2f
        val maxTranslationY = (container.height * (inScale - 1f)) / 2f

        return Offset(
            x = inOffset.x.coerceIn(-maxTranslationX, maxTranslationX),
            y = inOffset.y.coerceIn(-maxTranslationY, maxTranslationY),
        )
    }

    val gestureModifier =
        Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 3f)
                    scale = newScale

                    offset =
                        if (newScale == 1f) {
                            Offset.Zero
                        } else {
                            clampPanOffset(offset + pan, newScale, containerSize)
                        }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }

    val mangaImageLoader = remember(context.applicationContext) { MangaCoil.imageLoader(context.applicationContext) }

    AsyncImage(
        model =
            ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
        imageLoader = mangaImageLoader,
        contentDescription = null,
        modifier = gestureModifier,
    )
}
