# Implementation Plan

[Overview]  
Implement Phase 1 of the Quiz overhaul by adding animated question transitions, an urgency-aware timer, sound effects for answer feedback, and difficulty-driven dynamic backgrounds, while keeping the existing MVVM + Room stats flow intact.

This phase focuses purely on “feel”: smooth UI motion, clear time pressure, immediate feedback, and stronger visual theming per question difficulty. It intentionally avoids Phase 2+ features (currency/lifelines/adaptive difficulty/Firebase/NTP/leaderboards) so the rollout is incremental and low-risk.

The current Quiz feature uses `QuizViewModel` (state machine via `QuizUiState` + `QuizSessionState`) and `QuizMainScreen` / `QuizPlayingContent` for UI, with local `dummyQuestions` and `Room` persistence via `UserStatsDao`. Phase 1 will extend the UI layer (Compose) and add a small audio utility, with minimal changes to domain logic. Where logic is required (e.g., “play success/fail sound”), it will be done in a UI-friendly, lifecycle-safe way.

Key principles:
- No change to quiz rules, question source, or persistence schema in Phase 1.
- Compose-only changes should be deterministic and keyed by `q.id` to avoid timer/animation glitches.
- Sounds are short SFX → use `SoundPool` (per user preference).
- Background selection is purely visual and derived from `QuizQuestion.difficulty`.

[Types]  
Introduce small Phase-1-only UI and audio support types without changing existing database entities.

New / updated types:

1) `enum class QuizDifficultyUiTier`
- Purpose: normalize difficulty strings into stable tiers for UI mapping.
- Values:
  - `EASY`
  - `MEDIUM`
  - `HARD`
  - `INSANE`
  - `UNKNOWN`
- Mapping rule:
  - From `QuizQuestion.difficulty` string:
    - `DIFF_EASY` → `EASY`
    - `DIFF_MEDIUM` → `MEDIUM`
    - `DIFF_HARD` → `HARD`
    - `DIFF_INSANE` → `INSANE`
    - else → `UNKNOWN`

2) `data class QuizBackgroundStyle`
- Purpose: provide a consistent container of background styling inputs for Compose.
- Fields:
  - `val colors: List<Color>` (2–3 colors for gradient)
  - `val accent: Color` (used for border/highlights)
  - `val overlayAlpha: Float` (0.0–1.0) optional dimming
- Validation:
  - `colors.isNotEmpty()`
  - `overlayAlpha` clamped to `[0f, 1f]`

3) `sealed class QuizSfx`
- Purpose: identify which sound to play.
- Variants:
  - `data object Correct : QuizSfx`
  - `data object Wrong : QuizSfx`

4) `interface SoundManager`
- Purpose: abstraction for sound playback, allowing future replacement/testing.
- Functions:
  - `fun play(sfx: QuizSfx)`
  - `fun release()`

5) `class SoundPoolSoundManager(...) : SoundManager`
- Purpose: SoundPool-backed implementation.

No changes in Phase 1 to:
- `UserStatsEntity`
- `UserStatsDao`
- `QuizSessionState` fields
- `QuizUiState` variants
- `QuizQuestion` schema

[Files]  
Modify the quiz UI and add a dedicated sound manager with raw audio resources.

Existing files to be modified:

1) `app/src/main/java/com/saiyan/dragonballuniverse/quiz/QuizScreen.kt`
- Add `AnimatedContent` around the question card + options so each question transition animates (slide/fade or scale/fade).
- Update timer UI:
  - Keep existing 15s logic but add:
    - urgency detection for last 5 seconds (remainingMs <= 5000)
    - `animateColorAsState` to shift progress color from normal (e.g., `GokuOrange`) to red.
    - blinking effect in last 5 seconds (alpha pulsing) using an infinite transition.
- Integrate SoundManager usage:
  - Create/remember `SoundPoolSoundManager` in `QuizPlayingContent` (or higher) and release via `DisposableEffect`.
  - On answer selection:
    - If correct → play `QuizSfx.Correct`
    - Else → play `QuizSfx.Wrong`
  - On time expiry treat as wrong → play `QuizSfx.Wrong` once.
- Add dynamic backgrounds driven by difficulty:
  - Replace the fixed `DarkBackground` in the playing screen container with a per-question background (e.g., gradient).
  - Ensure home/results screens remain unchanged (still `DarkBackground`) unless explicitly desired.

