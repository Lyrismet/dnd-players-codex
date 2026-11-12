package com.lyrismet.incadent.feature.codex.presentation.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppCheckRow
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppTitleSubtitle
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.items_equip_done
import incadent.shared.generated.resources.items_equip_none
import incadent.shared.generated.resources.items_equip_others
import incadent.shared.generated.resources.items_equip_selected
import incadent.shared.generated.resources.items_equip_sheet_title
import incadent.shared.generated.resources.items_equip_will_move
import org.jetbrains.compose.resources.stringResource

/** the multi pick of what an owner carries - ticked items first, the others under them, "Done · N" at the bottom */
@Composable
internal fun EquipmentSheet(
    state: EquipmentSheetState,
    onEvent: (ItemEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { onEvent(ItemEvent.EquipmentDismissed) }) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppSheetHeader(
                    title = stringResource(Res.string.items_equip_sheet_title),
                    subtitle = state.ownerName,
                    onClose = { onEvent(ItemEvent.EquipmentDismissed) },
                )
                if (state.selected.isEmpty() && state.others.isEmpty()) {
                    Text(
                        stringResource(Res.string.items_equip_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppPalette.TextSecondary,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
                EquipmentBlock(stringResource(Res.string.items_equip_selected), state.selected, true, onEvent)
                EquipmentBlock(stringResource(Res.string.items_equip_others), state.others, false, onEvent)
            }
            AppDivider(color = AppPalette.BorderSubtle)
            Column(modifier = Modifier.padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 28.dp)) {
                FormPrimaryButton(
                    text = stringResource(Res.string.items_equip_done, state.selected.size),
                    onClick = { onEvent(ItemEvent.EquipmentConfirmed) },
                    height = 50.dp,
                )
            }
        }
    }
}

@Composable
private fun EquipmentBlock(
    title: String,
    rows: List<EquipmentRow>,
    checked: Boolean,
    onEvent: (ItemEvent) -> Unit,
) {
    if (rows.isEmpty()) return
    FormFieldLabel(text = title, modifier = Modifier.padding(top = 8.dp))
    rows.forEach { row ->
        AppCheckRow(checked = checked, onClick = { onEvent(ItemEvent.EquipmentToggled(row.id)) }) {
            AppTitleSubtitle(
                title = "${row.name} ${itemQuantityText(row.quantity)}",
                subtitle =
                    row.from
                        ?.let {
                            if (checked) stringResource(Res.string.items_equip_will_move, it.text()) else it.text()
                        }.orEmpty(),
            )
        }
    }
}
