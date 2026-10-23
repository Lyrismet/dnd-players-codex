package com.lyrismet.incadent.feature.combat.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_card_overline
import dndplayerscodex.shared.generated.resources.combat_list_rounds
import dndplayerscodex.shared.generated.resources.combat_results_delete
import dndplayerscodex.shared.generated.resources.combat_results_meta
import dndplayerscodex.shared.generated.resources.combat_results_minutes
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** the red-edged card of a fight in a session's feed, the only action is to delete it */
@Composable
fun CombatFeedCard(
    item: CombatCardItem,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var logExpanded by remember { mutableStateOf(false) }
    val rounds = pluralStringResource(Res.plurals.combat_list_rounds, item.rounds, item.rounds)
    val meta =
        stringResource(
            Res.string.combat_results_meta,
            rounds,
            stringResource(Res.string.combat_results_minutes, item.minutes),
        )
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(background = AppPalette.Surface, border = AppPalette.Maroon.copy(alpha = 0.6f))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionOverline(
                text = stringResource(Res.string.combat_card_overline, meta.uppercase()),
                color = AppPalette.MaroonBright,
                modifier = Modifier.weight(1f),
            )
            Text(text = item.timeLabel, color = AppPalette.TextTertiary, fontSize = AppFontSize.Caption)
            OutcomeBadge(item.outcome)
        }
        Text(text = item.title, style = MaterialTheme.typography.titleLarge, color = AppPalette.TextHeading)
        CombatSummaryBody(item.summary, logExpanded, { logExpanded = !logExpanded })
        if (onDelete != null) {
            AppButton(
                text = stringResource(Res.string.combat_results_delete),
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                tone = ButtonTone.Maroon,
                variant = ButtonVariant.Ghost,
            )
        }
    }
}
