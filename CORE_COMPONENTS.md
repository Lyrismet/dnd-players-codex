# CORE_COMPONENTS.md

Catalog of everything already built in `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/designsystem/`.

**Check this file before writing any new visual block** (a card, a pill, a segmented switch, an icon slot, a divider...).
If something here already covers the shape you need, use it with different parameters instead of hand-rolling
`clip`/`background`/`border` again. If two or more features end up needing a visual shape that isn't here yet,
that's the signal to add it here, not to copy-paste it a third time (see CLAUDE.md's "Recurring review feedback").

## Tokens

| Name | File | What it is |
|---|---|---|
| `AppPalette` | `AppPalette.kt` | raw color constants (`Gold`, `Surface`, `BorderSubtle`, `TextPrimary`, ...). Never hardcode a hex color in feature code - reference a palette value or a `StatusColor`/`GoldCursorBrush` below instead. |
| `StatusColor` | `StatusColor.kt` | `(foreground, background, border)` triple for one status. `NpcStatus.toStatusColor()`, `QuestStatus.toStatusColor()`, `LocationMentionColor` build these from a domain status. |
| `GoldCursorBrush` | `Brushes.kt` | the gold `SolidColor` cursor brush every hand-rolled `BasicTextField` uses - don't inline `SolidColor(AppPalette.Gold)` again. |
| `SentenceKeyboardOptions` | `TextInputDefaults.kt` | `KeyboardOptions` that capitalize sentences - pass it to every hand-rolled `BasicTextField` so the soft keyboard starts each sentence with a capital. |
| `Theme.kt` / `Type.kt` | - | Compose `MaterialTheme` wiring and the type scale. Reference `MaterialTheme.typography.*`, never a raw `TextStyle`. |

## Surfaces

