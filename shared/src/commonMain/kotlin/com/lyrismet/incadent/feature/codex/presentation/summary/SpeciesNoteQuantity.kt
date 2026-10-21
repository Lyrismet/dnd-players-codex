package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.mention.domain.MAX_MENTION_QUANTITY
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_species_note_quantity
import org.jetbrains.compose.resources.stringResource

/** "В этой заметке ×4 [−][+]" - how many of the species the tapped chip counts, [onStep] null leaves it read-only */
@Composable
internal fun SpeciesNoteQuantityRow(
    quantity: Int,
    onStep: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, AppPalette.Border)
                .padding(start = 14.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(Res.string.entity_sheet_species_note_quantity),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.TextSecondary,
        )
        Text(
            "×$quantity",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = AppFontSize.Lead),
            color = AppPalette.GoldBright,
        )
        if (onStep != null) {
            StepButton("−", enabled = quantity > 1) { onStep(-1) }
            StepButton("+", enabled = quantity < MAX_MENTION_QUANTITY) { onStep(1) }
        }
    }
}

@Composable
private fun StepButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconBadge(
        size = 44.dp,
        background = AppPalette.SurfacePopover,
        border = AppPalette.Border,
        onClick = onClick.takeIf { enabled },
    ) {
        Text(
            label,
            fontSize = AppFontSize.Title,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) AppPalette.Gold else AppPalette.TextMuted,
        )
    }
}
