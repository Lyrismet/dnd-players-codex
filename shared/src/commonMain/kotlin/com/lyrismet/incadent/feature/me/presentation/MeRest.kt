package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppSwitch
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.sheet.SheetIcons
import com.lyrismet.incadent.feature.codex.domain.model.RestSummary
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_rest_button
import incadent.shared.generated.resources.me_rest_confirm
import incadent.shared.generated.resources.me_rest_hp_change_format
import incadent.shared.generated.resources.me_rest_hp_full
import incadent.shared.generated.resources.me_rest_join_format
import incadent.shared.generated.resources.me_rest_log_live_format
import incadent.shared.generated.resources.me_rest_log_none
import incadent.shared.generated.resources.me_rest_log_title
import incadent.shared.generated.resources.me_rest_nothing
import incadent.shared.generated.resources.me_rest_overline_format
import incadent.shared.generated.resources.me_rest_resources
import incadent.shared.generated.resources.me_rest_slots
import incadent.shared.generated.resources.me_rest_slots_resources
import incadent.shared.generated.resources.me_rest_summary_format
import incadent.shared.generated.resources.me_rest_temp_note
import incadent.shared.generated.resources.me_rest_title
import incadent.shared.generated.resources.me_rest_undo_hint
import incadent.shared.generated.resources.me_rest_with_temp_format
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** the campfire button beside the hit points, it is on every tab */
@Composable
internal fun MeRestButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.appCard(onClick = onClick).heightIn(min = 52.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon16(SheetIcons.Campfire, AppPalette.Warm)
        Text(
            stringResource(Res.string.me_rest_button),
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.TextPrimary,
        )
    }
}

/** the long rest: what it brings back in one line and whether to leave a note in the session */
@Composable
internal fun MeRestSheet(
    name: String,
    rest: MeOverlay.Rest,
    onEvent: (RestEvent) -> Unit,
) {
    val close = { onEvent(RestEvent.Closed) }
    AppBottomSheet(onDismissRequest = close) {
        AppSheetColumn(
            header = {
                AppSheetHeader(
                    title = stringResource(Res.string.me_rest_title),
                    onClose = close,
                    overline = stringResource(Res.string.me_rest_overline_format, name).uppercase(),
                )
            },
        ) {
            Text(
                restText(name, rest),
                style = MaterialTheme.typography.bodyLarge,
                color = AppPalette.TextPrimary,
            )
            RestLogRow(rest) { onEvent(RestEvent.LogToggled) }
            FormPrimaryButton(
                text = stringResource(Res.string.me_rest_confirm),
                onClick = { onEvent(RestEvent.Confirmed) },
            )
            Text(
                stringResource(Res.string.me_rest_undo_hint),
                modifier = Modifier.fillMaxWidth(),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RestLogRow(
    rest: MeOverlay.Rest,
    onToggle: () -> Unit,
) {
    val live = rest.liveSessionTitle
    Column {
        AppDivider(color = AppPalette.RowDivider)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.me_rest_log_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppPalette.TextPrimary,
                )
                Text(
                    if (live != null) {
                        stringResource(Res.string.me_rest_log_live_format, live)
                    } else {
                        stringResource(Res.string.me_rest_log_none)
                    },
                    style = AppText.footnote,
                    color = AppPalette.TextSecondary,
                )
            }
            AppSwitch(checked = rest.log, onCheckedChange = { if (live != null) onToggle() })
        }
        AppDivider(color = AppPalette.RowDivider)
    }
}

// one line from the parts of the rest that apply, the hit points first
@Composable
private fun restSummaryText(
    name: String,
    summary: RestSummary,
): String {
    val before = summary.hpBefore
    val after = summary.hpAfter
    val hp =
        when {
            summary.hpFull -> stringResource(Res.string.me_rest_hp_full)
            before != null && after != null -> stringResource(Res.string.me_rest_hp_change_format, before, after)
            else -> null
        }
    val counters = restCountersText(summary)
    val line =
        when {
            hp != null && counters != null -> stringResource(Res.string.me_rest_join_format, hp, counters)
            else -> hp ?: counters ?: stringResource(Res.string.me_rest_nothing)
        }
    return stringResource(Res.string.me_rest_summary_format, name, line)
}

@Composable
private fun restCountersText(summary: RestSummary): String? {
    val count = summary.resourceCount
    return when {
        summary.hasSpellSlots && count > 0 -> pluralStringResource(Res.plurals.me_rest_slots_resources, count, count)
        summary.hasSpellSlots -> stringResource(Res.string.me_rest_slots)
        count > 0 -> pluralStringResource(Res.plurals.me_rest_resources, count, count)
        else -> null
    }
}

// the note about temporary hit points goes in the same paragraph
@Composable
private fun restText(
    name: String,
    rest: MeOverlay.Rest,
): String {
    val summary = restSummaryText(name, rest.summary)
    return if (rest.hasTemp) {
        stringResource(
            Res.string.me_rest_with_temp_format,
            summary,
            stringResource(Res.string.me_rest_temp_note),
        )
    } else {
        summary
    }
}