2) `app/src/main/java/com/saiyan/dragonballuniverse/quiz/QuizViewModel.kt`
- No behavioral changes required for Phase 1.
- Optional minimal addition (only if needed for cleaner UI triggers):
  - Expose last answer result in session state (NOT required; prefer local UI decision based on correctness already computed in `QuizScreen`).

3) `app/src/main/java/com/saiyan/dragonballuniverse/db/UserStatsDao.kt`
- No changes in Phase 1.

New files to be created:

1) `app/src/main/java/com/saiyan/dragonballuniverse/quiz/audio/SoundManager.kt`
- Define `QuizSfx`, `SoundManager` interface, and `SoundPoolSoundManager` implementation.

2) `app/src/main/java/com/saiyan/dragonballuniverse/quiz/ui/QuizBackgrounds.kt`
- Provide:
  - difficulty → `QuizBackgroundStyle` mapping
  - helper to normalize difficulty strings to `QuizDifficultyUiTier`

New resources to be created:

1) `app/src/main/res/raw/quiz_correct.mp3` (or `.wav` / `.ogg`)
2) `app/src/main/res/raw/quiz_wrong.mp3`

Configuration files to be modified:

1) `app/build.gradle.kts`
- Ensure `android {}` has `buildFeatures { compose = true }` already (likely present).
- Add nothing for SoundPool (platform API), but ensure `res/raw` inclusion is standard (no config needed).
- If `AnimatedContent` requires a newer Compose animation artifact than currently used, bump Compose BOM / versions in:
  - `gradle/libs.versions.toml` and/or `app/build.gradle.kts`
  - Only if compilation reveals missing symbols.

Files to be deleted/moved:
- None in Phase 1.

[Functions]  
Add utility functions for difficulty mapping/background selection and sound playback lifecycle.

1) `fun difficultyToUiTier(difficulty: String): QuizDifficultyUiTier`
- Purpose: Convert `QuizQuestion.difficulty` to a stable enum.
- Params:
  - `difficulty: String`
- Returns:
  - `QuizDifficultyUiTier`
- Behavior:
  - Match against `DIFF_EASY`, `DIFF_MEDIUM`, `DIFF_HARD`, `DIFF_INSANE`, else `UNKNOWN`.

2) `fun backgroundStyleFor(tier: QuizDifficultyUiTier): QuizBackgroundStyle`
- Purpose: Map tier to gradient + accent palette.
- Params:
  - `tier: QuizDifficultyUiTier`
- Returns:
  - `QuizBackgroundStyle`
- Suggested palettes (example):
  - EASY: green/teal gradient, gentle accent
  - MEDIUM: blue/purple gradient
  - HARD: orange/red gradient
  - INSANE: deep red/purple/black gradient with stronger accent

3) `@Composable fun QuizDifficultyBackground(tier: QuizDifficultyUiTier, modifier: Modifier = Modifier, content: @Composable () -> Unit)`
- Purpose: Apply background consistently in one place.
- Behavior:
  - Use `Brush.linearGradient` / `Brush.radialGradient`.
  - Add subtle overlay for readability if needed.
  - Render `content()` on top.

4) `class SoundPoolSoundManager(context: Context) : SoundManager`
- Key functions:
  - `override fun play(sfx: QuizSfx)`
    - loads (or preloads) sound IDs for correct/wrong
    - plays with reasonable volume (e.g., 1f left/right)
    - handles “not loaded yet” edge: preload in init, ignore play until loaded or keep a fallback load listener
  - `override fun release()`
    - `soundPool.release()`
- Error handling:
  - If audio fails to load/play, swallow exception and continue gameplay (no crashes).
  - Avoid repeated loads on recomposition: instantiate in `remember` and clean up in `DisposableEffect`.

5) `@Composable fun rememberSoundManager(): SoundManager`
- Purpose: Compose-friendly factory.
- Behavior:
  - uses `LocalContext.current`
  - `remember { SoundPoolSoundManager(context) }`
  - `DisposableEffect(Unit) { onDispose { release() } }`

6) Timer UI helpers in `QuizScreen.kt`:
- `val isUrgent = remainingMs <= 5000`
- `val targetColor = if (isUrgent) Color.Red else GokuOrange`
- `val animatedColor by animateColorAsState(targetColor, ...)`
- Blink:
  - `val alpha by infiniteTransition.animateFloat(...)`
  - Apply alpha only when urgent.

