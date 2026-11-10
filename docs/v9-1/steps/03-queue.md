# Шаг 3: Очередь: неполная, правка, подсветка, средний уровень

- **Кому:** агент-разработчик Compose (бой). Вид и жесты на устройстве проверяет владелец.
- **Зависит от:** шаг 1.
- **Миграции:** нет (номера сверить с папкой миграций).

Шаг состоит из частей, делать по порядку, после каждой сборка, `ktlintFormat`, `detekt`. Один PR на весь шаг. Если часть оказалась слишком большой, остановиться и записать это в отчёт.

## Часть 1. Неполная очередь, строка очереди, средний уровень
**Цель:** Разрешить старт боя с неполной очередью (`?N`), показать строку состояния очереди над кнопкой и средний уровень отряда на подготовке, убрать остатки режима "по инициативе".

**Читать (только это):** `DEV.md`: эпик D (D.1, D.2, D.3). Вид: `Design Discussion`, раунд 3; `Design Discussion 3`, раунд 21; `Design Discussion 4`, раунд 23. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/round/StartReadiness.kt, TurnSetup.kt, TurnOrder.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/preparation/CombatPreparation.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/prep/PrepOrderUi.kt, PrepRows.kt, CombatPrepPresenter.kt, CombatPrepUi.kt`

**Миграция и тесты:** миграций нет. Тесты: плотная нумерация с вставкой, `?N` на хвосте, пустая очередь, добавление в бою, средний уровень (пусто, без уровней, дробное).

**Не трогать:** правку очереди в бою (шаг 3, часть 2), строки участников.

**Приёмка:**
- Старт возможен без очереди, `?N` на хвосте.
- Над кнопкой строка очереди, нажатие прокручивает к третьему этапу.
- Средний уровень пересчитывается при смене отметок.
- Режима по инициативе нет.

**Строки и словарь:** `combat_prep_queue_*`, `combat_prep_avg_level`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.

## Часть 2. Правка очереди и подсветка хода
**Цель:** Правка очереди в бою перетаскиванием за ручку, передача хода нажатием на плитку, закрепление `?N`, подсветка ходящего и выбранного.

**Читать (только это):** `DEV.md`: эпик D (D.4, D.5). Вид: `Design Discussion`, раунды 4-8. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/round/TurnOrder.kt, TurnPosition.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/round/RoundRowUi.kt, RoundEditor.kt, RoundHandler.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/designsystem/component/FlameSweepEffect.kt (образец анимации)`

**Миграция и тесты:** миграций нет. Тесты: операция "переставить" (указатель хода следует за человеком), закрепление `?N`, передача хода.

**Не трогать:** логику старта (шаг 3, часть 1), ячейки (шаг 6, часть 2).

**Приёмка:**
- Удержание ≡ поднимает строку, номера пересчитываются на лету, тост "Очередь изменена" с отменой.
- Нажатие на плитку с номером передаёт ход.
- Искра по рамке ходящего, без анимации сплошная золотая.

**Строки и словарь:** `combat_queue_edit_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.
