package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppLiveDot
import com.lyrismet.incadent.core.designsystem.component.AppNoticeBar
import com.lyrismet.incadent.core.designsystem.component.AppSwitch
import com.lyrismet.incadent.feature.campaign.domain.model.LiveFight
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.exchange_live_fighters
import dndplayerscodex.shared.generated.resources.exchange_live_line_minutes
import dndplayerscodex.shared.generated.resources.exchange_live_line_round
import dndplayerscodex.shared.generated.resources.exchange_live_title_ongoing
import dndplayerscodex.shared.generated.resources.exchange_live_title_prep
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** the fight that is not over: a red glowing dot, what it is and a [note] on what happens to it */
@Composable
fun LiveFightCard(
    fight: LiveFight,
    note: String,
    modifier: Modifier = Modifier,
    checked: Boolean? = null,
    onToggle: () -> Unit = {},
) {
    val fighters = pluralStringResource(Res.plurals.exchange_live_fighters, fight.fighters, fight.fighters)
    val line =
        when {
            !fight.ongoing -> fighters
            fight.minutes == null -> stringResource(Res.string.exchange_live_line_round, fight.round, fighters)
            else -> stringResource(Res.string.exchange_live_line_minutes, fight.round, fight.minutes, fighters)
        }
    val title = if (fight.ongoing) Res.string.exchange_live_title_ongoing else Res.string.exchange_live_title_prep
    AppNoticeBar(tint = AppPalette.Maroon, modifier = modifier) {
        AppLiveDot()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(title),
                color = AppPalette.TextPrimary,
                style = AppText.rowTitle,
            )
            Text(text = line, color = AppPalette.TextDescription, style = AppText.rowSubtitle)
            Text(text = note, color = AppPalette.TextSecondary, style = AppText.footnote)
        }
        if (checked != null) {
            Spacer(Modifier.weight(1f))
            AppSwitch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}
