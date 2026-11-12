package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.dashedBottomRule
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_link_battle_only
import org.jetbrains.compose.resources.stringResource

private val UNDERLINE = Color(0xFF4A4D5C)
private val MARKS_ROOM = 112.dp

/** the name of the participant, dashed and with a pencil when it can be changed, the link mark and the role under it */
@Composable
internal fun PanelName(
    panel: RoundPanel,
    send: (CombatRoundEvent) -> Unit,
) {
    val editable = panel.more.identity != null || panel.more.naming != null
    Column(modifier = Modifier.padding(top = 2.dp)) {
        // the name is capped by the room the marks leave, so the line under it never runs past the text
        BoxWithConstraints {
            val nameMax = maxWidth - MARKS_ROOM
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = panel.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppPalette.TextHeading,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .widthIn(max = nameMax)
                            .then(
                                if (editable) {
                                    Modifier
                                        .clickable { send(CombatRoundEvent.NameTapped) }
                                        .dashedBottomRule(UNDERLINE)
                                } else {
                                    Modifier
                                },
                            ),
                )
                if (editable) {
                    Icon(
                        AppIcons.Edit,
                        contentDescription = null,
                        tint = AppPalette.TextSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                }
                when (panel.link) {
                    RowLink.CODEX -> CodexMark(7.dp)
                    RowLink.BATTLE_ONLY ->
                        BattleOnlyMark(
                            stringResource(Res.string.combat_link_battle_only),
                            shrunk = false,
                        )
                    RowLink.NONE -> Unit
                }
            }
        }
        if (panel.roleLine.isNotBlank()) {
            Text(
                text = panel.roleLine,
                color = AppPalette.TextMuted,
                fontSize = AppFontSize.Body,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
