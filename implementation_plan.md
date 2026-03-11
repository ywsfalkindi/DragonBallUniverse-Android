# Implementation Plan

[Overview]
Upgrade the `PosterCard` UI to include status badges and a separated bottom info box layout, and set "دراغون بول Z" status to "مكتمل".

The project is a single-activity Jetpack Compose Android app with most UI and in-memory models defined in `app/src/main/java/com/saiyan/dragonballuniverse/MainActivity.kt`. `PosterCard` currently renders an `AsyncImage` filling the whole card with a bottom gradient scrim and text over the image. The requested improvements add: (1) a status badge over the image (top-start), driven by a new `status` field on `AnimeSeason`, with specific color rules; (2) a structural refactor so the card is a `Column` where the image uses the top 80% and an explicit dark info box uses the bottom 20%, containing title and year with padding; and (3) change the DBZ season shown in the UI to have status `مكتمل`. These changes are localized to the Compose UI and internal models; they do not require new screens, new network calls, or changes to Retrofit/Coil configuration.

Because the app currently constructs a `dbzSeason` instance inside `DragonBallHomeContent` (overriding the earlier `animeSeasons` list), the DBZ status change must be applied to that `dbzSeason` instance to ensure the badge is shown in the running UI path. The badge rendering must be optional: if `status` is `null` or blank, no badge is shown.

[Types]
Add an optional `status` field to the internal `AnimeSeason` UI model so badges can be displayed.

Type changes (in `MainActivity.kt`):

```kotlin
private data class AnimeSeason(
    val title: String,
    val year: String,
    val description: String,
    val episodes: List<Episode>,
    val imageUrl: String = DEFAULT_DBZ_COVER_URL,
    val status: String? = null
)
```

Status validation/rendering rules:
- `status` is optional; if `status == null` or `status.isBlank()` then the badge is not rendered.
- Badge color mapping (exact Arabic strings):
  - `"مستمر"` -> Green background
  - `"مكتمل"` -> Blue background
  - `"قادم"` -> Orange background
  - Any other non-blank value -> Gray background (default)
- The badge text is the raw `status` string after trimming.
- Badge shape: `RoundedCornerShape(4.dp)`.

No other models (`Episode`, `Manga`, network DTOs) need changes.

[Files]
All changes are confined to existing Kotlin UI code in one file.

Existing files to be modified:
- `app/src/main/java/com/saiyan/dragonballuniverse/MainActivity.kt`
  - Extend `AnimeSeason` with optional `status`.
  - Update any `AnimeSeason(...)` instantiations to account for the new optional parameter where needed.
  - Update `PosterCard` implementation:
    - Convert from image-with-overlay to `Column` layout.
    - Top 80%: `AsyncImage` in a `Box` so we can overlay the badge at `Alignment.TopStart`.
    - Bottom 20%: info `Column` with background `Color(0xFF1A1A1A)` and padding `8.dp`.
    - Remove the existing bottom scrim gradient overlay in `PosterCard`.
  - Update the "دراغون بول Z" season shown in the UI (`dbzSeason`) to have `status = "مكتمل"`.

New files:
- None.

Files to be deleted or moved:
- None.

Configuration file updates:
- None.

[Functions]
Introduce a small helper to map status -> badge background color, and refactor `PosterCard` layout accordingly.

1) `private fun statusBadgeColor(status: String): Color`
- Signature:
  ```kotlin
  private fun statusBadgeColor(status: String): Color
  ```
- Purpose: Convert a trimmed status label into a background color.
- Parameters:
  - `status`: non-blank trimmed status label.
- Returns:
  - `Color` based on mapping rules (green/blue/orange/gray fallback).
- Key implementation details:
  - Use `when (status)` to match exact Arabic strings.
  - Use Compose `Color(...)` values (either `Color(0xFF...)` or `Color.Green/Blue` if preferred; ensure orange is `Color(0xFFFF9800)`-like).
- Error handling:
  - None; caller only calls when non-blank.

