package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppDashedTile
import com.lyrismet.incadent.core.designsystem.component.SegmentedControl
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect

@Composable
internal fun SelectedTarget(
    target: CalcTarget,
    emit: (CalcEvent) -> Unit,
    showParts: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
        if (showParts) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    target.name,
                    color = AppPalette.TextPrimary,
                    fontSize = AppFontSize.Small,
                    fontWeight = FontWeight.SemiBold,
                )
                PartsLine(target.parts)
            }
        }
        ScaleSelector(target, emit)
        target.quick?.let { QuickButton(it) { emit(CalcEvent.QuickRecorded) } }
    }
}

/** the master's words about the hit as a whole: in full, half, nothing, double */
@Composable
private fun ScaleSelector(
    target: CalcTarget,
    emit: (CalcEvent) -> Unit,
) {
    SegmentedControl(
        items = target.scales,
        onSelected = { option -> emit(CalcEvent.ScaleSelected(target.id, option.scale)) },
        itemBackground = { option -> if (option.selected) AppPalette.SurfaceElevated else Color.Transparent },
        containerShape = AppShapes.Control,
        containerBackground = AppPalette.Background,
        itemShape = AppShapes.Small,
        itemHeight = 36.dp,
        contentPadding = 3.dp,
        itemBorder = { option -> if (option.selected) AppPalette.RadioIdle else Color.Transparent },
    ) { option ->
        Text(
            option.label,
            color = if (option.selected) AppPalette.TextHeading else AppPalette.TextTertiary,
            fontSize = AppFontSize.Body,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** "it is a resistance: fire", one tap writes the answer into the bestiary */
@Composable
private fun QuickButton(
    quick: QuickResistance,
    onClick: () -> Unit,
) {
    val effect = HitEffect.entries.first { it.toDamageState() == quick.state }
    AppDashedTile(
        onClick = onClick,
        color = AppPalette.RadioIdle,
        cornerRadius = 9.dp,
        minHeight = 36.dp,
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        CalcShieldMark(CalcShield(quick.type, effect), 16.dp)
        Text(
            quick.label,
            color = AppPalette.TextDescription,
            fontSize = AppFontSize.SmallPlus,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