| Component | File | Use for | Key params |
|---|---|---|---|
| `Modifier.appCard()` | `component/AppCard.kt` | the clip+tint+optional-border+optional-click "surface card" chrome - codex cards, session rows, fact grids, composer input, popovers. A **Modifier extension**, not a composable: apply it to whatever `Row`/`Column` you already have, then add your own padding after it. | `shape`, `background`, `border` (nullable - `null` means no border), `borderWidth`, `onClick` (nullable) |
| `IconBadge` | `component/IconBadge.kt` | fixed-size tinted slot with centered content - avatar initials, entity emblems, circular header/dialog action buttons (edit, close, "@"). | `size`, `shape` (`CircleShape` by default), `background`, `border` (nullable), `onClick` (nullable), `content` |
| `AppDialog` | `component/AppDialog.kt` | the generic centered-dialog shell (rounded, bordered, padded `Column`). `ConfirmationDialog` is built on this. | `borderColor`, `content` |
| `AppBottomSheet` | `component/AppBottomSheet.kt` | the modal bottom sheet shell (26dp top corners, gold edge, drag handle) used by every entity sheet and the entry form. Also hosts `SheetCloseButton` (built on `IconBadge`). | `onDismissRequest`, `content` |
| `CampaignRenameSheet` | `component/CampaignRenameSheet.kt` | the campaign rename sheet (Settings and the session list header) - an `AppBottomSheet` with the overline, serif title, 56dp input, hint and 52dp save. Save is disabled and refused while the name is blank (`core/campaign`'s `savableCampaignName`). The caller owns the draft state. | `draft`, `onDraftChanged`, `onSave`, `onDismiss` |
| `NumberPadSheet` | `component/NumberPadSheet.kt` | the quick numeric keypad that overlays a stepper's form sheet when tapped (P2.9 "КОДЕКС" calculator, Players Codex v6.dc.html `pad` with `purpose === 'form'`) - calculator icon header, expr/result/range-hint block, a 4×3 digit/⌫/C grid, and a `FormPrimaryButton` apply. No mode/target/source rows - those only exist for the future combat calculator (P3), which isn't wired yet. The caller (e.g. `CodexEntryFormController`) owns which field is open and its typed expression via `core/calc`'s `CalcExpression`; this component takes only pre-formatted strings and key callbacks. | `overline`, `title`, `expr`, `result`, `rangeHint`, `applyLabel`, `applyEnabled`, `onDigit`, `onBackspace`, `onClear`, `onApply`, `onClose` |

## Buttons, badges, chips

| Component | File | Use for | Key params |
|---|---|---|---|
| `HeaderActionButton` | `component/HeaderActionButton.kt` | small tinted rounded-rect button in a screen header ("+ Сессия", "Завершить"). | `text`, `onClick`, `foreground`/`background`/`border` |
| `FormPrimaryButton` | `component/FormFields.kt` | full-width gold 52dp CTA at the bottom of a form/sheet. | `text`, `onClick`, `enabled` |
| `TagChip` | `component/TagChip.kt` | small rounded label pill - generic tags, badges. | `text`, `foreground`/`background`/`border` |
| `StatusBadge` | `component/StatusBadge.kt` | `TagChip` pre-wired to a `StatusColor` - NPC/quest status badges. | `text`, `color: StatusColor` |
| `ChoiceChip` | `component/ChoiceChip.kt` | 30dp selectable pill for one value out of a row - codex group chips. The selected one fills gold. | `text`, `selected`, `onClick`, `modifier` |
| `MentionChip` | `component/MentionChip.kt` | the colored "●NPC name"/"▲Location"/"◆Quest" pill used inline in session notes and entity sheets. Also owns `MentionGlyph` (the canonical `●`/`▲`/`◆` symbols - reference `MentionGlyph.X.symbol`, never retype the glyph). | `item: MentionChipItem`, `onClick`, `fontSize`, `glyphSize`, `glyphGap`, `lineHeightFactor` |
| `FormChipPicker` | `component/FormFields.kt` | the pill-chip picker for a form field (status, relationship, "given by", "where") - every chip-type field uses this, never a segmented row. A selected option takes the status color it carries in `FormChipOption.selectedColor`, gold otherwise. Optional `quickAdd` appends a dashed-gold "+ New X" pill after the options (codex NPC "где встретить", quest "кто дал"/"где") that opens an inline name input - see `FormChipQuickAdd` below. `null` (the default) keeps every other caller's rendering exactly as before. | `label`, `options: List<FormChipOption<T>>`, `onClick`, `quickAdd: FormChipQuickAdd?` |
| `FormChipQuickAdd` | `component/FormChipQuickAdd.kt` | data-holder + the pill/input-panel it renders for `FormChipPicker`'s optional quick-add (Players Codex v6.dc.html `quick`/`quickCreate`, lines ~2041-2047, 766-767) - dashed pill → autofocused name field + gold ✓ / outlined ✕ + helper text, Enter saves like ✓. The caller (a presenter/controller) owns `isOpen`/`draft` and all four callbacks; this file only renders them. | `addLabel`, `isOpen`, `draft`, `placeholder`, `helperText`, `onOpen`, `onDraftChanged`, `onSave`, `onCancel` |

## Pickers / segmented controls

| Component | File | Use for | Key params |
|---|---|---|---|
| `SegmentedControl<T>` | `component/SegmentedControl.kt` | the rounded-rect tab-switcher chrome - codex filter tabs, the entry-type switch, the entity status picker. One bordered container of equal-width clickable items; background/border per item are callbacks so a status-colored picker and a flat gold-accent picker both fit. | `items`, `onSelected`, `itemBackground`, `itemBorder` (nullable), `containerBackground`/`containerBorder`, `itemHeight`, `itemSpacing`, `itemContent` |

Don't hand-roll a new `Row { items.forEach { Box(...).clickable{} } }` tab switcher - it's the third time that exact
shape got copy-pasted that triggered this doc's `SegmentedControl` extraction in the first place.

## Dividers / dots / misc primitives

| Component | File | Use for | Key params |
|---|---|---|---|
| `AppDivider` | `component/Divider.kt` | the plain 1dp rule between a header/footer and the content beside it. Full-width by default. | `color`, `thickness`, `modifier` (pass your own `weight`/`padding` modifier to override the full-width default) |
| `HeaderDivider` | `component/HeaderDivider.kt` | the ornamental short-gold-line + diamond + long-border-line rule under a screen title (built out of `AppDivider` + `Spacer`s). | - |
| `Dot` | `component/Dot.kt` | a plain solid circle at a given size/color - filter-chip status dots, the core of `GlowingDot` below, and an outlined circle (pass `color = Color.Transparent` with `borderColor`) like the codex search icon. | `size`, `color`, `borderColor` (optional), `borderWidth` (1dp), `content` (optional, for nesting) |
| `GlowingDot` | `component/GlowingDot.kt` | a `Dot` with a concentric flat-color glow ring - the "live session" indicator. | `dotSize`, `dotColor`, `glowColor`, `ringWidth` |
| `FlameSweepEffect` | `component/FlameSweepEffect.kt` | one-shot flame band that sweeps bottom-to-top over its content, with ember particles - the splash logo reveal. Wraps `content` and draws it twice, so keep the content cheap. | `play`, `modifier`, `content` |
| `BrandMark` | `component/BrandMark.kt` | the "sealed d8" brand mark (two locking halves) - the splash screen, and reusable for a future "about" screen or app-icon preview. | `size` (76dp), `tint` |
| `NumberLabel` | `component/NumberLabel.kt` | centered fixed-width roman-numeral index in front of a list row (session number, note number) - the font shrinks as the label gets longer, per `core/format`'s `NumberSizeLadder`. | `text`, `width` (44dp), `color`, `sizeLadder`, `lineHeightFactor` |
| `SectionOverline` | `component/SectionOverline.kt` | tracked all-caps single-line eyebrow label ("АРХИВ", "БАЗА ЗНАНИЙ"). The design uses 0.18em for page eyebrows, 0.16em for list sections and 0.14em for sheet and form labels, so pass the tracking. | `text`, `color`, `letterSpacing`, `fontSize`, `onClick`, `inlineContent` (sits right after the text, e.g. a quill), `trailingContent` |
| `ScreenHeader` | `component/ScreenHeader.kt` | overline + serif title + optional `HeaderDivider` - top of every top-level screen. `onOverlineClick` makes the overline editable (quill after the text). | `overline`, `title`, `onOverlineClick`, `overlineTrailingContent` |
| `EmptyStatePlaceholder` | `component/EmptyStatePlaceholder.kt` | centered secondary-text message for an empty list/search/loading state. | `text` |
| `EmptyStateActions` | `component/EmptyStateActions.kt` | the empty state of a list the user is meant to fill: serif title, hint and up to two buttons (`primaryAction` gold, `secondaryAction` outlined; each an `EmptyStateAction(label, onClick)`). Use it when there is something to do about the emptiness, `EmptyStatePlaceholder` when there isn't. | `title`, `text`, `primaryAction`, `secondaryAction` |
| `OrnamentedEmptyState` | `component/OrnamentedEmptyState.kt` | the "blank page" state of a screen meant to be filled: gold diamond, serif title and a hint (empty session feed). | `title`, `hint` |
| `SwipeToDeleteRow` | `component/SwipeToDeleteRow.kt` | swipe-left-to-reveal-delete row chrome, plus an optional swipe-right edit (dim gold, bright gold past the full-swipe point, quill and "Изменить" / "Отпустите"; only when `editAction` is passed - codex form mode). Never deletes anything itself - caller drives the real delete, which is usually immediate (see `UndoToast`) rather than a confirm dialog. `peekOffsetPx` adds an external, non-drag nudge on top of the row's own rest position - how the one-time swipe-hint peek pushes a specific row without touching its real drag state, see `SwipeHintBanner`. | `onDeleteRequested`, `deleteContentDescription`, `editAction: SwipeEditAction?`, `peekOffsetPx: Float`, `content` |
| `SwipeHintBanner` / `rememberSwipeHintPlayback` | `component/SwipeHintBanner.kt` | the one-time "swipe to delete/edit" teaching peek shown the first time the session list and the codex list each appear (sessions and codex-quick-mode are delete-only, codex form-mode also demos the right edit leg) - plain arrow-glyph plates matching the mockup exactly (gold "→" + maroon "←", 30×32dp each, or one maroon "‹‹" 40×32dp for delete-only - not the quill/bin icons the real swipe row uses), title/subtitle and a "Понятно" dismiss, floating above the bottom nav like `UndoToast`. `rememberSwipeHintPlayback(targetKey, direction)` drives the choreography (peek timings + the 9s auto-hide) and returns the `peekOffsetPx`/`bannerVisible` the caller feeds into `SwipeToDeleteRow` and this banner - the caller decides *when* (via `core/swipehint`'s `shouldStartSwipeHint`) and passes a fresh non-null `targetKey` every time the hint should (re)play, see `core/swipehint`'s `SwipeHintTarget`. | `visible`, `direction: SwipeHintDirection`, `title`, `subtitle`, `dismissLabel`, `onDismiss` |
| `UndoToast` | `component/UndoToast.kt` | the "X deleted · Отменить" bar that floats above the bottom tab bar for 5s after an optimistic delete or quick edit (sessions, entries, codex entities) - driven by `core/undo/UndoController`, not local screen state. The codex card hosts its own copy, since the sheet covers the app-level one. | `action: UndoAction?`, `onUndo` |
| `ConfirmationDialog` | `component/ConfirmationDialog.kt` | "are you sure?" dialog for a destructive action that has no undo (session/entry/codex deletes use `UndoToast` instead), built on `AppDialog` + `IconBadge`. | `title`, `text`, `onConfirm`, `onDismiss` |
| `Modifier.dismissKeyboardOnTap()` | `component/DismissKeyboardOnTap.kt` | clears focus/hides keyboard on an unconsumed tap - applied once at the app root. | - |
| `AppToast` | `component/AppToast.kt` | the plain floating message bar - the `UndoToast` chrome without an action, for a timed confirmation ("Кампания переименована"). The caller owns the timer and passes `text` (null hides it). | `text`, `modifier` |
| `HoldToEditField` | `component/HoldToEditField.kt` | press-and-hold wrapper for in-place editing: a translucent gold (16%) fill covers the full height and grows over 450 ms, released early it falls back in 150 ms ease. `onHeld` fires on completion, a drag past 8dp cancels, an early release reports through `LocalHoldTooShort`. `HoldFrame` presets (`Title`, `Description`, `FactLine`) carry the mockup's negative margins, paddings and radius. Scroll and swipe are not consumed. | `onHeld`, `frame: HoldFrame`, `content` |
| `RadioOptionRow` | `component/RadioOptionRow.kt` | the settings option row - 18px ring with a gold dot when selected, title and description; a selected row is gold-tinted. | `title`, `description`, `selected`, `onClick` |
| `HoldHintCard` | `component/HoldHint.kt` | the one-time gold hint row under a card header - pin glyph, text and a "Понятно" button. | `text`, `dismissLabel`, `onDismiss` |
| `HoldTooShortToast` | `component/HoldHint.kt` | the 2 s "hold to edit" nudge in the UndoToast style - the host bumps `trigger` on every too-short press. | `trigger`, `text` |

## Forms

| Component | File | Use for |
|---|---|---|
| `FormTextField` | `component/FormFields.kt` | single-line labeled text input matching the create/edit form style. |
| `FormTextArea` | `component/FormFields.kt` | multi-line variant of `FormTextField`, 3-line minimum height. |
| `FormChipPicker` | `component/FormFields.kt` | see "Buttons, badges, chips" above. |
| `FormStepper` | `component/FormFields.kt` | labeled integer field with −/+ buttons clamped to an `IntRange` - level, max hp, armor class, initiative bonus. Pass `onOpenCalculator` to make the centered value tappable and open `NumberPadSheet` above (only for ranges with a non-negative minimum - initiative bonus leaves it `null`). |
| `FormPrimaryButton` | `component/FormFields.kt` | see "Buttons, badges, chips" above. |

Every field in `CodexEntryFormUi.kt` is built from these five - if a new form needs a field type that doesn't fit,
add it here rather than reaching for a raw Material `TextField` in feature code.

## Navigation / icons

| Component | File | Use for |
|---|---|---|
| `BottomTabBar` / `BottomTabBarItem` | `component/BottomTabBar.kt` / `BottomTabBarItem.kt` | the persistent 4-column bottom nav bar. |
| `QuillIcon` | `component/icons/QuillIcon.kt` | the mockup's quill (the ✎ action of notes, the hold hint's pin, the swipe-right edit) - a square glyph in one tint. | `size`, `tint` |
| `AppIcons` | `component/icons/AppIcons.kt` | glyphs reused across 2+ features: `Edit` is the design's quill, `Delete` the stock Material bin. A single-use icon doesn't need an entry here - only add one once a second feature wants the same glyph. |
| `TabIconSlot` + `CodexTabIcon`/`CombatTabIcon`/`SessionsTabIcon`/`SettingsTabIcon` | `component/icons/` | the 4 hand-drawn bottom-nav glyphs, all sharing one fixed-height alignment slot. |

## Quick edit (codex card, `core/quickedit/` and `core/designsystem/component/EntitySummaryQuickEdit.kt`)

| Component | File | Use for | Key params |
|---|---|---|---|
| `EditorShield` | `core/quickedit/EditorShield.kt` | the transparent layer over the whole sheet while an editor is open - takes every touch except inside the open editor (`EditorHoleTracker` reports its rectangle), a tap on it cancels the edit. Scrolling inside the editor still scrolls the sheet. | `hole`, `onOutsideTap` (as `BoxScope` extension) |
| `InlineFieldEditor` | `core/quickedit/QuickEditEditors.kt` | the open editor that replaces a field's value - gold-ringed frame, label row with Отмена (ghost) / Готово (gold) buttons, then a bare input with the caret at the end of the text (`editorCaretAtEnd`) (serif title, Inter fields, 5-line description with hint). Enter / done saves a one-line input, Ctrl/Cmd+Enter a description, Esc cancels. | `label`, `draft`, `kind: InlineInputKind`, `onEvent`, `modifier` |
| `LinkOptionEditor` | `core/quickedit/QuickEditEditors.kt` | the editor of a link fact ("Кто дал", "Где") - Отмена header, then 38px pill buttons: "Не указано" and one per candidate, the linked one ✓ and in its entity's colours. | `label`, `options`, `selectedId`, `onEvent`, `modifier` |
| `FactLine` | `component/EntitySummaryQuickEdit.kt` | one fact row of a card - 104px label column, value; a held line turns into a full-width `InlineFieldEditor` or `LinkOptionEditor`. `QuickEditableValue` wraps the title and description the same way. | `fact`, `style`, `onEntityRefClicked`, `quick` |

## Entity sheet (built on the above, not a primitive itself)

`component/EntitySummarySheetBody.kt`'s `EntitySummarySheetContent` is the one entity-detail sheet shared by
`codex`, `sessionlist` and `sessiondetail` - it's composed entirely from `IconBadge`, `AppCard`, `SegmentedControl`,
`NumberLabel`, `MentionChip` and `SectionOverline` above. Quick edit is opt-in through `EntitySummarySheetActions.quickEdit`
(codex only): without it the card is read-only and status pickers stay interactive, with `statusesReadOnly` they collapse to a `StatusBadge`. If you need a new fact/relation/status block inside an
entity sheet, it almost certainly belongs as a case inside this file, not a new top-level component.

## Not a visual component - don't look here for UI shapes

`core/campaign` holds the campaign-name rule (`savableCampaignName`: trimmed, null when blank). `core/mention`, `core/format`, `core/entitysummary` hold **logic** (parsing, formatting, cross-entity mapping)
reused across 3+ features, not visual building blocks - see the root `CLAUDE.md` project overview for the
`core/<name>` vs `core/designsystem` split. `core/entitysummary`'s `EntitySheetInteractions` is the shared
tap/dismiss/status-update event-handling every presenter delegates to; it has no composable of its own.
`core/swipehint` holds the swipe-hint **decision** logic shared by the session list and codex presenters -
`shouldStartSwipeHint` (pending + a first row + nothing covering the screen) and `codexSwipeHintDirection`
(codex only grows the edit leg in form edit mode). The animation and the banner it decides to play live in
`SwipeHintBanner` above, not here. `core/calc`'s `CalcExpression` is the sum-of-terms keypad logic behind
`NumberPadSheet` (digit/plus/backspace/clear/value/display) - pure, no Compose, reused by the future combat
calculator (P3) once it's wired.

## When nothing here fits

Adding a genuinely new visual primitive is fine - this file just exists so "genuinely new" is a real judgment call,
not a guess. Add the new component under `core/designsystem/component/`, then add a row to this file in the same PR.
