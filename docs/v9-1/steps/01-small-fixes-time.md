# Шаг 1: Мелкие правки и игровое время боя

- **Кому:** агент-разработчик KMP. Вид и жесты на устройстве проверяет владелец.
- **Зависит от:** нет.
- **Миграции:** нет (номера сверить с папкой миграций).

Шаг состоит из частей, делать по порядку, после каждой сборка, `ktlintFormat`, `detekt`. Один PR на весь шаг. Если часть оказалась слишком большой, остановиться и записать это в отчёт.

## Часть 1. Мелкие правки и ошибки
**Цель:** Исправить ошибки и мелкие правки без новых экранов: временные хиты между листом "Я" и боем, курсор в "Особенности", иконка "поделиться" и плавная панель упоминаний, универсальные тексты "Ресурсов", закрытие панели боя смахиванием, проверка, что настройки режима правки записи нет.

**Читать (только это):** `DEV.md`: эпик A (A.1-A.6). Вид: отдельных холстов нет, вид текущего приложения; иконка и анимация по аналогии с соседними экранами. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/presentation/MeEventHandler.kt, MeSheetFigures.kt, MeTextBlocks.kt, MeResourcesTab.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/round/RoundPanelUi.kt, PanelHpBlock.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/session/presentation/detail/SessionDetailHeader.kt, SessionDetailMentions.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/preferences/AppPreferences.kt (режим правки, FORM убран хотфиксом)`

**Миграция и тесты:** миграций нет. Тесты на контроллеры, если логика вынесена; тест, что изменение временных хитов видно с обеих сторон.

**Не трогать:** поведение остальных вкладок листа и панели боя; формат хранения хитов.

**Приёмка:**
- Временные хиты, введённые в бою, видны в листе "Я" и обратно, и не теряются после завершения боя.
- Курсор в "Особенности" не прыгает при наборе.
- В деталях сессии иконка "поделиться" и плавная панель упоминаний.
- Панель участника закрывается смахиванием вниз.
- Тексты "Ресурсов" не сужены до классовых умений.

**Строки и словарь:** `me_resources_*`, `session_detail_*`, значения в `values` и `values-en`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.

## Часть 2. Игровое время и ход назад
**Цель:** Заменить часы реального времени игровым временем `(раунд - 1) × 6 с` и добавить кнопку "‹" (предыдущий ход) в карточку хода.

**Читать (только это):** `DEV.md`: эпик B (B.1, B.2). Вид: `Design Discussion`, раунд 7 (игровое время, пара "‹" "›"). `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/round/ElapsedFormat.kt, TurnOrder.kt, TurnPosition.kt, RoundActions.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/results/Duration.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/round/RoundTexts.kt, RoundHandler.kt, CombatRoundPresenter.kt`

**Миграция и тесты:** миграций нет. Тесты: формат времени (раунды 1, 4, 12, 100), `previousTurn` (в раунде, граница раунда, первый ход боя), итоги "N РАУНДОВ · M МИН".

**Не трогать:** список участников, метки, калькулятор; подсветку хода (шаг 3, часть 2).

**Приёмка:**
- В шапке "БОЙ · 18 с ?", время меняется только на смене раунда.
- "‹" возвращает ход, на первом ходе раунда возвращает раунд с тостом "Снова раунд N", на первом ходе боя неактивна.
- Итоги боя считают раунды целиком.

**Строки и словарь:** `combat_round_*`, `combat_time_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.
