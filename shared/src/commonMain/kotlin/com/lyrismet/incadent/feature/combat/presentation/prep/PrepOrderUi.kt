package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.Dot
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_list_reset
import dndplayerscodex.shared.generated.resources.combat_prep_order_header
import dndplayerscodex.shared.generated.resources.combat_prep_order_hint
import org.jetbrains.compose.resources.stringResource

private val NumberShape = CircleShape

/** the third step: tap the participants in the order the master names them */
@Composable
internal fun PrepOrderSection(
    rows: List<PrepOrderRow>,
    onEvent: (CombatPrepEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 14.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionOverline(
            text = stringResource(Res.string.combat_prep_order_header),
            color = AppPalette.TextTertiary,
            style = OverlineStyle.Field,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = stringResource(Res.string.combat_list_reset),
            onClick = { onEvent(CombatPrepEvent.OrderReset) },
            tone = ButtonTone.Neutral,
            variant = ButtonVariant.Ghost,
        )
    }
    Text(
        text = stringResource(Res.string.combat_prep_order_hint),
        color = AppPalette.TextSecondary,
        fontSize = AppFontSize.SmallPlus,
        lineHeight = AppLineHeight.Row,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    rows.forEach { row -> OrderRow(row) { onEvent(CombatPrepEvent.OrderTapped(row.memberId)) } }
}

@Composable
private fun OrderRow(
    row: PrepOrderRow,
    onClick: () -> Unit,
) {
    val has = row.number != null
    val dot =
        when (row.side) {
            OrderSide.PARTY -> AppPalette.Azure
            OrderSide.ALLY -> AppPalette.Emerald
            OrderSide.ENEMY -> AppPalette.Enemy
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.SurfaceVariant,
                    border = if (has) AppPalette.Gold.copy(alpha = 0.35f) else dot.copy(alpha = 0.25f),
                    onClick = onClick,
                ).defaultMinSize(minHeight = 60.dp)
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { Dot(size = 8.dp, color = dot) }
        PrepRowTitle(row.name, row.sub, Modifier.weight(1f))
        OrderNumber(row.number)
    }
}

@Composable
private fun OrderNumber(number: Int?) {
    val base = Modifier.size(44.dp)
    Box(
        modifier =
            if (number != null) {
                base.background(AppPalette.Gold, NumberShape).border(1.5.dp, AppPalette.Gold, NumberShape)
            } else {
                base
                    .background(AppPalette.Background, NumberShape)
                    .dashedBorder(
                        AppPalette.RadioIdle,
                        width = 1.5.dp,
                        cornerRadius = 22.dp,
                        dashLength = 3.dp,
                        dashGap = 3.dp,
                    )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (number != null) {
            Text(
                number.toString(),
                color = AppPalette.Background,
                fontSize = AppFontSize.Heading,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** the bar over the tab bar: what keeps the fight from starting, or the way into it */
@Composable
internal fun PrepStartBar(
    start: PrepStart,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(AppPalette.SurfaceFooter)) {
        AppDivider(color = AppPalette.BorderSubtle)
        AppButton(
            text = start.label,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 12.dp),
            variant = ButtonVariant.Solid,
            size = ButtonSize.Primary,
            enabled = start.ready,
        )
    }
}
