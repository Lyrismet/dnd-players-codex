package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.feature.combat.domain.model.ConditionGroup

/** the colours of the fight, taken from the mockup's wound scale, sides and condition groups */
internal object RoundColors {
    private val Enemy = Color(0xFFC0443F)
    private val Bloodied = Color(0xFFE0794A)
    private val Barely = Color(0xFFE5484D)
    private val SlateTint = Color(0x1F94A3B8)
    private val RestraintChip = Color(0xFFF2B8B8)
    private val WeakeningChip = Color(0xFFCDB6F2)
    private val WeakeningTint = Color(0x33966EDC)
    private val WeakeningTitle = Color(0xFFBFA3EE)
    private val RestraintOn = Color(0xFFF6CACA)
    private val WeakeningOn = Color(0xFFDCCBF7)
    private val WeakeningOnFill = Color(0x3D966EDC)
    private val WeakeningOnBorder = Color(0xB3966EDC)
    private const val CHIP_FILL_ALPHA = 0.14f
    private const val RESTRAINT_FILL_ALPHA = 0.28f
    private const val RESTRAINT_ON_FILL_ALPHA = 0.35f
    private const val EFFECT_ON_BORDER_ALPHA = 0.55f

    fun side(side: RowSide): Color =
        when (side) {
            RowSide.PARTY -> AppPalette.Azure
            RowSide.ALLY -> AppPalette.Emerald
            RowSide.ENEMY -> Enemy
            RowSide.NEUTRAL -> AppPalette.TextMuted
            RowSide.FRIEND -> AppPalette.Emerald
            RowSide.FLED -> AppPalette.TextTertiary
        }

    fun wound(tone: WoundTone): Color =
        when (tone) {
            WoundTone.WHOLE -> AppPalette.EmeraldBright
            WoundTone.WOUNDED -> AppPalette.Gold
            WoundTone.BLOODIED -> Bloodied
            WoundTone.BARELY -> Barely
            WoundTone.DOWN -> AppPalette.TextSecondary
            WoundTone.DYING -> Barely
            WoundTone.DEAD -> AppPalette.TextTertiary
        }

    fun hp(level: HpLevel): Color =
        when (level) {
            HpLevel.HIGH -> AppPalette.Emerald
            HpLevel.MID -> AppPalette.Gold
            HpLevel.LOW -> Enemy
            HpLevel.ZERO -> AppPalette.RadioIdle
        }

    val Success: Color = AppPalette.Emerald
    val Failure: Color = Barely
    val Reaction: Color = AppPalette.Azure

    /** the text and the fill of a chip under a name */
    fun chip(kind: ChipKind): Pair<Color, Color> =
        when (kind) {
            ChipKind.CAPTURED -> AppPalette.Azure to AppPalette.Azure.copy(alpha = CHIP_FILL_ALPHA)
            ChipKind.NEUTRAL -> AppPalette.TextMuted to SlateTint
            ChipKind.FRIEND -> AppPalette.EmeraldBright to AppPalette.Emerald.copy(alpha = CHIP_FILL_ALPHA)
            ChipKind.FLED -> AppPalette.TextMuted to AppPalette.SurfaceElevated
            ChipKind.RESTRAINT -> RestraintChip to AppPalette.Maroon.copy(alpha = RESTRAINT_FILL_ALPHA)
            ChipKind.WEAKENING -> WeakeningChip to WeakeningTint
            ChipKind.EFFECT -> AppPalette.GoldBright to AppPalette.Gold.copy(alpha = CHIP_FILL_ALPHA)
        }

    fun groupTitle(group: ConditionGroup): Color =
        when (group) {
            ConditionGroup.RESTRAINT -> AppPalette.MaroonBright
            ConditionGroup.WEAKENING -> WeakeningTitle
            ConditionGroup.EFFECT -> AppPalette.GoldBright
        }

    /** a condition that is on: its text, fill and border */
    fun groupOn(group: ConditionGroup): Triple<Color, Color, Color> =
        when (group) {
            ConditionGroup.RESTRAINT ->
                Triple(RestraintOn, AppPalette.Maroon.copy(alpha = RESTRAINT_ON_FILL_ALPHA), AppPalette.Maroon)
            ConditionGroup.WEAKENING -> Triple(WeakeningOn, WeakeningOnFill, WeakeningOnBorder)
            ConditionGroup.EFFECT ->
                Triple(
                    AppPalette.GoldBright,
                    AppPalette.Gold.copy(alpha = CHIP_FILL_ALPHA),
                    AppPalette.Gold.copy(alpha = EFFECT_ON_BORDER_ALPHA),
                )
        }
}
