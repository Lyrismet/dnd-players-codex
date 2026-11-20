package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.dashedBottomRule
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_link_battle_only
import org.jetbrains.compose.resources.stringResource

private val UNDERLINE = Color(0xFF4A4D5C)
private val MARKS_ROOM = 44.dp
private val MARKS_ROOM_LABELED = 112.dp
private val QUILL_WIDTH = 7.dp

/** the name of the participant, dashed and with a pencil when it can be changed, the link mark and the role under it */
@Composable
internal fun PanelName(
    panel: RoundPanel,
    send: (CombatRoundEvent) -> Unit,
) {
    val editable = panel.more.identity != null || panel.more.naming != null
    Column(modifier = Modifier.padding(top = 2.dp)) {
        // the rule is as long as the ink of the name, so it never runs past the text
        BoxWithConstraints {
            val nameMax = maxWidth - if (panel.link == RowLink.BATTLE_ONLY) MARKS_ROOM_LABELED else MARKS_ROOM
            var ink by remember { mutableFloatStateOf(0f) }
            val inkDp = with(LocalDensity.current) { ink.toDp() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = panel.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppPalette.TextHeading,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { ink = it.getLineRight(0) },
                    modifier =
                        Modifier
                            .widthIn(max = nameMax)
                            .then(
                                if (editable) {
                                    Modifier
                                        .clickable { send(CombatRoundEvent.NameTapped) }
                                        .dashedBottomRule(UNDERLINE, length = inkDp)
                                } else {
                                    Modifier
                                },
                            ),
                )
                if (editable) {
                    QuillMark()
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

// the quill is drawn in the middle of its square, the frame is as wide as the drawing
@Composable
private fun QuillMark() {
    Box(modifier = Modifier.size(width = QUILL_WIDTH, height = 15.dp), contentAlignment = Alignment.Center) {
        Icon(
            AppIcons.Edit,
            contentDescription = null,
            tint = AppPalette.TextSecondary,
            modifier = Modifier.requiredSize(15.dp),
        )
    }
}
