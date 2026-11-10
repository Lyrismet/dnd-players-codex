# Шаг 6: Заклинания и магия в бою

- **Кому:** агент по данным и Compose (лист "Я", бой). Вид и жесты на устройстве проверяет владелец.
- **Зависит от:** шаги 3, 4.
- **Миграции:** `17.sqm` (номера сверить с папкой миграций).

Шаг состоит из частей, делать по порядку, после каждой сборка, `ktlintFormat`, `detekt`. Один PR на весь шаг. Если часть оказалась слишком большой, остановиться и записать это в отчёт.

## Часть 1. Список заклинаний и блок "Заклинания" в листе "Я"
**Цель:** Модель заклинаний, общий блок "Заклинания" с плитками, кругами, списком и карточкой, трата ячейки и бесплатных применений в листе "Я".

**Читать (только это):** `DEV.md`: эпик G (G.1, G.2 без боя). Вид: `Design Discussion`, раунд 14. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/presentation/MeSpellsTab.kt, MeCounterHandler.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/codex/domain/model/CharacterSheet.kt, CharacterResource.kt`
- `shared/src/commonMain/sqldelight/com/lyrismet/incadent/db/CharacterSheet.sq, CharacterResource.sq`

**Миграция и тесты:** `17.sqm`: `character_spell`. Тест миграции. Тесты: трата ячейки, бесплатное применение до отдыха, концентрация.

**Не трогать:** ресурсы, записанные вручную; вкладка "Магия" в бою (шаг 6, часть 2).

**Приёмка:**
- Раскрытый круг показывает список, "Применить · N" тратит одним нажатием.
- Карточка заклинания с полным описанием, правка и удаление.
- Плитка "Заговоры / Фокусы" со значком "∞".

**Строки и словарь:** `me_spells_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.

## Часть 2. Вкладка "Магия" в бою и анимация ячеек
**Цель:** Тот же блок "Заклинания" как четвёртая вкладка панели своего персонажа в бою, общее состояние ячеек с "Я", метка концентрации, анимация ячеек и общий таймер анимаций.

**Читать (только это):** `DEV.md`: эпик G (G.2, G.3). Вид: `Design Discussion`, раунд 14. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/round/RoundPanelTabsUi.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/presentation/MeSpellsTab.kt (блок выносится в общий подпакет)`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/designsystem/component/FlameSweepEffect.kt`
- ArchitectureTest: список SHARED_PRESENTATION

**Миграция и тесты:** миграций нет. Тесты: трата в бою видна в "Я" и обратно, концентрация ставит метку и снимает прежнюю.

**Не трогать:** вкладки других участников (у них вкладки "Магия" нет), классовые ресурсы.

**Приёмка:**
- Вкладка "Магия" только у "я".
- Ячейки анимированы по таблице кругов, при "Уменьшить движение" спокойные.
- Применение концентрации ставит метку "◎ Название".

**Строки и словарь:** `combat_magic_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.
