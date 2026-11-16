package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.core.designsystem.AppPalette
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_calc_mode_damage
import incadent.shared.generated.resources.combat_calc_mode_heal
import incadent.shared.generated.resources.combat_calc_mode_temp
import incadent.shared.generated.resources.hp_exact_mode
import org.jetbrains.compose.resources.StringResource

/** the words a calculator mode is named with, on its tab and on its button */
internal fun CalcMode.labelRes(): StringResource =
    when (this) {
        CalcMode.DAMAGE -> Res.string.combat_calc_mode_damage
        CalcMode.HEAL -> Res.string.combat_calc_mode_heal
        CalcMode.TEMP -> Res.string.combat_calc_mode_temp
        CalcMode.SET -> Res.string.hp_exact_mode
    }

/** the colour a calculator mode is written in */
internal fun CalcMode.tint(): Color =
    when (this) {
        CalcMode.DAMAGE -> AppPalette.MaroonBright
        CalcMode.HEAL -> AppPalette.EmeraldBright
        CalcMode.TEMP -> AppPalette.Azure
        CalcMode.SET -> AppPalette.GoldBright
    }
