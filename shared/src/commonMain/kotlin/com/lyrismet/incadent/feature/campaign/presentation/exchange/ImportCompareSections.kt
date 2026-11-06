@file:Suppress("TooManyFunctions")

package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppNoticeBar
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppToggleCard
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.toShortDayMonthUpper
import com.lyrismet.incadent.core.localization.currentAppLanguage
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChange
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChoice
import com.lyrismet.incadent.feature.campaign.domain.model.RecordField
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.presentation.summary.creatureTypeLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.npcLifeLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.npcStatusLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.questStatusLabels
import com.lyrismet.incadent.feature.mention.domain.plainMentionText
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.exchange_compare_close
import dndplayerscodex.shared.generated.resources.exchange_compare_empty
import dndplayerscodex.shared.generated.resources.exchange_compare_sessions_label
import dndplayerscodex.shared.generated.resources.exchange_deleted_head
import dndplayerscodex.shared.generated.resources.exchange_deleted_label
import dndplayerscodex.shared.generated.resources.exchange_deleted_return
import dndplayerscodex.shared.generated.resources.exchange_deleted_returned
import dndplayerscodex.shared.generated.resources.exchange_deleted_sub
import dndplayerscodex.shared.generated.resources.exchange_detail_both
import dndplayerscodex.shared.generated.resources.exchange_detail_both_hint
import dndplayerscodex.shared.generated.resources.exchange_detail_conflicts
import dndplayerscodex.shared.generated.resources.exchange_detail_done
import dndplayerscodex.shared.generated.resources.exchange_detail_file
import dndplayerscodex.shared.generated.resources.exchange_detail_fill_label
import dndplayerscodex.shared.generated.resources.exchange_detail_fills_count
import dndplayerscodex.shared.generated.resources.exchange_detail_fills_label
import dndplayerscodex.shared.generated.resources.exchange_detail_mine
import dndplayerscodex.shared.generated.resources.exchange_detail_skip
import dndplayerscodex.shared.generated.resources.exchange_detail_take
import dndplayerscodex.shared.generated.resources.exchange_detail_text_suffix
import dndplayerscodex.shared.generated.resources.exchange_field_class
import dndplayerscodex.shared.generated.resources.exchange_field_creature_type
import dndplayerscodex.shared.generated.resources.exchange_field_description
import dndplayerscodex.shared.generated.resources.exchange_field_hit_points
import dndplayerscodex.shared.generated.resources.exchange_field_level
import dndplayerscodex.shared.generated.resources.exchange_field_life_state
import dndplayerscodex.shared.generated.resources.exchange_field_name
import dndplayerscodex.shared.generated.resources.exchange_field_nicknames
import dndplayerscodex.shared.generated.resources.exchange_field_notes
import dndplayerscodex.shared.generated.resources.exchange_field_price
import dndplayerscodex.shared.generated.resources.exchange_field_quantity
import dndplayerscodex.shared.generated.resources.exchange_field_race
import dndplayerscodex.shared.generated.resources.exchange_field_reward
import dndplayerscodex.shared.generated.resources.exchange_field_status
import dndplayerscodex.shared.generated.resources.exchange_field_subclass
import dndplayerscodex.shared.generated.resources.exchange_field_vulnerabilities
import dndplayerscodex.shared.generated.resources.exchange_new_sessions
import dndplayerscodex.shared.generated.resources.exchange_new_sessions_sub
import dndplayerscodex.shared.generated.resources.exchange_nothing_text_both
import dndplayerscodex.shared.generated.resources.exchange_nothing_text_records
import dndplayerscodex.shared.generated.resources.exchange_nothing_title
import dndplayerscodex.shared.generated.resources.exchange_personal_keep_device
import dndplayerscodex.shared.generated.resources.exchange_personal_other
import dndplayerscodex.shared.generated.resources.exchange_personal_own
import dndplayerscodex.shared.generated.resources.exchange_personal_own_choice
import dndplayerscodex.shared.generated.resources.exchange_personal_take_file
import dndplayerscodex.shared.generated.resources.exchange_records_count
import dndplayerscodex.shared.generated.resources.exchange_session_both_label
import dndplayerscodex.shared.generated.resources.exchange_session_merge
import dndplayerscodex.shared.generated.resources.exchange_session_merge_note
import dndplayerscodex.shared.generated.resources.exchange_session_overlap
import dndplayerscodex.shared.generated.resources.exchange_session_separate
import dndplayerscodex.shared.generated.resources.exchange_session_separate_note
import dndplayerscodex.shared.generated.resources.exchange_sessions_count
import dndplayerscodex.shared.generated.resources.exchange_take_as_mine
import dndplayerscodex.shared.generated.resources.exchange_take_as_mine_anon
import dndplayerscodex.shared.generated.resources.exchange_take_as_mine_hint
import dndplayerscodex.shared.generated.resources.exchange_take_vitals
import dndplayerscodex.shared.generated.resources.exchange_take_vitals_hint
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AppDividerLine() {
    AppDivider(color = AppPalette.SurfacePopover)
}

