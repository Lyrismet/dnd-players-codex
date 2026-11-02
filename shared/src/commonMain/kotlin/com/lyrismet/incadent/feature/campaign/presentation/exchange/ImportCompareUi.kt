@file:Suppress("TooManyFunctions")

package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppCheckBox
import com.lyrismet.incadent.core.designsystem.component.AppChevron
import com.lyrismet.incadent.core.designsystem.component.AppSearchField
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChoice
import com.lyrismet.incadent.feature.campaign.domain.model.Resolution
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.exchange_auto_fills
import dndplayerscodex.shared.generated.resources.exchange_auto_fills_sub
import dndplayerscodex.shared.generated.resources.exchange_auto_new
import dndplayerscodex.shared.generated.resources.exchange_auto_none
import dndplayerscodex.shared.generated.resources.exchange_auto_none_sub
import dndplayerscodex.shared.generated.resources.exchange_auto_twins
import dndplayerscodex.shared.generated.resources.exchange_auto_twins_off
import dndplayerscodex.shared.generated.resources.exchange_auto_twins_on
import dndplayerscodex.shared.generated.resources.exchange_compare_auto
import dndplayerscodex.shared.generated.resources.exchange_compare_badge_file
import dndplayerscodex.shared.generated.resources.exchange_compare_badge_mine
import dndplayerscodex.shared.generated.resources.exchange_compare_badge_mixed
import dndplayerscodex.shared.generated.resources.exchange_compare_changed
import dndplayerscodex.shared.generated.resources.exchange_compare_conflict_count
import dndplayerscodex.shared.generated.resources.exchange_compare_file_caps
import dndplayerscodex.shared.generated.resources.exchange_compare_keep_all
import dndplayerscodex.shared.generated.resources.exchange_compare_merge
import dndplayerscodex.shared.generated.resources.exchange_compare_mine_caps
import dndplayerscodex.shared.generated.resources.exchange_compare_need
import dndplayerscodex.shared.generated.resources.exchange_compare_need_sub
import dndplayerscodex.shared.generated.resources.exchange_compare_new
import dndplayerscodex.shared.generated.resources.exchange_compare_search
import dndplayerscodex.shared.generated.resources.exchange_compare_similar_count
import dndplayerscodex.shared.generated.resources.exchange_compare_similar_label
import dndplayerscodex.shared.generated.resources.exchange_compare_status
import dndplayerscodex.shared.generated.resources.exchange_compare_status_later
import dndplayerscodex.shared.generated.resources.exchange_compare_status_none
import dndplayerscodex.shared.generated.resources.exchange_compare_status_sub
import dndplayerscodex.shared.generated.resources.exchange_compare_summary
import dndplayerscodex.shared.generated.resources.exchange_compare_summary_short
import dndplayerscodex.shared.generated.resources.exchange_compare_take_all
import dndplayerscodex.shared.generated.resources.exchange_import_reading
import dndplayerscodex.shared.generated.resources.exchange_new_npc
import dndplayerscodex.shared.generated.resources.exchange_new_party
import dndplayerscodex.shared.generated.resources.exchange_new_place
import dndplayerscodex.shared.generated.resources.exchange_new_quest
import dndplayerscodex.shared.generated.resources.exchange_new_species
import dndplayerscodex.shared.generated.resources.exchange_records_count
import dndplayerscodex.shared.generated.resources.exchange_sessions_count
import dndplayerscodex.shared.generated.resources.exchange_similar_apart
import dndplayerscodex.shared.generated.resources.exchange_similar_later
import dndplayerscodex.shared.generated.resources.exchange_similar_same
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val GoldTint = AppPalette.Gold.copy(alpha = 0.35f)

/** the step after the pick: what the file brings measured against the campaign, and an answer for what is in doubt */
@Composable
internal fun CompareStep(state: ImportState) {
    val view = state.compare
    val sink = state.eventSink
    when {
        view == null -> Note(stringResource(Res.string.exchange_import_reading))
        view.nothingNew -> NothingNewStep(view) { sink(ImportEvent.Closed) }
        view.detail != null -> CompareDetailStep(view.detail, sink)
        else -> CompareList(view, sink)
    }
}

@Composable
private fun CompareList(
    view: CompareView,
    sink: (ImportEvent) -> Unit,
) {
    CompareSummary(view)
    view.personal?.let { PersonalSection(it, sink) }
    if (view.hasNeed) NeedSection(view, sink)
    AutoSection(view, sink)
    SessionsSection(view, sink)
    if (view.deleted.isNotEmpty()) DeletedSection(view.deleted, sink)
    CompareStatus(view) { sink(ImportEvent.ImportConfirmed) }
    BackLink { sink(ImportEvent.Back) }
}

