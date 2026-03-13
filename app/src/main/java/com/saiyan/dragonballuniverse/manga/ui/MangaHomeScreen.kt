package com.saiyan.dragonballuniverse.manga.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saiyan.dragonballuniverse.manga.MangaArc
import com.saiyan.dragonballuniverse.manga.MangaHomeUiState
import com.saiyan.dragonballuniverse.manga.MangaRepository
import com.saiyan.dragonballuniverse.manga.MangaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaHomeScreen(
    viewModel: MangaViewModel,
    onOpenChapter: (arc: MangaArc, chapterNumber: Int) -> Unit,
) {
    val state by viewModel.homeUiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state) {
        val s = state
        if (s is MangaHomeUiState.Error) {
            Toast
                .makeText(context, "Manga load error: ${s.details}", Toast.LENGTH_LONG)
                .show()
        }
    }

    // Main arcs
    val tabs = listOf(MangaArc.CLASSIC, MangaArc.Z, MangaArc.SUPER)
    val selectedArc = viewModel.getCurrentArc()
    val selectedIndex = tabs.indexOf(selectedArc).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("المانجا") },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedIndex) {
                tabs.forEachIndexed { idx, arc ->
                    Tab(
                        selected = idx == selectedIndex,
                        onClick = { viewModel.loadChapters(arc) },
                        text = {
                            Text(
                                when (arc) {
                                    MangaArc.CLASSIC -> "Classic"
                                    MangaArc.Z -> "Z"
                                    MangaArc.SUPER -> "Super"
                                },
                            )
                        },
                    )
                }
            }

            when (val s = state) {
                is MangaHomeUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.padding(8.dp))
                        Text("جاري تحميل الفصول...")
                    }
                }

                is MangaHomeUiState.Error -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "حدث خطأ: ${s.message}",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.padding(6.dp))
                        Text(
                            text = "تفاصيل: ${s.details}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.padding(8.dp))
                        Text(
                            text = "اضغط على التبويب لإعادة المحاولة",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                is MangaHomeUiState.Success -> {
                    // Simple in-list download UI state (per chapter)
                    val downloadStatusByChapter = remember { mutableStateMapOf<Int, String>() }
                    val downloadProgressByChapter = remember { mutableStateMapOf<Int, Pair<Int, Int>>() }

                    MangaChapterList(
                        chapters = s.chapters,
                        downloadStatusByChapter = downloadStatusByChapter,
                        downloadProgressByChapter = downloadProgressByChapter,
                        onDownload = { chapter ->
                            downloadStatusByChapter[chapter.info.chapterNumber] = "downloading"
                            // Let the ViewModel resolve page URLs and download + track progress.
                            viewModel.downloadChapter(
                                arc = chapter.info.arc,
                                chapterNumber = chapter.info.chapterNumber,
                            ) { status, done, total ->
                                downloadStatusByChapter[chapter.info.chapterNumber] = status
                                downloadProgressByChapter[chapter.info.chapterNumber] = done to total
                            }
                        },
                        onOpen = { chapter -> onOpenChapter(chapter.info.arc, chapter.info.chapterNumber) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaChapterList(
    chapters: List<MangaRepository.MangaChapterInfoWithUserState>,
    downloadStatusByChapter: Map<Int, String>,
    downloadProgressByChapter: Map<Int, Pair<Int, Int>>,
    onDownload: (MangaRepository.MangaChapterInfoWithUserState) -> Unit,
    onOpen: (MangaRepository.MangaChapterInfoWithUserState) -> Unit,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(chapters) { chapter ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(chapter) }
                        .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الفصل ${chapter.info.chapterNumber}: ${chapter.info.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "عدد الصفحات: ${chapter.info.pageCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val dlStatus = downloadStatusByChapter[chapter.info.chapterNumber]
                val dlProg = downloadProgressByChapter[chapter.info.chapterNumber]

                if (chapter.isDownloaded) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Offline") },
                    )
                } else if (dlStatus == "downloading") {
                    val label =
                        if (dlProg != null) "تحميل ${dlProg.first}/${dlProg.second}"
                        else "تحميل..."
                    AssistChip(
                        onClick = {},
                        label = { Text(label) },
                    )
                } else {
                    // Enable "Download Chapter" (Phase 2)
                    Button(
                        onClick = { onDownload(chapter) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("Download")
                    }
                }

                if (!chapter.isDownloaded && dlStatus == null) {
                    // keep existing progress chips when not downloaded and not actively downloading
                    if (chapter.isCompleted) {
                        AssistChip(
                            onClick = {},
                            label = { Text("مكتمل") },
                        )
                    } else if (chapter.lastReadPageIndex > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("صفحة ${chapter.lastReadPageIndex + 1}") },
                        )
                    }
                }
            }
        }
    }
}
