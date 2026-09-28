# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Kotlin Multiplatform (KMP) app targeting Android and iOS, sharing UI (Compose Multiplatform) and business logic through the `shared` module.

- `androidApp` — thin Android entry point; wires `shared` into an `Activity`. No business logic here.
- `iosApp` — Xcode project, the iOS entry point. `ContentView.swift` calls `MainViewControllerKt.MainViewController()` from `shared`'s compiled `.framework` to get the root `UIViewController`. SwiftUI code (if any) lives here, not in `shared`.
- `shared/src/commonMain` — almost everything, layered:
  - `domain/model`, `domain/repository` — plain models and repository interfaces, no platform/framework dependencies.
  - `data/repository` — repository implementations (SQLDelight-backed), `data/db` — SQLDelight driver/database wiring, `data/AppContainer.kt` — manual DI container (no DI framework; wire new dependencies here, including registering each feature's UI on the `Circuit.Builder`).
  - `presentation/<feature>/` — one Circuit screen per feature: `Screen` + `CircuitUiState` + `CircuitUiEvent`, a `Presenter`, a `@Composable` UI, and a `Circuit.Builder` wiring extension (see boundary rule below).
  - `core/designsystem/` — pure visual building blocks (palette, type, reusable `@Composable`s) with no domain-model or repository awareness. Logic that maps, filters, or cross-references domain models for reuse across 3+ features (not one feature's own view state) gets its own `core/<name>` package instead — see `core/mention`, `core/format`, `core/entitysummary`.
- `shared/src/androidMain`, `shared/src/iosMain` — `actual` implementations of `commonMain`'s `expect` declarations (e.g. `Platform`, `DatabaseDriverFactory`). Put platform-only code here, not in `commonMain`.

Stack: Compose Multiplatform (UI), Circuit (navigation/presentation), SQLDelight (local DB), Ktor (networking), Multiplatform Settings (prefs), kotlinx.serialization/coroutines/datetime.

### Scaffolding ahead of a feature
Laying down a `domain/model`, a repository interface, and a SQLDelight schema for a large feature that's tracked in `FEATURES.md` but not started yet (e.g. `CombatScratchpad`/`Combatant` ahead of Бой) is intentional groundwork, not a half-finished implementation — it's fine for it to have no repository `Impl` and no `AppContainer` wiring yet. "No half-finished implementations" means don't leave a feature you're actively building partially wired; it doesn't mean every interface needs a caller the moment it's typed.

### Engineering & Architecture Principles
- **OOP & Clean Code**: Design modular, maintainable, and loosely coupled components. Encapsulate business logic cleanly.
- **DRY (Don't Repeat Yourself)**: Extract reusable Components, repositories, or extension functions; eliminate duplicated logic.
- **KISS (Keep It Simple, Stupid)**: Favor simple, readable implementations over premature over-engineering.
- **SOLID**: Respect single-responsibility, dependency inversion, and interface segregation, especially in Circuit Presenters and repositories.
- **Occam's Razor**: The simplest solution that accurately satisfies all requirements is the best one.

### UI vs Presenter boundary (Circuit)
Each feature under `presentation/<feature>/` is four files — see `presentation/sessionlist/{SessionListScreen,SessionListPresenter,SessionListUi,SessionListCircuit}.kt` for the reference shape:
- **`<Feature>Screen.kt`** — the `@Serializable` `Screen` (navigation key), the `CircuitUiState` data class (always ends with an `eventSink: (XEvent) -> Unit = {}` field), and the `sealed interface XEvent : CircuitUiEvent`.
- **`<Feature>Presenter.kt`** — a `Presenter<XState>` with `@Composable override fun present(): XState`. Owns all data loading (repository flows via `collectAsState`/`produceState`), async orchestration (`rememberCoroutineScope().launch { ... }`), and navigation (`Navigator.goTo`/`pop`). Builds and returns the `eventSink` `when`-block that reacts to `XEvent`.
- **`<Feature>Ui.kt`** — `@Composable fun XUi(state: XState, modifier: Modifier = Modifier)`. Pure render: reads `state`, dispatches user actions through `state.eventSink(XEvent...)`. Never touches a repository, `Navigator`, or a coroutine scope directly.
- **`<Feature>Circuit.kt`** — a `Circuit.Builder` extension (`fun Circuit.Builder.addXUi(...): Circuit.Builder`) wiring the Presenter and Ui to the Screen. Registered in `AppContainer`'s `Circuit.Builder` chain.
- A `XUi` composable may own at most one trivial piece of purely visual local state (e.g. `remember { mutableStateOf(false) }` for a transient animation/scroll flag) — anything more (loading, merging, async) belongs in the Presenter, not the Ui.
- Before finishing a new/changed Ui or Presenter, state its responsibility in one sentence without "and" (e.g. "renders the NPC list" / "loads and mutates the NPC list"). If a Ui's sentence needs "and manages X state" — move that into the Presenter.
- Compare footprint to sibling `presentation/<feature>` folders. If a new Presenter is doing meaningfully more than its siblings, that's a signal to split the screen.
- `CircuitUiState` should expose only what the Ui needs to render and react to (plain values + the single `eventSink`), never a repository reference or a `Navigator` — SOLID's ISP applied concretely.
- When 3+ presenters need identical interactive behavior around a shared `core/<name>` model (e.g. tap a mention chip → show the entity sheet → dismiss/update-status/follow a related note), extract the event handling into a plain class in that `core/<name>` package (see `EntitySheetInteractions` in `core/entitysummary`) that each presenter constructs and delegates its matching event branches to — don't let the same `when` branches get copy-pasted per presenter.
- Exception: `presentation/codex/CodexScreen.kt` is intentionally one Circuit screen with an internal, Presenter-owned sub-tab switch across 4 related entity types (Отряд/NPC/Квесты/Места), not 4 independent features - the design mock treats it as one screen with tab UI, not 4 screens, so splitting it would fight the source of truth for no benefit.

### Kotlin typing rules
- Strict, non-nullable-by-default typing. Avoid `Any`, unchecked `as` casts, and `!!` — prefer `as?` with explicit handling, or remove the need for the cast with a `sealed interface`/`sealed class`.
- Model exhaustive UI/domain states with `sealed interface`/`sealed class` rather than nullable flags or booleans — keeps `when` exhaustive without a defensive `else`.

### Comments
No more than 1 line, concise, and only where the code actually needs explaining (non-obvious logic, a workaround, a hidden constraint) — never restate what the code already says. Formatting: lowercase first letter, a single sentence, no trailing period, no em/en dashes (use a hyphen `-` instead), no `ё`, no colons, no semicolons.

### Static analysis & formatting
- **ktlint** owns formatting/style (official Kotlin code style, applied to `.kt` and `.kts` including Gradle build scripts). Never hand-format against what `./gradlew ktlintFormat` would produce.
- **detekt** owns code-quality rules (complexity, unused code, code smells). Project overrides live in `config/detekt/detekt.yml` (empty today — add rule overrides there as they come up, `buildUponDefaultConfig = true` is set so unmentioned rules keep detekt's defaults).
- detekt is pinned to `dev.detekt` `2.0.0-alpha.6` because Kotlin 2.4.20 has no stable detekt release yet. Bump the `detekt` version in `gradle/libs.versions.toml` once a stable 2.x build supports this Kotlin version — don't downgrade Kotlin to chase detekt stability instead, that risks breaking compatibility with Compose Multiplatform/Circuit/SQLDelight/Ktor versions that were pinned together.
- `build/generated/**` (SQLDelight, Compose resources) is excluded from ktlint in `shared/build.gradle.kts` via a direct `tasks.withType<BaseKtLintCheckTask> { exclude(...) }` — the ktlint extension's own `filter {}` block does not reliably exclude generated Kotlin-Multiplatform sources on its own (known ktlint-gradle limitation), keep both in place.
- `@Composable` functions are allowed PascalCase names via `.editorconfig` (`ktlint_function_naming_ignore_when_annotated_with = Composable`). The one non-Composable PascalCase exception, `MainViewController` (name required verbatim by the Swift caller in `iosApp`), uses a targeted `@Suppress("ktlint:standard:function-naming")` instead of loosening `.editorconfig` further.
- **Do not downgrade AGP** to silence IntelliJ IDEA's "incompatible AGP version" warning (IntelliJ's bundled Android plugin currently caps at AGP 9.0.0). Compose Multiplatform 1.12.1's Android artifacts require AGP 9.1+ — downgrading breaks the real Gradle build (`checkDebugAarMetadata` fails on ~14 dependencies). The warning is IDE-only; ignore it or update IntelliJ instead.

### Recurring review feedback — check before opening a PR
- Don't copy-paste the same `when`-block across presenters for shared interactive state (e.g. the entity-sheet tap/dismiss/status-update/related-note flow) — extract it once into the relevant `core/<name>` package and have each presenter delegate to it.
- Logic that maps/queries domain models for reuse across 3+ features doesn't belong inside `designsystem/component`, even if a design-system component renders its output — give it its own `core/<name>`.
- Pre-built scaffolding (domain model + repository interface + SQLDelight schema) for a large feature tracked in FEATURES.md but not started is fine to merge without an `Impl` or `AppContainer` wiring.
- Pure logic added under `core/*` (parsing, formatting, mapping) needs a `commonTest` unit test alongside it — this repo went 5 PRs with only the generated placeholder tests before anyone noticed.

## Git commits
- Never create git commits (`git commit`). Prepare and stage changes, but leave committing to the user.
- Exception: committing is allowed when resolving merge conflicts (e.g. merging master into a feature branch).

## Mandatory Post-Edit Verification Workflow
After making any code changes or writing new files, you must automatically and sequentially execute these validation commands in the terminal:
1. Compile: `./gradlew build` (use a narrower task like `./gradlew :shared:compileKotlinMetadata` for a quick check while iterating on `commonMain`-only changes, but run the full `./gradlew build` before considering the task done — it's the only command that also compiles `androidMain`/`iosMain`).
2. Auto-format: `./gradlew ktlintFormat`
3. Static analysis: `./gradlew detekt`
- If any compile errors, ktlint violations, or detekt findings are reported, fix them immediately before marking the task as complete.
