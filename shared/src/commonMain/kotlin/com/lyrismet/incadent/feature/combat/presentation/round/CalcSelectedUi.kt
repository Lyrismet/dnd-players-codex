package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Control,
                    background = AppPalette.Background,
                    border = AppPalette.BorderSubtle,
                ).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        target.scales.forEach { option ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 36.dp)
                        .appCard(
                            shape = AppShapes.Small,
                            background = if (option.selected) AppPalette.SurfaceElevated else Color.Transparent,
                            border = if (option.selected) AppPalette.RadioIdle else null,
                            onClick = { emit(CalcEvent.ScaleSelected(target.id, option.scale)) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.label,
                    color = if (option.selected) AppPalette.TextHeading else AppPalette.TextTertiary,
                    fontSize = AppFontSize.Body,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** "it is a resistance: fire", one tap writes the answer into the bestiary */
@Composable
private fun QuickButton(
    quick: QuickResistance,
    onClick: () -> Unit,
) {
    val effect = HitEffect.entries.first { it.toDamageState() == quick.state }
    Row(
        modifier =
            Modifier
                .dashedBorder(AppPalette.RadioIdle, cornerRadius = 9.dp)
                .clickable(onClick = onClick)
                .defaultMinSize(minHeight = 36.dp)
                .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Shield(CalcShield(quick.type, effect), 16.dp)
        Text(
            quick.label,
            color = AppPalette.TextDescription,
            fontSize = AppFontSize.SmallPlus,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
