package com.saiyan.dragonballuniverse.manga

/**
 * Local catalog for chapter metadata (Option B).
 *
 * This replaces any need for a remote JSON “chapters” endpoint.
 *
 * Notes:
 * - `chapterNumber` must match your folder naming: chapter-XXX (3-digit padded).
 * - `title` is UI-only; pages are still probed from R2 at runtime.
 */
object MangaLocalCatalog {

    data class ChapterMeta(
        val chapterNumber: Int,
        val title: String,
        val releaseYear: String? = null,
    )

    /**
     * Update these lists as you upload new chapters.
     *
     * Minimal viable config: just add chapter numbers you have uploaded.
     */
    fun chaptersForArc(arc: MangaArc): List<ChapterMeta> =
        when (arc) {
            MangaArc.CLASSIC ->
                listOf(
                    // TODO: fill when uploaded
                )

            MangaArc.Z ->
                listOf(
                    // TODO: fill when uploaded
                )

            MangaArc.SUPER ->
                listOf(
                    ChapterMeta(chapterNumber = 1, title = "Chapter 001", releaseYear = null),
                )
        }
}
