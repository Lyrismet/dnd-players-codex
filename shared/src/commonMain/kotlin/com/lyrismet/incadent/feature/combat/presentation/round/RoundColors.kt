package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.feature.combat.domain.model.ConditionGroup

/** the colours of the fight, taken from the mockup's wound scale, sides and condition groups */
internal object RoundColors {
    private const val CHIP_FILL_ALPHA = 0.14f
    private const val RESTRAINT_FILL_ALPHA = 0.28f
    private const val RESTRAINT_ON_FILL_ALPHA = 0.35f
    private const val EFFECT_ON_BORDER_ALPHA = 0.55f

    fun side(side: RowSide): Color =
        when (side) {
            RowSide.PARTY -> AppPalette.Azure
            RowSide.ALLY -> AppPalette.Emerald
            RowSide.ENEMY -> AppPalette.Enemy
            RowSide.NEUTRAL -> AppPalette.TextMuted
            RowSide.FRIEND -> AppPalette.Emerald
            RowSide.FLED -> AppPalette.TextTertiary
        }

    fun wound(tone: WoundTone): Color =
        when (tone) {
            WoundTone.WHOLE -> AppPalette.EmeraldBright
            WoundTone.WOUNDED -> AppPalette.Gold
            WoundTone.BLOODIED -> AppPalette.Bloodied
            WoundTone.BARELY -> AppPalette.Barely
            WoundTone.DOWN -> AppPalette.TextSecondary
            WoundTone.DYING -> AppPalette.Barely
            WoundTone.DEAD -> AppPalette.TextTertiary
        }

    fun hp(level: HpLevel): Color =
        when (level) {
            HpLevel.HIGH -> AppPalette.Emerald
            HpLevel.MID -> AppPalette.Gold
            HpLevel.LOW -> AppPalette.Enemy
            HpLevel.ZERO -> AppPalette.RadioIdle
        }

    val Success: Color = AppPalette.Emerald
    val Failure: Color = AppPalette.Barely
    val Reaction: Color = AppPalette.Azure

    /** the text and the fill of a chip under a name */
    fun chip(kind: ChipKind): Pair<Color, Color> =
        when (kind) {
            ChipKind.CAPTURED -> AppPalette.Azure to AppPalette.Azure.copy(alpha = CHIP_FILL_ALPHA)
            ChipKind.NEUTRAL -> AppPalette.TextMuted to AppPalette.SlateTint
            ChipKind.FRIEND -> AppPalette.EmeraldBright to AppPalette.Emerald.copy(alpha = CHIP_FILL_ALPHA)
            ChipKind.FLED -> AppPalette.TextMuted to AppPalette.SurfaceElevated
            ChipKind.RESTRAINT -> AppPalette.RestraintChip to AppPalette.Maroon.copy(alpha = RESTRAINT_FILL_ALPHA)
            ChipKind.WEAKENING -> AppPalette.WeakeningChip to AppPalette.WeakeningTint
            ChipKind.EFFECT -> AppPalette.GoldBright to AppPalette.Gold.copy(alpha = CHIP_FILL_ALPHA)
        }

    fun groupTitle(group: ConditionGroup): Color =
        when (group) {
            ConditionGroup.RESTRAINT -> AppPalette.MaroonBright
            ConditionGroup.WEAKENING -> AppPalette.WeakeningTitle
            ConditionGroup.EFFECT -> AppPalette.GoldBright
        }

    /** a condition that is on: its text, fill and border */
    fun groupOn(group: ConditionGroup): Triple<Color, Color, Color> =
        when (group) {
            ConditionGroup.RESTRAINT ->
                Triple(
                    AppPalette.RestraintOn,
                    AppPalette.Maroon.copy(alpha = RESTRAINT_ON_FILL_ALPHA),
                    AppPalette.Maroon,
                )
            ConditionGroup.WEAKENING ->
                Triple(
                    AppPalette.WeakeningOn,
                    AppPalette.WeakeningOnFill,
                    AppPalette.WeakeningOnBorder,
                )
            ConditionGroup.EFFECT ->
                Triple(
                    AppPalette.GoldBright,
                    AppPalette.Gold.copy(alpha = CHIP_FILL_ALPHA),
                    AppPalette.Gold.copy(alpha = EFFECT_ON_BORDER_ALPHA),
                )
        }
}