[Changes]  
Implement Phase 1 by refactoring `QuizPlayingContent` to wrap question content in `AnimatedContent`, augmenting the timer composable with urgency animation, adding a SoundPool manager, and applying a difficulty-based background wrapper.

Step-by-step plan:

1) Add audio resources
- Create `app/src/main/res/raw/quiz_correct.*` and `quiz_wrong.*`.
- Keep files small (short SFX).
- Verify they’re packaged by building the app.

2) Create the sound manager abstraction
- Add `quiz/audio/SoundManager.kt`:
  - `QuizSfx`, `SoundManager`, `SoundPoolSoundManager`.
- Implementation details:
  - Use `SoundPool.Builder().setMaxStreams(2).build()`.
  - `load(context, resId, 1)` for correct/wrong.
  - Optionally track loaded state using `setOnLoadCompleteListener`.
  - `play(soundId, 1f, 1f, 1, 0, 1f)`.

3) Create difficulty → UI tier + background mapping
- Add `quiz/ui/QuizBackgrounds.kt`:
  - `QuizDifficultyUiTier`
  - `difficultyToUiTier`
  - `backgroundStyleFor`
  - `QuizDifficultyBackground` composable
- Ensure background is only applied in playing mode so Home/Results remain stable.

4) Update `QuizPlayingContent` to apply dynamic background
- In `QuizMainScreen`, inside `QuizUiState.Playing`, or inside `QuizPlayingContent`:
  - compute `val tier = difficultyToUiTier(q.difficulty)`
  - wrap existing `Column` with `QuizDifficultyBackground(tier) { ... }`
- Make sure padding/readability remain good.

5) Implement AnimatedContent for question transitions
- Wrap the question + answers region in:
  - `AnimatedContent(targetState = q.id, transitionSpec = { ... }) { ... }`
- Use `q.id` (or `session.currentIndex`) as the target state key so animation triggers on next question.
- Inside the animated block, render:
  - question `Card`
  - answer buttons
- Keep timer outside animated block if you want timer to reset cleanly per question; or key timer state by `q.id` as currently done (it is keyed by `remember(q.id)`), which is correct.

6) Upgrade timer to urgency-aware color + blink
- Derive `remainingMs` already exists.
- Add:
  - `isUrgent` when `remainingMs <= 5000`.
  - `animatedColor` via `animateColorAsState`.
  - blink alpha via `rememberInfiniteTransition` only when urgent (or always but apply conditionally).
- Apply `color = animatedColor.copy(alpha = alphaIfUrgent)` to `LinearProgressIndicator`.

7) Add sound playback triggers
- Instantiate `SoundManager` once per playing session:
  - `val soundManager = rememberSoundManager()`
- On answer click:
  - compute `isCorrect`
  - call `soundManager.play(if (isCorrect) QuizSfx.Correct else QuizSfx.Wrong)`
  - then call `onAnswer(index)` as today.
- On time expiry:
  - right before `onTimeExpired()`, call `soundManager.play(QuizSfx.Wrong)`
  - ensure it fires only once by guarding with `localAnswered`.

8) Verify lifecycle + recomposition safety
- Ensure `SoundPool` is released when leaving playing screen:
  - via `DisposableEffect` inside `rememberSoundManager`.
- Ensure timer resets correctly on question change (already keyed by `q.id`).
- Ensure no double sounds: keep `localAnswered` as the single gate.

9) Build & run smoke checks
- Manual checks:
  - Animated transition occurs between questions.
  - Timer turns red and blinks in last 5 seconds.
  - Correct and wrong sounds play once.
  - Background changes with difficulty label.
  - No crashes when leaving quiz or rotating (if supported).

[Tests]  
Use a mix of unit tests for mapping utilities and Compose UI tests for basic rendering and state transitions, plus manual verification for audio/animation behavior.

- Unit tests:
  - `difficultyToUiTier` mapping for each `DIFF_*` and unknown.
  - `backgroundStyleFor` returns non-empty colors and valid alpha.
- Compose UI tests (if test infra exists):
  - Render `QuizPlayingContent` with a fake session and verify:
    - difficulty label shown
    - progress indicator exists
    - answer buttons count matches options
  - (Animations are hard to assert; focus on stable nodes/semantics.)
- Manual verification (required due to audio/animation):
  - Confirm SFX plays for correct/wrong and not multiple times.
  - Confirm timer urgency effect triggers at 5s.
  - Confirm transition is smooth and keyed by question changes.
