package com.lyrismet.incadent.feature.codex.presentation.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppDamageShield
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType

/** the shield of one damage type in one state - its icon, colour and rim from the codex mappers */
@Composable
fun DamageTypeShield(
    type: DamageType,
    state: DamageState,
    modifier: Modifier = Modifier,
    width: Dp = 30.dp,
) {
    AppDamageShield(
        iconPath = type.shieldIconPath(),
        iconColor = type.shieldColor(),
        style = state.toShieldStyle(),
        modifier = modifier,
        width = width,
    )
}
