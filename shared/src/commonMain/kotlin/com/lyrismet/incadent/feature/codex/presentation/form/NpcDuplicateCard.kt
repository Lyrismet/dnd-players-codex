package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppTitleSubtitle
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.Species
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_link_npc
import incadent.shared.generated.resources.npc_merge_button
import incadent.shared.generated.resources.npc_merge_hint
import org.jetbrains.compose.resources.stringResource

/** the line under the name of a twin, what it is and what it is by class */
@Composable
fun twinOf(
    npc: Npc,
    species: List<Species>,
): DuplicateNpc {
    val plain = stringResource(Res.string.combat_link_npc)
    val kind = species.firstOrNull { it.id == npc.speciesId }?.name
    val subtitle = joinWithDot(plain, kind.orEmpty(), npc.characterClass)
    return DuplicateNpc(npc.id, npc.name, subtitle)
}

/** the record that already has the name, and the way to make the two one */
@Composable
fun NpcDuplicateCard(
    twin: DuplicateNpc,
    onMerge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(shape = AppShapes.Field, background = AppPalette.Background, border = AppPalette.Border)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppTitleSubtitle(title = twin.name, subtitle = twin.subtitle)
        Text(
            stringResource(Res.string.npc_merge_hint),
            color = AppPalette.TextTertiary,
            fontSize = AppFontSize.Caption,
        )
        AppButton(
            text = stringResource(Res.string.npc_merge_button),
            onClick = onMerge,
            size = ButtonSize.Medium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
