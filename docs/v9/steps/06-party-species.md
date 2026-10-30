# Шаг 6. Отряд и виды: уровень остальным, плашка «я», свои типы

- **Кому:** агент-разработчик KMP (Compose, Circuit, Metro, SQLDelight). Ревью: QA-агент (сборка, ktlint, detekt, `ArchitectureTest`, тест миграции). Устройство: владелец (сценарии 10 и 11).
- **Цель:** при повышении своего уровня приложение предлагает повысить остальным (тост с Undo); в «Отряде» без метки «я» стоит плашка выбора своего персонажа; тип существа выбирается в шите со своими типами (создать, переименовать, удалить).
- **Зависит от:** шаг 5 (`AppSelectSheet` с «+ Создать «X»», 1.4). Токены и метка «я» не меняются, существующий флаг метки — `PartyMember.isPlayerCharacter`.

## Задачи
- 3.1 повышение уровня остальным: тост «Повысить уровень остальным (N)?» после сохранения своего уровня, чистая функция отбора, Undo: `DESIGN_V9.md:178-186`.
- 3.2 плашка «Выберите своего персонажа» в «Отряде», выбор ставит метку одному, Undo, крестик, возврат после импорта: `DESIGN_V9.md:187-192`.
- 3.3 свои типы существ: шит выбора, 14 встроенных + свои, переименование и удаление, миграция: `DESIGN_V9.md:193-204`.

## Читать (только это)
| Файл | Строки | Зачем |
|---|---|---|
| `DESIGN_V9.md` | 27-39 | «Готовность задачи» |
| `DESIGN_V9.md` | 178-204 | задачи 3.1-3.3 |
| `DESIGN_V9.md` | 582 (карта), 609 (миграция), 626 (тест), 675-676 (сценарии 10, 11) | миграция, тесты, приёмка |
| `Players Codex v9.dc.html` | 300 (`meBanner` рендер), 3917 (`meBanner` состояние), 2607-2609 (`mePick`), 1543 (`ov.isMe`) | плашка и шит выбора «я» |
| `Players Codex v9.dc.html` | 1544-1549 (`ov.isType/hasOwn/hasSrd/isTEdit`), 2609-2617 (`ctype`, `ctEdit`, подтверждение удаления), 1668 (`CTYPES`), 3774 (поле «ТИП СУЩЕСТВА») | шит типа, правка и удаление своего типа |
| `Players Codex v9.dc.html` | 3072, 3080, 3769 (`lvlFromMe`) | уровень «как у вашего персонажа» в форме |
| `v9 Design System.dc.html` | 89 (блоки «09 · ЭЛЕМЕНТЫ»: «Плашка-баннер», «Тост с отменой», «Подтверждение»; «07 · ПОЛЯ») | синяя плашка про «я», тост, диалог удаления |
| `CORE_COMPONENTS.md` | 55, 119, 124, 129 | `AppSelectSheet`, `EmptyState`, `ConfirmationDialog`, `AppToast` (`UndoToast`) |
| `CLAUDE.md` | 29, 77-98, 103 | миграции, Circuit-границы, локализация, комментарии |

## Код (где работать)
Корень: `shared/src/commonMain/kotlin/com/lyrismet/incadent/`.
- 3.1: модель `feature/codex/domain/model/PartyMember.kt` (`level`, `PartyStatRanges.level = 1..20`, `isPlayerCharacter`), `feature/codex/domain/repository/PartyRepository.kt`, `feature/codex/data/PartyRepositoryImpl.kt:61-107`, форма `feature/codex/presentation/form/CodexEntryFormController.kt`, быстрая правка `quickedit/QuickEditField.kt`. Чистая функция отбора — новый файл в `feature/codex/domain` (рядом с моделью). Тост: `core/toast/`, `core/undo/UndoController.kt` (окно Undo 5 с, `UNDO_WINDOW_MILLIS`).
- 3.2: список «Отряда» `feature/codex/presentation/CodexLists.kt`, `CodexUi.kt`, `CodexEmptyStates.kt`, `CodexPresenter.kt`, запрос `clearOtherPlayerCharacters` в `PartyRepositoryImpl.kt:73`. Тот же выбор потом использует экран «Я» (шаг 10): выносить в переиспользуемый шит, не в экран «Отряда».
- 3.3: модель `feature/codex/domain/model/Species.kt` (`creatureType: CreatureType`, 14 значений + `UNSPECIFIED`), таблица `sqldelight/.../db/Species.sq` (`creature_type TEXT AS CreatureType NOT NULL`), адаптер `persistence/Adapters.kt:27`, регистрация `persistence/AppDatabaseFactory.kt`, `feature/codex/data/SpeciesRepositoryImpl.kt`, `domain/grouping/SrdSpecies.kt`, `QuickRecords.kt`; чипы типа `form/CodexEntryFormController.kt:591,656` (`creatureTypeOptions`) заменить шитом; подписи `feature/codex/presentation/summary/SpeciesLabels.kt`, `creatureTypeLabels()`; потребители: `CodexPresenter.kt:497`, `CodexCards.kt:158`, `feature/combat/presentation/prep/PrepRows.kt:179`. Тип описывать `sealed` (встроенный или свой), не `String?` и не `Any`.
- Риск: импорт/экспорт читает тип строкой (`feature/campaign/data/file/RecordValidator.kt`, `feature/campaign/domain/exchange`); свои типы должны переживать файл. Сломать нельзя, довершает шаг 7.

## Миграция и тесты
- Миграция: ориентир `4.sqm` (шаги 3 и 5 заняли `2.sqm`, `3.sqm`; сверить папку `shared/src/commonMain/sqldelight/migrations`), таблица своих типов с `campaign_id`, значения типа у существующих видов не теряются; тест по образцу `shared/src/androidHostTest/.../persistence/SchemaMigrationTest.kt`.
- Тесты (приложение В): отбор для 3.1 — у кого уровень равен старому, павшие и без уровня пропущены, 20 не повышается; тест репозитория типов (создать, переименовать, удалить → вид остаётся с «—»).

## Не трогать
- Таблицы `origin_id`, надгробия, импорт (шаги 3, 7), экран «Я» и таб-бар (шаг 10), `AppGraph`, `DataBindings` (точечно), чужие префиксы `strings.xml`.

## Приёмка
- Сценарий 10 (`DESIGN_V9.md:675`): создать «Механизм», назначить виду, удалить → вид на месте, тип «—».
- Сценарий 11 (`DESIGN_V9.md:676`): поднять свой уровень → тост «Повысить уровень остальным (N)?» → нажатие повышает равных старому, Undo возвращает; некого повышать — тоста нет.
- В «Отряде» есть персонажи, метки нет → плашка; выбор ставит метку одному, Undo, ✕ скрывает.
- Удаление своего типа спрашивает подтверждение с перечнем видов.

## Строки и словарь
- Префиксы `party_level_up_*`, `me_pick_*`, `species_type_*`; `values` и `values-en`, плюрали для «(N)», словарь: хиты, отряд, нажмите, метка «я».

## Готово, когда
- Выполнено всё из `DESIGN_V9.md:27-39`; шаг 6 отмечен `[x]` в приложении А (`DESIGN_V9.md:582`); в `FEATURES.md` отмечены пункты «Свои типы существ», «Повысить уровень остальным» (раздел 19) и плашка выбора в «Нет метки» (раздел 15).
