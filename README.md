# DnD Players Codex (Incadent)

A diary and codex for tabletop RPG players: session notes with `@` mentions, a codex of party, NPCs, quests and
places, and (planned) a combat tracker. Kotlin Multiplatform, Android and iOS, UI in Compose Multiplatform, data
stays on the device (SQLDelight).

## Layout

* [/androidApp](./androidApp) is the thin Android entry point.
* [/iosApp](./iosApp/iosApp) is the Xcode project, the iOS entry point (it hosts the shared Compose UI).
* [/shared](./shared/src) is almost all of the code:
  - [commonMain](./shared/src/commonMain/kotlin) groups code by feature (`feature/<name>/{domain,data,presentation}`),
    with cross-feature building blocks in `core/` (design system, formatting, undo, ...) and the Metro graph in `di/`.
  - [androidMain](./shared/src/androidMain/kotlin) and [iosMain](./shared/src/iosMain/kotlin) hold the `actual`
    implementations of platform-only code.
  - [commonTest](./shared/src/commonTest/kotlin) and [androidHostTest](./shared/src/androidHostTest/kotlin) hold the tests.

More detail: [CLAUDE.md](./CLAUDE.md) (architecture, DI, rules for code), [CORE_COMPONENTS.md](./CORE_COMPONENTS.md)
(reusable UI building blocks) and [FEATURES.md](./FEATURES.md) (feature map and what is done).

## Running the apps

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

## Checks

- Tests: `./gradlew :shared:testAndroidHostTest` and `./gradlew :shared:iosSimulatorArm64Test`
- Formatting: `./gradlew ktlintFormat` (check with `ktlintCheck`)
- Static analysis: `./gradlew detekt`
- Everyday build without the slow release iOS framework links:
  `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`