@Composable
private fun CompareSummary(view: CompareView) {
    val news = pluralStringResource(Res.plurals.exchange_compare_new, view.newCount, view.newCount)
    val changed = pluralStringResource(Res.plurals.exchange_compare_changed, view.changedCount, view.changedCount)
    val text =
        if (view.sessionCount > 0) {
            val sessions =
                pluralStringResource(Res.plurals.exchange_sessions_count, view.sessionCount, view.sessionCount)
            stringResource(Res.string.exchange_compare_summary, news, changed, sessions)
        } else {
            stringResource(Res.string.exchange_compare_summary_short, news, changed)
        }
    Text(text = text, color = AppPalette.TextPrimary, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun NeedSection(
    view: CompareView,
    sink: (ImportEvent) -> Unit,
) {
    SectionOverline(
        text = stringResource(Res.string.exchange_compare_need, view.needTotal),
        color = AppPalette.GoldBright,
        style = OverlineStyle.Field,
        inlineContent = {
            Text(
                text = needSub(view),
                modifier = Modifier.padding(start = 8.dp),
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        },
    )
    view.similar.forEach { SimilarCard(it, sink) }
    if (view.conflicts.isNotEmpty() || view.needTotal > view.similar.size) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BulkButton(Res.string.exchange_compare_take_all) { sink(ImportEvent.AllFieldsChosen(FieldChoice.FILE)) }
            BulkButton(Res.string.exchange_compare_keep_all) { sink(ImportEvent.AllFieldsChosen(FieldChoice.MINE)) }
        }
    }
    if (view.hasSearch) {
        AppSearchField(
            query = view.query,
            onQueryChange = { sink(ImportEvent.QueryChanged(it)) },
            placeholder = stringResource(Res.string.exchange_compare_search),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (view.conflicts.isNotEmpty()) ConflictList(view.conflicts, sink)
}

@Composable
private fun needSub(view: CompareView): String {
    val similar = pluralStringResource(Res.plurals.exchange_compare_similar_count, view.similar.size, view.similar.size)
    val count = view.needTotal - view.similar.size
    val conflicts = pluralStringResource(Res.plurals.exchange_compare_conflict_count, count, count)
    return when {
        view.similar.isEmpty() -> conflicts
        count == 0 -> similar
        else -> stringResource(Res.string.exchange_compare_need_sub, similar, conflicts)
    }
}

@Composable
private fun RowScope.BulkButton(
    label: StringResource,
    onClick: () -> Unit,
) {
    AppButton(
        text = stringResource(label),
        onClick = onClick,
        modifier = Modifier.weight(1f),
        tone = ButtonTone.Neutral,
        variant = ButtonVariant.Tinted,
        size = ButtonSize.Large,
    )
}

@Composable
private fun SimilarCard(
    item: CompareSimilar,
    sink: (ImportEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, GoldTint)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.exchange_compare_similar_label),
            color = AppPalette.TextSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SimilarSide(Res.string.exchange_compare_mine_caps, item.mineName, Modifier.weight(1f))
            Text(text = "≈", color = AppPalette.TextTertiary, style = MaterialTheme.typography.bodyMedium)
            SimilarSide(Res.string.exchange_compare_file_caps, item.fileName, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SimilarAnswer(item, Resolution.SAME, Res.string.exchange_similar_same, sink)
            SimilarAnswer(item, Resolution.APART, Res.string.exchange_similar_apart, sink)
            SimilarAnswer(item, Resolution.LATER, Res.string.exchange_similar_later, sink)
        }
    }
}

@Composable
private fun SimilarSide(
    label: StringResource,
    name: String,
    modifier: Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SectionOverline(stringResource(label), color = AppPalette.TextTertiary, style = OverlineStyle.Field)
        Text(
            text = name,
            color = AppPalette.TextHeading,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = AppFontSize.Medium),
        )
    }
}

@Composable
private fun RowScope.SimilarAnswer(
    item: CompareSimilar,
    answer: Resolution,
    label: StringResource,
    sink: (ImportEvent) -> Unit,
) {
    val on = item.choice == answer
    val quiet = answer == Resolution.LATER
    AppButton(
        text = stringResource(label),
        onClick = { sink(ImportEvent.SimilarAnswered(item.ref, answer)) },
        modifier = Modifier.weight(1f),
        tone = if (on) ButtonTone.Gold else ButtonTone.Neutral,
        variant = if (on || !quiet) ButtonVariant.Tinted else ButtonVariant.Ghost,
        size = ButtonSize.Large,
        contentColor = if (quiet && !on) AppPalette.TextSecondary else Color.Unspecified,
    )
}

@Composable
private fun ConflictList(
    rows: List<CompareConflict>,
    sink: (ImportEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, AppPalette.Border)
                .padding(start = 14.dp, end = 12.dp),
    ) {
        rows.forEachIndexed { index, row ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .appCard(AppShapes.Control, Color.Transparent, null, onClick = {
                            sink(ImportEvent.RecordOpened(row.ref))
                        }),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(
                        text = row.name,
                        color = AppPalette.TextHeading,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = AppFontSize.Medium),
                    )
                    Text(
                        text = row.fields.map { stringResource(it.label()) }.joinToString(", "),
                        color = AppPalette.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                ConflictBadge(row.badge)
                AppChevron()
            }
            if (index < rows.lastIndex) AppDividerLine()
        }
    }
}

