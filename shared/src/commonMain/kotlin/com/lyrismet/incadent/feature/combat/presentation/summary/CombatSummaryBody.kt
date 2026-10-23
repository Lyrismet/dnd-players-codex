package com.lyrismet.incadent.feature.combat.presentation.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.Dot
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.combat.domain.results.Highlights
import com.lyrismet.incadent.feature.combat.domain.results.LogLine
import com.lyrismet.incadent.feature.combat.domain.results.ResultRow
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_results_col_dealt
import dndplayerscodex.shared.generated.resources.combat_results_col_member
import dndplayerscodex.shared.generated.resources.combat_results_col_taken
import dndplayerscodex.shared.generated.resources.combat_results_falls
import dndplayerscodex.shared.generated.resources.combat_results_falls_count
import dndplayerscodex.shared.generated.resources.combat_results_finishers
import dndplayerscodex.shared.generated.resources.combat_results_log_close
import dndplayerscodex.shared.generated.resources.combat_results_log_open
import dndplayerscodex.shared.generated.resources.combat_results_log_round
import dndplayerscodex.shared.generated.resources.combat_results_top
import org.jetbrains.compose.resources.stringResource

private val DealtWidth = 56.dp
private val TakenWidth = 72.dp

/** the table, the highlights and the log of a fight, [logExpanded] with [onLogToggle] left out hides the log link */
@Composable
fun CombatSummaryBody(
    summary: CombatSummary,
    logExpanded: Boolean,
    onLogToggle: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ResultsTable(summary.rows)
        HighlightChips(summary.highlights)
        if (onLogToggle != null && summary.log.isNotEmpty()) {
            LogToggle(logExpanded, onLogToggle)
            if (logExpanded) RoundLog(summary.log)
        }
    }
}

@Composable
private fun ResultsTable(rows: List<ResultRow>) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(background = AppPalette.Background)
                .padding(horizontal = 14.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColumnTitle(stringResource(Res.string.combat_results_col_member), Modifier.weight(1f), TextAlign.Start)
            ColumnTitle(stringResource(Res.string.combat_results_col_dealt), Modifier.width(DealtWidth), TextAlign.End)
            ColumnTitle(stringResource(Res.string.combat_results_col_taken), Modifier.width(TakenWidth), TextAlign.End)
        }
        rows.forEach { row ->
            AppDivider(color = AppPalette.SurfacePopover)
            ResultLine(row)
        }
    }
}

@Composable
private fun ColumnTitle(
    text: String,
    modifier: Modifier,
    align: TextAlign,
) {
    Text(
        text = text,
        modifier = modifier,
        style = AppText.fieldOverline,
        color = AppPalette.TextTertiary,
        textAlign = align,
        maxLines = 1,
    )
}

@Composable
private fun ResultLine(row: ResultRow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Dot(size = 6.dp, color = row.side.dotColor())
                Text(
                    text = row.name,
                    style = AppText.rowTitle.copy(fontSize = AppFontSize.Body),
                    color = AppPalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = row.result.text(),
                modifier = Modifier.padding(start = 12.dp),
                style = AppText.rowNote,
                color = row.result.color(),
            )
        }
        StatNumber(row.dealt, AppPalette.TextPrimary, Modifier.width(DealtWidth))
        StatNumber(row.taken, AppPalette.MaroonBright, Modifier.width(TakenWidth))
    }
}

@Composable
private fun StatNumber(
    value: Int,
    color: Color,
    modifier: Modifier,
) {
    Text(
        text = value.toString(),
        modifier = modifier,
        color = color,
        fontSize = AppFontSize.MediumPlus,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun HighlightChips(highlights: Highlights) {
    val lines = highlightLines(highlights)
    if (lines.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        lines.forEach { line ->
            Text(
                text = line,
                color = AppPalette.TextDescription,
                fontSize = AppFontSize.Caption,
                modifier =
                    Modifier
                        .appCard(shape = AppShapes.Badge, background = AppPalette.SurfacePopover, border = null)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun highlightLines(highlights: Highlights): List<String> {
    val top = highlights.topDamage?.let { stringResource(Res.string.combat_results_top, it.first, it.second) }
    val finishers =
        highlights.finishers.takeIf { it.isNotEmpty() }?.let {
            stringResource(Res.string.combat_results_finishers, it.joinToString(", "))
        }
    val falls =
        highlights.falls.takeIf { it.isNotEmpty() }?.let { list ->
            val names =
                list
                    .map { (name, count) ->
                        if (count > 1) stringResource(Res.string.combat_results_falls_count, name, count) else name
                    }.joinToString(", ")
            stringResource(Res.string.combat_results_falls, names)
        }
    return listOfNotNull(top, finishers, falls)
}

@Composable
private fun LogToggle(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text =
            stringResource(
                if (expanded) Res.string.combat_results_log_close else Res.string.combat_results_log_open,
            ),
        color = AppPalette.GoldBright,
        style = AppText.rowSubtitle,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
    )
}

@Composable
private fun RoundLog(log: List<LogLine>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        log.forEach { line ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.combat_results_log_round, line.round),
                    modifier = Modifier.width(28.dp),
                    color = AppPalette.TextTertiary,
                    fontSize = AppFontSize.Caption,
                    lineHeight = AppLineHeight.Row,
                )
                Text(
                    text = line.entry.text(),
                    modifier = Modifier.weight(1f),
                    color = AppPalette.TextDescription,
                    fontSize = AppFontSize.Caption,
                    lineHeight = AppLineHeight.Row,
                )
            }
        }
    }
}
