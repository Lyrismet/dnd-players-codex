# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Kotlin Multiplatform (KMP) app targeting Android and iOS, sharing UI (Compose Multiplatform) and business logic through the `shared` module.

- `androidApp` — thin Android entry point; wires `shared` into an `Activity`. No business logic here.
- `iosApp` — Xcode project, the iOS entry point. `ContentView.swift` calls `MainViewControllerKt.MainViewController()` from `shared`'s compiled `.framework` to get the root `UIViewController`. SwiftUI code (if any) lives here, not in `shared`.
- `shared/src/commonMain` — almost everything, layered:
  - `domain/model`, `domain/repository` — plain models and repository interfaces, no platform/framework dependencies.
  - `data/repository` — repository implementations (SQLDelight-backed), `data/db` — SQLDelight driver/database wiring, `data/AppContainer.kt` — legacy manual DI container, being replaced by Metro (see "Dependency injection" below). Until the migration lands, new dependencies are still wired here, including registering each feature's UI on the `Circuit.Builder`.
  - `presentation/<feature>/` — one Circuit screen per feature: `Screen` + `CircuitUiState` + `CircuitUiEvent`, a `Presenter`, a `@Composable` UI, and a `Circuit.Builder` wiring extension (see boundary rule below).
  - `core/designsystem/` — pure visual building blocks (palette, type, reusable `@Composable`s) with no domain-model or repository awareness. Logic that maps, filters, or cross-references domain models for reuse across 3+ features (not one feature's own view state) gets its own `core/<name>` package instead — see `core/mention`, `core/format`, `core/entitysummary`. **Before writing any new visual block (a card, a pill, a segmented switch, an icon slot, a divider...) check [`CORE_COMPONENTS.md`](CORE_COMPONENTS.md)** — it catalogs everything already built here with params and file paths. Reuse with different parameters instead of re-deriving `clip`/`background`/`border` by hand; add a new entry there in the same PR if you add a genuinely new primitive.
- `shared/src/androidMain`, `shared/src/iosMain` — `actual` implementations of `commonMain`'s `expect` declarations (e.g. `Platform`, `DatabaseDriverFactory`). Put platform-only code here, not in `commonMain`.

Stack: Compose Multiplatform (UI), Circuit (navigation/presentation), Metro (DI), SQLDelight (local DB), Ktor (networking), Multiplatform Settings (prefs), kotlinx.serialization/coroutines/datetime. Firebase (online sharing) is planned, see "Planned: Firebase sharing".

### Architecture
Clean-architecture layering with unidirectional data flow (MVI-style via Circuit), single `shared` module, package-by-layer at the top and package-by-feature inside `presentation`:
- `presentation/<feature>` (Presenter + Ui, Circuit) -> `domain` (models + repository interfaces) <- `data` (SQLDelight-backed implementations). Dependencies point inward: `domain` knows nothing about `data`, `presentation` or any framework, `presentation` depends on `domain` interfaces only, never on `data`.
- `core/*` is shared cross-feature logic and the design system; it may depend on `domain`, never on `presentation` or `data`.
- State flows down as immutable `CircuitUiState`, user intent flows up as `CircuitUiEvent` through `eventSink`. Repositories expose `Flow` for reads and `suspend` functions for writes.
- The database is the single source of truth for the UI: Presenters observe repository flows, they never hold their own copy of persisted data.

### Dependency injection (Metro, no codegen)
Decision: **Metro** (`dev.zacsweers.metro`), a compile-time, KMP-native DI framework implemented as a Kotlin compiler plugin. Circuit's `circuit-codegen` is deliberately **not** used: it is a separate KSP processor, which would add a third build tool (Metro + KSP + codegen) with unverified compatibility with Kotlin 2.4.20 and the `com.android.kotlin.multiplatform.library` AGP plugin. Revisit only if hand-written factories become a real burden.

Why Metro:
- Compile-time graph validation (a missing binding is a build error naming the missing type, not a launch crash), works on Android and iOS/Kotlin Native from `commonMain`. The project is maintained by one developer with Claude and iOS cannot be smoke-tested in the cloud, so build-time errors matter more than brevity.
- Scoping and graph extensions (`@DependencyGraph`, `@GraphExtension`, `@SingleIn`) model the future signed-in-user and active-campaign scopes needed for Firebase (see below).
- Compatibility checked against the Metro compatibility table: Kotlin 2.4.20 is supported from Metro 1.2.0 (latest seen: 1.4.5). Metro is tied to the Kotlin compiler version, so **check https://github.com/ZacSweers/metro/blob/main/docs/compatibility.md before bumping Kotlin** and bump Metro together with it.
- Fallback if Metro cannot be made to build on both platforms: Koin (runtime resolution, errors only at launch). Alternative considered and not chosen: kotlin-inject-anvil (KSP-based, smaller ecosystem).

Target shape (rules for new code):
- One `@DependencyGraph` (`di/AppGraph.kt`) per app process. Platform-only inputs enter through `@DependencyGraph.Factory` as `@Provides` parameters (today `ObservableSettings`, created by `AppContainer` from `SettingsFactory`; `DatabaseDriverFactory` joins when the database moves into the graph). Once `AppContainer` is gone the graph is created in the platform entry points (`MainActivity`, `MainViewController`) and `App()` receives what it needs from it, not the whole graph.
- Constructor injection with `@Inject` everywhere. Repository implementations are bound to their `domain/repository` interface with `@ContributesBinding`, singletons with `@SingleIn(AppScope::class)`. Never inject a concrete `*Impl` into a Presenter, and never use a service-locator style `graph.get()` inside `presentation`.
- Circuit wiring stays hand-written, but the parameter lists go away: each feature's `<Feature>Circuit.kt` holds an `@Inject @ContributesIntoSet(AppScope::class)` `Presenter.Factory` and `Ui.Factory` that receive their repositories by constructor injection and build the Presenter for the matching `Screen`. The graph provides `Circuit` from `Set<Presenter.Factory>` and `Set<Ui.Factory>`. No `Circuit.Builder.addXUi` extensions and no repository lists in `AppContainer`.
- Wiring that is platform-specific (`DatabaseDriverFactory`, `SettingsFactory`, `ImageCompressor`) stays behind `expect`/`actual` and is provided into the graph from the platform source set.
- `domain` stays framework-free: no Metro annotations in `domain/model` or `domain/repository`. Annotations go on `data`, `presentation` and `core` implementation classes.
- Migration is incremental and not done yet. Pilot first: migrate `presentation/settings` only, confirm Android and iOS both build and run, then migrate the other features one at a time. Do not mix both styles inside one feature. Migrating a feature means moving it fully to `@Inject` factories and removing its parameters from `AppContainer`. `AppContainer` is deleted when the last feature is migrated.
- Migration status: `presentation/settings`, `presentation/sessionlist` and `presentation/sessiondetail` are migrated, together with the database, every repository, `UndoController`, `ImageCompressor` and `SwipeHintReplayController`, which now live in the graph (`di/AppGraph.kt`, `di/DataBindings.kt`). Only `presentation/codex` is still wired by hand in `AppContainer`, which reads what it needs from the graph. Reference shape for a migrated feature: `SettingsCircuit.kt` (simple) and `SessionDetailCircuit.kt` (uses the `Screen` argument). Presenters stay plain classes and keep their constructor, only the `<Feature>Circuit.kt` factory receives the injected repositories and passes them in. Verified in the Linux cloud environment: `:shared:compileAndroidMain`, `detekt`, `ktlintFormat` and `:shared:testAndroidHostTest`; the settings pilot was also built and run by the owner on iOS. Not yet verified on iOS after the session screens moved: build and smoke test on a Mac before migrating codex.
- Standalone `@Provides`/`@IntoSet` functions go in a `@BindingContainer @ContributesTo(AppScope::class) object` - Metro warns on a plain `@ContributesTo` interface.

### Planned: Firebase sharing (online sync of the local data)
Everything stored locally today (sessions, notes, NPCs, quests, locations, party, tags) will later be shared online through Firebase. Rules so today's code does not block that:
- Keep **local-first**: SQLDelight stays the source of truth and the only thing Presenters read. Remote sync is a separate layer that writes into the same database, so the UI and Presenters do not change when sync is added.
- Repository interfaces in `domain/repository` stay storage-agnostic. Add remote access behind separate interfaces (for example `RemoteDataSource<T>`, `AuthRepository`, `SyncRepository`) implemented in `data/remote`, not by adding Firebase calls to the existing `*RepositoryImpl`.
- Firebase is reached through a KMP SDK (candidate: GitLive `firebase-kotlin-sdk`, or native SDKs behind `expect`/`actual`) - not decided yet. Whichever is chosen, it lives in `data/remote` only; no Firebase types in `domain` or `presentation`.
- Prepare for sync metadata when adding or changing tables: stable globally unique ids (not local autoincrement ids), `updatedAt`, soft-delete flag instead of hard delete for shared entities, and an owning campaign id. Do not invent these columns ahead of the sync feature, just do not make choices that rule them out (for example id types tied to rowid).
- Sharing implies a campaign/user scope: signed-in user and active campaign are the natural Metro graph extension scopes, so campaign-scoped repositories get the campaign id by injection instead of every Presenter passing it around.
- Open decisions (not made yet, ask the user): conflict resolution strategy (last-write-wins vs per-field merge), auth providers, Firestore vs Realtime Database, offline behavior for shared campaigns.

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
- **`<Feature>Circuit.kt`** — a `Circuit.Builder` extension (`fun Circuit.Builder.addXUi(...): Circuit.Builder`) wiring the Presenter and Ui to the Screen. Registered in `AppContainer`'s `Circuit.Builder` chain until the feature is migrated to Metro, after which this file holds the `@ContributesIntoSet` factories instead of the `addXUi` extension.
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
- Before adding a new card/pill/segmented-switch/icon-slot/divider, check [`CORE_COMPONENTS.md`](CORE_COMPONENTS.md) for an existing one to reuse with different parameters.
- Don't copy-paste the same `when`-block across presenters for shared interactive state (e.g. the entity-sheet tap/dismiss/status-update/related-note flow) — extract it once into the relevant `core/<name>` package and have each presenter delegate to it.
- Logic that maps/queries domain models for reuse across 3+ features doesn't belong inside `designsystem/component`, even if a design-system component renders its output — give it its own `core/<name>`.
- Pre-built scaffolding (domain model + repository interface + SQLDelight schema) for a large feature tracked in FEATURES.md but not started is fine to merge without an `Impl` or `AppContainer` wiring.
- Pure logic added under `core/*` (parsing, formatting, mapping) needs a `commonTest` unit test alongside it — this repo went 5 PRs with only the generated placeholder tests before anyone noticed.

## Git commits
- Never create git commits (`git commit`). Prepare and stage changes, but leave committing to the user.
- Exception: committing is allowed when resolving merge conflicts (e.g. merging master into a feature branch).

## Mandatory Post-Edit Verification Workflow
After making any code changes or writing new files, you must automatically and sequentially execute these validation commands in the terminal:
1. Compile: `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64` for everyday feature work (the two release iOS framework links take ~8.5 min each and dominate build time; the iOS store upload is the only thing that needs them). Run the plain `./gradlew build` with those links only when preparing an iOS App Store / release build. Use a narrower task like `./gradlew :shared:compileCommonMainKotlinMetadata` for a quick check while iterating on `commonMain`-only changes.
2. Auto-format: `./gradlew ktlintFormat`
3. Static analysis: `./gradlew detekt`
- If any compile errors, ktlint violations, or detekt findings are reported, fix them immediately before marking the task as complete.