2) `@Composable private fun PosterCard(season: AnimeSeason, onClick: () -> Unit, modifier: Modifier = Modifier)`
- Purpose: Display a poster with an 80/20 split layout and an optional status badge.
- Behavior changes:
  - Use `Column(Modifier.fillMaxSize())` inside `Card`.
  - Top image section (80%):
    - `Box(Modifier.fillMaxWidth().weight(0.8f))`
    - `AsyncImage(modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, ...)`
    - If `season.status` is not null/blank:
      - Render badge `Box` aligned `Alignment.TopStart` with `padding(8.dp)`
      - Badge `background(color = statusBadgeColor(trimmed), shape = RoundedCornerShape(4.dp))`
      - Inner text with small font size and white color, plus internal padding (e.g. horizontal 8.dp, vertical 4.dp)
  - Bottom info section (20%):
    - `Column(Modifier.fillMaxWidth().weight(0.2f).background(Color(0xFF1A1A1A)).padding(8.dp), verticalArrangement = Arrangement.Center)`
    - Title: bold, white, maxLines (1–2 depending on available height)
    - Year: below in light gray (`Color(0xFFBDBDBD)` or `Color.White.copy(alpha=0.7f)`)
  - Remove the existing gradient scrim overlay (no `Brush.verticalGradient` in `PosterCard`).
- Error handling:
  - Keep `placeholder` and `error` painters for `AsyncImage` to avoid blank state.

3) Data update: `val dbzSeason = AnimeSeason(...)`
- Change:
  - Add `status = "مكتمل"` to the `dbzSeason` instance created inside `DragonBallHomeContent`.

[Changes]
Implement the requested UI upgrade by extending the `AnimeSeason` model with `status`, mapping status to badge color, and restructuring `PosterCard` into a split layout.

Step-by-step implementation plan:
1. Update `AnimeSeason` data class in `MainActivity.kt`:
   - Add `val status: String? = null` as the last parameter to preserve call-site readability and default behavior.
   - Ensure `AnimeSeasonSaver` includes `status` if the seasons are saved/restored (required because `NullableAnimeSeasonSaver` uses `AnimeSeasonSaver`).
     - Update the `save` list to append `season.status`.
     - Update the `restore` to read status (as `String?`) and pass it into `AnimeSeason(...)`.
2. Update all `AnimeSeason(...)` instantiations:
   - Existing static seasons can omit `status` (defaults to null).
   - Update `dbzSeason` inside `DragonBallHomeContent` to set `status = "مكتمل"`.
3. Add helper function `statusBadgeColor(status: String): Color` near other helpers (e.g., near `resolveImageUrl`).
   - Use exact mapping:
     - مستمر -> `Color(0xFF2E7D32)` (green)
     - مكتمل -> `Color(0xFF1565C0)` (blue)
     - قادم -> `Color(0xFFEF6C00)` (orange)
     - else -> `Color(0xFF616161)` (gray)
4. Refactor `PosterCard`:
   - Replace current `Box` overlay structure with `Column`.
   - Keep `Card` size and shape as-is (`160x240 dp`, `RoundedCornerShape(16.dp)`), unless later adjustments are needed.
   - Top image area:
     - `Box(weight(0.8f))` containing `AsyncImage(fillMaxSize())`
     - Overlay badge at `Alignment.TopStart` with outer `padding(8.dp)`.
   - Bottom info box:
     - `Column(weight(0.2f))` with background `#1A1A1A`, padding `8.dp`.
     - Title bold, year light gray below.
   - Remove the gradient scrim `Brush.verticalGradient` in `PosterCard`.
5. Compile & sanity check:
   - Fix any import changes (e.g., `Brush` may become unused after removing scrim; remove unused imports).
   - Ensure savers compile after adding `status`.
6. Manual verification (visual):
   - Launch app, go to Anime tab.
   - Verify DBZ poster shows badge "مكتمل" in top-start of image.
   - Verify image occupies ~80% and bottom info box occupies ~20% with background `#1A1A1A` and padded text.
   - Verify if `status` is null/blank for any other season, no badge is displayed.
   - Verify unknown status shows gray badge.

[Tests]
Use build verification plus a small manual UI smoke test for the Compose layout changes.

- Unit tests:
  - Optional: add a small unit test for `statusBadgeColor` mapping (not strictly required in this project; UI-only change).
- Integration/UI tests:
  - Optional future: Compose UI test asserting the badge exists when `status` is provided and not when missing.
- Manual test cases / edge cases:
  - `status = null` -> no badge.
  - `status = ""` or `"   "` -> no badge.
  - `status = "مستمر"` -> green badge.
  - `status = "مكتمل"` -> blue badge.
  - `status = "قادم"` -> orange badge.
  - `status = "غير معروف"` -> gray badge.
  - Long title -> verify maxLines prevents overflow into year.
  - Image load failure -> placeholder/error painter visible and badge still renders (if status exists).
