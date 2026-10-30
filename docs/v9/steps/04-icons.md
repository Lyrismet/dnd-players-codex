# Шаг 4. Иконки сущностей и интерфейса

- **Кому:** агент-разработчик KMP (Compose Multiplatform, `ImageVector`, Circuit, Metro). Ревью: QA-агент (сборка, ktlint, detekt, `ArchitectureTest`, тесты). Вид на экране проверяет владелец на устройстве.
- **Цель:** знаки `● ▲ ◆ ■ ○` заменены иконками-`ImageVector` везде, где рисуется тип записи (чипы `@`, карточки и списки кодекса, форма, дропдаун). Иконки интерфейса листа (хиты, КД, инициатива, сапог, медаль, глаз, костёр, монета, лупа) заведены в коде для шагов 10-12.
- **Блокер:** файла `pc-icons.js` в репозитории нет (`find` ничего не вернул). Семь иконок сущностей (`npc, location, quest, party, species, item, me`) лежат только там. В прототипе они — глифы шрифта `<pc-ico g="...">`: `npc` U+E001, `location` U+E002, `quest` U+E003, `party` U+E004, `species` U+E005, `item` U+E006 (`Players Codex v9.dc.html:1666`). Пока файла нет, делай иконки интерфейса и каркас; пути семи иконок сущностей не придумывать.
- **Расхождение с документом:** `DESIGN_V9.md:336-345` просит токены `s` и `i`. Токен `s` уже есть (`feature/mention/domain/MentionToken.kt:8,22`). Токен `i` без `EntityKind.ITEM` не добавить, `ITEM` вводится в шаге 8. В этом шаге `i` не делать, зафиксировать в PR.

## Задачи
- 5.1 набор иконок и размеры 12/16/24/48 dp: `DESIGN_V9.md:311-335`.
- 5.2 внедрение: `ImageVector` без файлов-ассетов, замена знаков везде, обновить `CORE_COMPONENTS.md`: `DESIGN_V9.md:336-345`.

## Читать (только это)
| Файл | Строки | Зачем |
|---|---|---|
| `DESIGN_V9.md` | 27-39 | «Готовность задачи» |
| `DESIGN_V9.md` | 309-345 | эпик 5 целиком |
| `DESIGN_V9.md` | 580 | строка шага в дорожной карте |
| `v9 Design System.dc.html` | 11 (блоки «03 · ИКОНКИ», «04 · ИНТЕРФЕЙС», цвета сущностей в «01 · ЦВЕТА») | размеры, чипы статуса, цвета, 27 встроенных `<svg>` интерфейса |
| `v9 Design System.dc.html` | 89 (блок «08 · ИНДИКАТОРЫ», иконки в кнопках), 14, 40, 66 | ещё 4 `<svg>` и вырез таб-бара |
| `Players Codex v9.dc.html` | 1666 | `TY`: тип → глиф, подпись |
| `CORE_COMPONENTS.md` | 31, 81, 102-107, 119 | `PortraitArch`, `MentionChip`, `Diamond`/`AppTriangle`/`Dot`, `EmptyState` |
| `CLAUDE.md` | 29, 114-125 | миграции (здесь нет), частые замечания ревью |

## Код (где работать)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/mention/presentation/MentionChip.kt:56-70` — `enum MentionGlyph(symbol: String)` с `●▲◆■○`, `EntityKind.mentionGlyph()`. Главная точка: символ заменить на иконку.
- Потребители `MentionGlyph.*.symbol`: `feature/codex/presentation/CodexCards.kt` (147, 242), `CodexLists.kt` (70), `CodexSpeciesList.kt` (38), `form/CodexEntryFormUi.kt` (204, 283, 320). Найти остальные: `grep -rn "\.symbol\|leadingGlyph" shared/src/commonMain`.
- Остальные знаки: `feature/codex/domain/grouping/CodexSubtitles.kt:9` (`PLACE_GLYPH "▲"`, домен: текст, а не иконка, решить, остаётся ли знак в строке подзаголовка), `feature/codex/presentation/style/StatusGlyph.kt:8`, `CodexCards.kt:320` (`AppTriangle`), `CodexComponents.kt:104,181`.
- Иконки интерфейса: `core/designsystem/component/icons/AppIcons.kt` (образец: `addPathNodes`, viewport 24). Новые файлы по образцу `SearchIcon.kt`, `CodexTabIcon.kt`. Сущности: `core/designsystem/component/icons/EntityIcons.kt` (предложение).
- Тесты: `shared/src/commonTest/.../feature/mention/presentation/MentionDisplayTest.kt` и `feature/mention/domain/*Test.kt` не должны сломаться.

## Миграция и тесты
- Миграции нет. Тесты: приложение В, строка «Токены упоминаний» (`DESIGN_V9.md:630`), здесь только `n p l q s`, старые токены валидны.

## Не трогать
- Таб-бар `BottomTabBar` и круг «Я» (шаг 10), токен `i` и `EntityKind.ITEM` (шаг 8), `AppGraph`, `DataBindings`, чужие префиксы `strings.xml`.

## Приёмка
- Знаков `●▲◆■○` как текста нет ни на одном экране (чипы, списки, форма, дропдаун).
- Форма несёт тип, цвет несёт статус; у вида и предмета цвет нейтральный серый (`#B4BAC6`, блок «01 · ЦВЕТА»).
- Иконки читаются на 12, 16, 24, 48 dp на тёмном фоне.
- Старые токены `@[Имя|n:12]` разбираются как раньше.
- Сценариев приложения Д нет, проверка визуальная владельцем.

## Строки и словарь
- Новых строк нет (подписи типов уже есть). Новые `contentDescription` — ключи с префиксом `icon_` в `values` и `values-en`.

## Готово, когда
- Выполнено всё из `DESIGN_V9.md:27-39`; шаг 4 отмечен `[x]` в приложении А (`DESIGN_V9.md:580`); пункты «Иконки сущностей» в `FEATURES.md` (раздел 19) отмечены после приёмки владельцем.
