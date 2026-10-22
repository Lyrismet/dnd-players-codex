package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.NumberPadSheet
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_hp_apply
import dndplayerscodex.shared.generated.resources.combat_hp_mode_hp
import dndplayerscodex.shared.generated.resources.combat_hp_mode_temp
import dndplayerscodex.shared.generated.resources.combat_hp_overline
import dndplayerscodex.shared.generated.resources.combat_quick_create
import dndplayerscodex.shared.generated.resources.combat_quick_footer
import dndplayerscodex.shared.generated.resources.combat_quick_hint_enemy
import dndplayerscodex.shared.generated.resources.combat_quick_hint_party
import dndplayerscodex.shared.generated.resources.combat_quick_label
import dndplayerscodex.shared.generated.resources.combat_quick_need_name
import dndplayerscodex.shared.generated.resources.combat_quick_overline
import dndplayerscodex.shared.generated.resources.combat_quick_title_enemy
import dndplayerscodex.shared.generated.resources.combat_quick_title_party
import org.jetbrains.compose.resources.stringResource

private val LineHeight20 = 20.sp

/** a codex record with only a name that joins the fight at once */
@Composable
internal fun QuickCreateSheet(
    quick: QuickCreateState,
    onEvent: (CombatPrepEvent) -> Unit,
) {
    val party = quick.kind == QuickKind.PARTY
    val canCreate = quick.name.isNotBlank()
    AppBottomSheet(onDismissRequest = { onEvent(CombatPrepEvent.QuickClosed) }) {
        AppSheetColumn(spacing = 16.dp) {
            AppSheetHeader(
                title =
                    stringResource(
                        if (party) Res.string.combat_quick_title_party else Res.string.combat_quick_title_enemy,
                    ),
                onClose = { onEvent(CombatPrepEvent.QuickClosed) },
                overline = stringResource(Res.string.combat_quick_overline),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormFieldLabel(stringResource(Res.string.combat_quick_label))
                AppTextInput(
                    value = quick.name,
                    onValueChange = { onEvent(CombatPrepEvent.QuickNameChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    autoFocus = true,
                )
                Text(
                    text =
                        stringResource(
                            if (party) Res.string.combat_quick_hint_party else Res.string.combat_quick_hint_enemy,
                        ),
                    color = AppPalette.TextSecondary,
                    fontSize = AppFontSize.Small,
                    lineHeight = LineHeight20,
                )
            }
            FormPrimaryButton(
                text =
                    stringResource(
                        if (canCreate) Res.string.combat_quick_create else Res.string.combat_quick_need_name,
                    ),
                onClick = { onEvent(CombatPrepEvent.QuickConfirmed) },
                enabled = canCreate,
            )
            Text(
                text = stringResource(Res.string.combat_quick_footer),
                color = AppPalette.TextSecondary,
                fontSize = AppFontSize.Small,
                lineHeight = LineHeight20,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** the keypad of a party member's hit points or temporary hit points before the fight */
@Composable
internal fun HpPadSheet(
    pad: HpPadState,
    onEvent: (CombatPrepEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { onEvent(CombatPrepEvent.HpPadClosed) }) {
        NumberPadSheet(
            overline = stringResource(Res.string.combat_hp_overline),
            title = pad.name,
            expr = "",
            result = pad.result,
            rangeHint = pad.preview,
            applyLabel = stringResource(Res.string.combat_hp_apply),
            applyEnabled = pad.canApply,
            onDigit = { onEvent(CombatPrepEvent.HpDigit(it)) },
            onBackspace = { onEvent(CombatPrepEvent.HpBackspace) },
            onClear = { onEvent(CombatPrepEvent.HpCleared) },
            onApply = { onEvent(CombatPrepEvent.HpApplied) },
            onClose = { onEvent(CombatPrepEvent.HpPadClosed) },
            modes =
                if (!pad.modesVisible) {
                    null
                } else {
                    {
                        AppSegmentedTabs(
                            tabs =
                                listOf(
                                    SegmentedTab(HpMode.HP, stringResource(Res.string.combat_hp_mode_hp)),
                                    SegmentedTab(HpMode.TEMP, stringResource(Res.string.combat_hp_mode_temp)),
                                ),
                            selected = pad.mode,
                            onSelected = { onEvent(CombatPrepEvent.HpModeChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
        )
    }
}
