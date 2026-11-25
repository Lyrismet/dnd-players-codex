package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.feature.me.domain.HitPointMode
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_hp_mode_max
import incadent.shared.generated.resources.me_hp_mode_minus
import incadent.shared.generated.resources.me_hp_mode_plus
import incadent.shared.generated.resources.me_hp_mode_temp
import org.jetbrains.compose.resources.stringResource

/** the tabs of the hit point pad and the button that swaps them for the maximum */
@Composable
internal fun HitPointsModes(
    pad: MeOverlay.HitPoints,
    onEvent: (HitPointsEvent) -> Unit,
) {
    val editingMax = pad.mode == HitPointMode.MAX
    AppSegmentedTabs(
        tabs =
            listOf(
                SegmentedTab(
                    HitPointMode.HEAL,
                    stringResource(Res.string.me_hp_mode_plus),
                    accent = HitPointMode.HEAL.accent(),
                ),
                SegmentedTab(
                    HitPointMode.DAMAGE,
                    stringResource(Res.string.me_hp_mode_minus),
                    accent = HitPointMode.DAMAGE.accent(),
                ),
                SegmentedTab(HitPointMode.TEMP, stringResource(Res.string.me_hp_mode_temp)),
            ),
        selected = pad.mode,
        onSelected = { onEvent(HitPointsEvent.ModeChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
    )
    AppButton(
        text = stringResource(Res.string.me_hp_mode_max),
        onClick = {
            onEvent(HitPointsEvent.ModeChanged(if (editingMax) HitPointMode.HEAL else HitPointMode.MAX))
        },
        modifier = Modifier.fillMaxWidth(),
        variant = if (editingMax) ButtonVariant.Solid else ButtonVariant.Ghost,
        size = ButtonSize.Medium,
    )
}
