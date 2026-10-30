# Шаг 5. Заметки записей: «+ Создать», `@`, связи

- **Кому:** агент-разработчик KMP (Compose, Circuit, Metro, SQLDelight). Ревью: QA-агент (сборка, ktlint, detekt, `ArchitectureTest`, тест миграции). Вид и жесты на устройстве проверяет владелец.
- **Цель:** строка «+ Создать «X»» в шитах выбора и в дропдауне `@`; `@`-упоминания работают в поле «Заметки» записей (форма и быстрая правка); после сохранения заметки приложение предлагает связать упомянутое с полем записи (с Undo, отказ запоминается).
- **Зависит от:** шаг 2 (панель действия 1.1, нужна 2.1), шаг 4 (иконки сущностей в строках и чипах).

## Задачи
- 1.4 `AppSelectSheet` получает необязательное действие «создать»: `DESIGN_V9.md:101-106`.
- 2.1 закреплённая строка «+ Создать запись» в дропдауне `@` (≤3 записей — с подписью, больше — компактно): `DESIGN_V9.md:138-146`.
- 2.2 `@` в поле «Заметки» записи: токены `@[Имя|n:12]`, чипы, сама запись не предлагается, вид без количества: `DESIGN_V9.md:147-156`.
- 2.3 связи из заметок: место у NPC → «Где встретить», NPC у места → «Кто здесь», NPC у квеста → «от кого»; чистая функция, отказы в БД: `DESIGN_V9.md:157-175`.

## Читать (только это)
| Файл | Строки | Зачем |
|---|---|---|
| `DESIGN_V9.md` | 27-39 | «Готовность задачи» |
| `DESIGN_V9.md` | 101-175 | задачи 1.4, 2.1, 2.2, 2.3 |
| `DESIGN_V9.md` | 581 (карта), 608 (миграция 2.3), 627 (тест), 668-669 (сценарий 7) | миграция, тесты, приёмка |
| `Players Codex v9.dc.html` | 1318-1324 (`fsV.canCreate`), 1544-1547 (`ov.isType`, `ov.hasCreate`) | строка «+ Создать «X»» в шитах |
| `Players Codex v9.dc.html` | 211-215 (`suggFoot.on/isBtn/isHint`), 1926-1927 (`many`, `few`, `createLabel`), 3213-3222 (`suggFoot`) | дропдаун `@`, закреплённая строка |
| `Players Codex v9.dc.html` | 1086 (`f.isArea`), 3771-3775 (`area('desc', …)`) | поле «Заметки» записи с `@` |
| `Players Codex v9.dc.html` | 905-915 (`onYes`), 1928-1940 (`linkSugs`, `toggleRel`, тост «Связано ·»), 3478 | предложения связи, «Связать» / «Не надо» |
| `v9 Design System.dc.html` | 89 (блоки «06 · КНОПКИ», «09 · ЭЛЕМЕНТЫ»: «Плашка-предложение», «Дропдаун @», «Чипы упоминаний») | вид плашки, кнопок «Связать»/«Не надо» |
| `CORE_COMPONENTS.md` | 55, 81, 122, 129, 188 | `AppSelectSheet`, `MentionChip`, `FloatingBar`, `AppToast`, `core/selectlist` |
| `CLAUDE.md` | 29, 77-98 | миграции, Circuit-границы, локализация |

## Код (где работать)
Корень: `shared/src/commonMain/kotlin/com/lyrismet/incadent/`.
- 1.4: `core/designsystem/component/AppSelectSheet.kt` (356 строк), `AppSelectSheetParts.kt`; потребители `feature/codex/presentation/form/PlaceSelectUi.kt:23,61`, `SpeciesSelectSheet.kt:25,43`, `form/CodexEntryFormUi.kt:402,410`, `quickedit/EntityCardSheet.kt:38,46`. Быстрое создание уже есть в `feature/codex/domain/repository/QuickRecords.kt`.
- 2.1: `feature/session/presentation/detail/MentionDropdown.kt` (134 строки), строка `mention_create_row_format` (`composeResources/values/strings.xml:42`), домен `feature/mention/domain/MentionSuggestions.kt` (`searchMentions`, `emptyMentionRows`).
- 2.2: форма `feature/codex/presentation/form/CodexNpcFormFields.kt`, `CodexEntryFormUi.kt`, `CodexEntryFormModel.kt`; быстрая правка `feature/codex/presentation/quickedit/QuickEditEditors.kt`; чипы `feature/mention/presentation/MentionDisplay.kt`, `MentionChip.kt`; карточка `feature/codex/presentation/summary/EntitySummary.kt`. Дропдаун из `feature/session/...` не импортировать, вынести в `feature/mention/presentation` (все его пакеты общие).
- 2.3: чистая функция и модель — `feature/mention/domain` или `feature/codex/domain` (новый файл), репозиторий отказов — интерфейс в `feature/codex/domain/repository`, реализация в `feature/codex/data` (`@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)`), таблица — новый `.sq` в `shared/src/commonMain/sqldelight/com/lyrismet/incadent/db/`.
- Архив только для чтения: `feature/codex/presentation/CodexReadOnly.kt`.

## Миграция и тесты
- Миграция: ориентир `3.sqm` (шаг 3 добавит `2.sqm`; сверить `ls shared/src/commonMain/sqldelight/migrations`), таблица отказов «запись + упоминание», тест на старой версии по образцу `shared/src/androidHostTest/kotlin/com/lyrismet/incadent/persistence/SchemaMigrationTest.kt`.
- Тесты (приложение В): предложения связей — пары по таблице, существующая связь не предлагается, отказ запоминается, запрет циклов для мест (`DESIGN_V9.md:627`). Парсер упоминаний: токены в заметках записей, удалённая запись → «?»-тег.

## Не трогать
- `BottomTabBar` и всё про вкладку «Я» (шаг 10), `4.sqm`/свои типы (шаг 6), `AppGraph`, `DataBindings` (только точечные регистрации), чужие префиксы `strings.xml`.

## Приёмка
- Сценарий 7 приложения Д (`DESIGN_V9.md:668`): `@Мирдейл` в заметках NPC → чип → «Связать» заполняет «Где встретить» → Undo → «Не надо» больше не спрашивает.
- В новой кампании с одной записью `@` показывает подписанную строку создания.
- «+ Создать «X»» есть в шитах мест, квестов, видов (типов существ — шаг 6).
- Сама запись в своих заметках не предлагается; в архиве предложений нет.

## Строки и словарь
- Префиксы `mention_*` (создание, подпись «Нет нужного? Напишите имя, и мы создадим запись»), `link_suggestion_*` (предложение, «Связать», «Не надо», тост «Связано · …»); `values` и `values-en`, словарь: место, нажмите.

## Готово, когда
- Выполнено всё из `DESIGN_V9.md:27-39`; шаг 5 отмечен `[x]` в приложении А (`DESIGN_V9.md:581`); в `FEATURES.md` раздел 19 отмечены пункты «+ Создать «X»», строка в дропдауне `@`, `@` в заметках записей, связи из заметок.
