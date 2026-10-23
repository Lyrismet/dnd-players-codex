package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.NumberPadSheet
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_hp_apply
import dndplayerscodex.shared.generated.resources.combat_hp_mode_hp
import dndplayerscodex.shared.generated.resources.combat_hp_mode_temp
import dndplayerscodex.shared.generated.resources.combat_hp_overline
import org.jetbrains.compose.resources.stringResource

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
