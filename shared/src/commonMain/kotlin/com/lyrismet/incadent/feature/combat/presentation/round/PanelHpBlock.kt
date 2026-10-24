package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette

/** the current and max hit points of the open panel, a tap opens the hit points pad */
@Composable
internal fun PanelHpBlock(
    hp: PanelHp,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.clickable { send(CombatRoundEvent.SetHpOpened) },
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                hp.current,
                color = AppPalette.TextPrimary,
                fontSize = AppFontSize.Stat,
                fontWeight = FontWeight.Bold,
            )
            Text(" / ${hp.max}", color = AppPalette.TextTertiary, fontSize = AppFontSize.MediumPlus)
        }
        Text(
            hp.caption,
            color = AppPalette.TextTertiary,
            fontSize = AppFontSize.Caption,
            lineHeight = AppLineHeight.Row,
        )
    }
}