@Composable
internal fun NothingNewStep(
    view: CompareView,
    onClose: () -> Unit,
) {
    Text(
        text = stringResource(Res.string.exchange_nothing_title),
        color = AppPalette.TextHeading,
        style = MaterialTheme.typography.headlineSmall,
    )
    val records = pluralStringResource(Res.plurals.exchange_records_count, view.emptyRecords, view.emptyRecords)
    val text =
        if (view.emptySessions > 0) {
            val sessions =
                pluralStringResource(Res.plurals.exchange_sessions_count, view.emptySessions, view.emptySessions)
            stringResource(Res.string.exchange_nothing_text_both, records, sessions)
        } else {
            stringResource(Res.string.exchange_nothing_text_records, records)
        }
    Text(text = text, color = AppPalette.TextDescription, style = MaterialTheme.typography.bodyLarge)
    AppButton(
        text = stringResource(Res.string.exchange_compare_close),
        onClick = onClose,
        modifier = Modifier.fillMaxWidth(),
        tone = ButtonTone.Neutral,
        variant = ButtonVariant.Tinted,
        size = ButtonSize.Primary,
    )
}

@Composable
internal fun SessionsSection(
    view: CompareView,
    sink: (ImportEvent) -> Unit,
) {
    if (view.sessions.isEmpty() && view.newSessions.isEmpty()) return
    SectionOverline(
        stringResource(Res.string.exchange_compare_sessions_label),
        color = AppPalette.TextTertiary,
        style = OverlineStyle.Field,
    )
    view.sessions.forEach { SessionCard(it, sink) }
    if (view.newSessions.isNotEmpty()) {
        val language = currentAppLanguage()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .appCard(AppShapes.Card, AppPalette.Background, AppPalette.Border)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = stringResource(Res.string.exchange_new_sessions, view.newSessions.size),
                color = AppPalette.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text =
                    stringResource(
                        Res.string.exchange_new_sessions_sub,
                        view.newSessions.map { it.toShortDayMonthUpper(language).lowercase() }.joinToString(", "),
                    ),
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: CompareSession,
    sink: (ImportEvent) -> Unit,
) {
    val day = session.day.toShortDayMonthUpper(currentAppLanguage()).lowercase()
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, AppPalette.Gold.copy(alpha = 0.35f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${session.title} · $day",
                color = AppPalette.TextHeading,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.exchange_session_both_label),
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            text =
                stringResource(
                    Res.string.exchange_session_overlap,
                    session.overlap.mine,
                    session.overlap.theirs,
                    session.overlap.common,
                ),
            color = AppPalette.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        AppSegmentedTabs(
            tabs =
                listOf(
                    SegmentedTab(true, stringResource(Res.string.exchange_session_merge)),
                    SegmentedTab(false, stringResource(Res.string.exchange_session_separate)),
                ),
            selected = session.merge,
            onSelected = { sink(ImportEvent.SessionMerged(session.fileId, it)) },
            itemHeight = 40.dp,
            containerBackground = AppPalette.SurfaceVariant,
        )
        Text(
            text =
                if (session.merge) {
                    pluralStringResource(
                        Res.plurals.exchange_session_merge_note,
                        session.overlap.merged,
                        session.overlap.merged,
                    )
                } else {
                    stringResource(Res.string.exchange_session_separate_note)
                },
            color = AppPalette.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun DeletedSection(
    deleted: List<CompareDeleted>,
    sink: (ImportEvent) -> Unit,
) {
    SectionOverline(
        stringResource(Res.string.exchange_deleted_label),
        color = AppPalette.TextTertiary,
        style = OverlineStyle.Field,
    )
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, AppPalette.Border)
                .padding(start = 14.dp, end = 12.dp),
    ) {
        Column(modifier = Modifier.heightIn(min = 50.dp), verticalArrangement = Arrangement.Center) {
            Text(
                text = stringResource(Res.string.exchange_deleted_head, deleted.size),
                color = AppPalette.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(Res.string.exchange_deleted_sub),
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        deleted.forEach { item ->
            AppDividerLine()
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource(item.ref.kind.label())} · ${item.name}",
                    modifier = Modifier.weight(1f),
                    color = AppPalette.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                AppButton(
                    text =
                        stringResource(
                            if (item.brought) {
                                Res.string.exchange_deleted_returned
                            } else {
                                Res.string.exchange_deleted_return
                            },
                        ),
                    onClick = { sink(ImportEvent.BroughtBack(item.ref)) },
                    tone = ButtonTone.Neutral,
                    variant = ButtonVariant.Tinted,
                    size = ButtonSize.Medium,
                    contentColor = if (item.brought) AppPalette.EmeraldBright else AppPalette.GoldBright,
                )
            }
        }
    }
}

@Composable
internal fun PersonalSection(
    personal: ComparePersonal,
    sink: (ImportEvent) -> Unit,
) {
    if (personal.ownCopy) {
        Text(
            text = stringResource(Res.string.exchange_personal_own),
            color = AppPalette.TextHeading,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(Res.string.exchange_personal_own_choice),
            color = AppPalette.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        AppSegmentedTabs(
            tabs =
                listOf(
                    SegmentedTab(false, stringResource(Res.string.exchange_personal_keep_device)),
                    SegmentedTab(true, stringResource(Res.string.exchange_personal_take_file)),
                ),
            selected = personal.takeAsMine,
            onSelected = { sink(ImportEvent.OwnCopyChosen(it)) },
            itemHeight = 36.dp,
            containerBackground = AppPalette.Background,
        )
        return
    }
    AppNoticeBar(tint = AppPalette.Gold) {
        Text(
            text = stringResource(Res.string.exchange_personal_other),
            color = AppPalette.TextDescription,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    val name = personal.characterName
    AppToggleCard(
        title =
            if (name == null) {
                stringResource(Res.string.exchange_take_as_mine_anon)
            } else {
                stringResource(Res.string.exchange_take_as_mine, name)
            },
        hint = stringResource(Res.string.exchange_take_as_mine_hint),
        checked = personal.takeAsMine,
        onToggle = { sink(ImportEvent.TakeAsMineToggled) },
    )
    AppToggleCard(
        title = stringResource(Res.string.exchange_take_vitals),
        hint = stringResource(Res.string.exchange_take_vitals_hint),
        checked = personal.takeVitals,
        onToggle = { sink(ImportEvent.TakeVitalsToggled) },
    )
}

/** one record opened from the list of conflicts: a choice for every field, and the fields the file fills in */
@Composable
internal fun CompareDetailStep(
    detail: CompareDetail,
    sink: (ImportEvent) -> Unit,
) {
    val kind = detail.ref.kind
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(
            size = 40.dp,
            background = AppPalette.BorderSubtle,
            onClick = { sink(ImportEvent.RecordOpened(null)) },
        ) {
            Text(text = "‹", color = AppPalette.TextMuted, style = MaterialTheme.typography.titleMedium)
        }
        Column(modifier = Modifier.weight(1f)) {
            SectionOverline(detailOverline(kind, detail), style = OverlineStyle.Field)
            Text(text = detail.name, color = AppPalette.TextHeading, style = MaterialTheme.typography.titleLarge)
        }
    }
    detail.fields.forEach { FieldChoices(kind, detail.ref, it, sink) }
    if (detail.fills.isNotEmpty()) {
        SectionOverline(
            stringResource(Res.string.exchange_detail_fills_label),
            color = AppPalette.TextTertiary,
            style = OverlineStyle.Field,
        )
        detail.fills.forEach { FillRow(kind, detail.ref, it, sink) }
    }
    FormPrimaryButton(
        text = stringResource(Res.string.exchange_detail_done),
        onClick = { sink(ImportEvent.RecordOpened(null)) },
    )
}

@Composable
private fun detailOverline(
    kind: ExchangeKind,
    detail: CompareDetail,
): String {
    val conflicts = pluralStringResource(Res.plurals.exchange_detail_conflicts, detail.fields.size, detail.fields.size)
    val fills = detail.fills.size
    val tail = if (fills > 0) " · " + stringResource(Res.string.exchange_detail_fills_count, fills) else ""
    return "${stringResource(kind.label())} · $conflicts$tail".uppercase()
}

@Composable
private fun FieldChoices(
    kind: ExchangeKind,
    ref: com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef,
    item: CompareField,
    sink: (ImportEvent) -> Unit,
) {
    val change = item.change
    val label = stringResource(change.field.label())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionOverline(
            if (change.isLongText()) {
                stringResource(
                    Res.string.exchange_detail_text_suffix,
                    label,
                ).uppercase()
            } else {
                label.uppercase()
            },
            color = AppPalette.TextTertiary,
            style = OverlineStyle.Field,
        )
        ChoiceCard(
            Res.string.exchange_detail_mine,
            valueText(kind, change, change.mine),
            item.choice == FieldChoice.MINE,
        ) {
            sink(ImportEvent.FieldChosen(ref, change.field, FieldChoice.MINE))
        }
        ChoiceCard(
            Res.string.exchange_detail_file,
            valueText(kind, change, change.theirs),
            item.choice == FieldChoice.FILE,
        ) {
            sink(ImportEvent.FieldChosen(ref, change.field, FieldChoice.FILE))
        }
        if (change.isLongText()) {
            ChoiceCard(
                Res.string.exchange_detail_both,
                stringResource(Res.string.exchange_detail_both_hint),
                item.choice == FieldChoice.BOTH,
            ) { sink(ImportEvent.FieldChosen(ref, change.field, FieldChoice.BOTH)) }
        }
    }
}

private fun FieldChange.isLongText(): Boolean =
    isText && (field == RecordField.DESCRIPTION || field == RecordField.NOTES)

@Composable
private fun ChoiceCard(
    head: StringResource,
    text: String,
    on: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    AppShapes.Card,
                    if (on) AppPalette.Gold.copy(alpha = 0.08f) else AppPalette.Background,
                    if (on) AppPalette.Gold else AppPalette.Border,
                    onClick = onClick,
                ).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = (if (on) "● " else "○ ") + stringResource(head),
            color = if (on) AppPalette.GoldBright else AppPalette.TextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
        SelectionContainer {
            Text(text = text, color = AppPalette.TextDescription, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FillRow(
    kind: ExchangeKind,
    ref: com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef,
    fill: CompareFill,
    sink: (ImportEvent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, AppPalette.Border)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.exchange_detail_fill_label, stringResource(fill.change.field.label())),
                color = AppPalette.TextTertiary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = valueText(kind, fill.change, fill.change.theirs),
                color = AppPalette.TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        AppButton(
            text = stringResource(if (fill.taken) Res.string.exchange_detail_skip else Res.string.exchange_detail_take),
            onClick = { sink(ImportEvent.FillToggled(ref, fill.change.field)) },
            tone = ButtonTone.Neutral,
            variant = ButtonVariant.Ghost,
            size = ButtonSize.Medium,
            contentColor = if (fill.taken) AppPalette.TextSecondary else AppPalette.GoldBright,
        )
    }
}

/** one side as the person reads it: a status or a type by its label, a text as it is */
@Composable
private fun valueText(
    kind: ExchangeKind,
    change: FieldChange,
    raw: String,
): String {
    val empty = stringResource(Res.string.exchange_compare_empty)
    if (change.isText) return plainMentionText(raw).ifBlank { empty }
    return when (change.field) {
        RecordField.STATUS ->
            if (kind == ExchangeKind.QUEST) {
                questStatusLabels()[QuestStatus.entries.firstOrNull { it.name == raw }]
            } else {
                npcStatusLabels()[NpcStatus.entries.firstOrNull { it.name == raw }]
            }
        RecordField.LIFE_STATE -> npcLifeLabels()[NpcLifeState.entries.firstOrNull { it.name == raw }]
        RecordField.CREATURE_TYPE -> creatureTypeLabels()[CreatureType.entries.firstOrNull { it.name == raw }] ?: raw
        else -> null
    } ?: raw
}

@Suppress("CyclomaticComplexMethod")
internal fun RecordField.label(): StringResource =
    when (this) {
        RecordField.NAME -> Res.string.exchange_field_name
        RecordField.DESCRIPTION -> Res.string.exchange_field_description
        RecordField.STATUS -> Res.string.exchange_field_status
        RecordField.RACE -> Res.string.exchange_field_race
        RecordField.NICKNAMES -> Res.string.exchange_field_nicknames
        RecordField.VULNERABILITIES -> Res.string.exchange_field_vulnerabilities
        RecordField.NOTES -> Res.string.exchange_field_notes
        RecordField.HIT_POINTS -> Res.string.exchange_field_hit_points
        RecordField.CHARACTER_CLASS -> Res.string.exchange_field_class
        RecordField.SUBCLASS -> Res.string.exchange_field_subclass
        RecordField.LEVEL -> Res.string.exchange_field_level
        RecordField.REWARD -> Res.string.exchange_field_reward
        RecordField.LIFE_STATE -> Res.string.exchange_field_life_state
        RecordField.CREATURE_TYPE -> Res.string.exchange_field_creature_type
        RecordField.QUANTITY -> Res.string.exchange_field_quantity
        RecordField.PRICE -> Res.string.exchange_field_price
    }