@Composable
private fun ConflictBadge(badge: CompareBadge) {
    val (label, color, fill) =
        when (badge) {
            CompareBadge.MINE ->
                Triple(
                    Res.string.exchange_compare_badge_mine,
                    AppPalette.TextPrimary,
                    AppPalette.BorderSubtle,
                )
            CompareBadge.FILE ->
                Triple(
                    Res.string.exchange_compare_badge_file,
                    AppPalette.TextMuted,
                    AppPalette.SurfacePopover,
                )
            CompareBadge.MIXED ->
                Triple(
                    Res.string.exchange_compare_badge_mixed,
                    AppPalette.GoldBright,
                    AppPalette.Gold.copy(alpha = 0.13f),
                )
        }
    Text(
        text = stringResource(label),
        modifier = Modifier.appCard(AppShapes.Small, fill, null).padding(horizontal = 8.dp, vertical = 4.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun AutoSection(
    view: CompareView,
    sink: (ImportEvent) -> Unit,
) {
    SectionOverline(
        stringResource(Res.string.exchange_compare_auto),
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
        val rows = autoRows(view)
        rows.forEachIndexed { index, row ->
            AutoRow(row, onClick = row.onClick(sink))
            if (index < rows.lastIndex) AppDividerLine()
        }
    }
}

private data class AutoLine(
    val title: String,
    val sub: String,
    val check: Boolean? = null,
    val kind: AutoKind,
)

private enum class AutoKind { TWINS, FILLS, NEW, NONE }

private fun AutoLine.onClick(sink: (ImportEvent) -> Unit): (() -> Unit)? =
    if (kind == AutoKind.TWINS) ({ sink(ImportEvent.NameTwinsToggled) }) else null

@Composable
private fun autoRows(view: CompareView): List<AutoLine> {
    val rows = mutableListOf<AutoLine>()
    if (view.nameTwins > 0) {
        rows +=
            AutoLine(
                stringResource(Res.string.exchange_auto_twins, view.nameTwins),
                stringResource(
                    if (view.mergeNameTwins) Res.string.exchange_auto_twins_on else Res.string.exchange_auto_twins_off,
                ),
                view.mergeNameTwins,
                AutoKind.TWINS,
            )
    }
    if (view.fillFields > 0) {
        rows +=
            AutoLine(
                pluralStringResource(Res.plurals.exchange_auto_fills, view.fillFields, view.fillFields),
                stringResource(
                    Res.string.exchange_auto_fills_sub,
                    pluralStringResource(Res.plurals.exchange_records_count, view.fillRecords, view.fillRecords),
                ),
                null,
                AutoKind.FILLS,
            )
    }
    val fresh = view.newByKind.values.sum()
    if (fresh > 0) {
        rows += AutoLine(stringResource(Res.string.exchange_auto_new, fresh), newKindsText(view), null, AutoKind.NEW)
    }
    if (rows.isEmpty()) {
        rows +=
            AutoLine(
                stringResource(Res.string.exchange_auto_none),
                stringResource(Res.string.exchange_auto_none_sub),
                null,
                AutoKind.NONE,
            )
    }
    return rows
}

@Composable
private fun newKindsText(view: CompareView): String {
    val order =
        listOf(
            ExchangeKind.PLACE to Res.plurals.exchange_new_place,
            ExchangeKind.NPC to Res.plurals.exchange_new_npc,
            ExchangeKind.QUEST to Res.plurals.exchange_new_quest,
            ExchangeKind.SPECIES to Res.plurals.exchange_new_species,
            ExchangeKind.PARTY to Res.plurals.exchange_new_party,
        )
    return order
        .mapNotNull { (kind, plural) ->
            view.newByKind[kind]?.let { pluralStringResource(plural, it, it) }
        }.joinToString(" · ")
}

@Composable
private fun AutoRow(
    line: AutoLine,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .appCard(AppShapes.Control, Color.Transparent, null, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        line.check?.let { AppCheckBox(checked = it) }
        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(text = line.title, color = AppPalette.TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text(text = line.sub, color = AppPalette.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CompareStatus(
    view: CompareView,
    onMerge: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text =
                    if (view.hasNeed) {
                        stringResource(Res.string.exchange_compare_status, view.needDone, view.needTotal)
                    } else {
                        stringResource(Res.string.exchange_compare_status_none)
                    },
                color = AppPalette.TextHeading,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text =
                    stringResource(
                        if (view.undecided >
                            0
                        ) {
                            Res.string.exchange_compare_status_later
                        } else {
                            Res.string.exchange_compare_status_sub
                        },
                    ),
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        AppButton(
            text = stringResource(Res.string.exchange_compare_merge),
            onClick = onMerge,
            variant = ButtonVariant.Solid,
            size = ButtonSize.Primary,
            height = 52.dp,
        )
    }
}
