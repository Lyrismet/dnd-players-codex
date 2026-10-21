package com.lyrismet.incadent.feature.codex.presentation.style

import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.core.designsystem.component.ShieldStyle
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType

fun DamageState.toShieldStyle(): ShieldStyle =
    when (this) {
        DamageState.UNKNOWN -> ShieldStyle.Unknown
        DamageState.NORMAL -> ShieldStyle.Plain
        DamageState.RESISTANCE -> ShieldStyle.Resist
        DamageState.IMMUNITY -> ShieldStyle.Immune
        DamageState.VULNERABILITY -> ShieldStyle.Vulnerable
    }

private val Steel = Color(0xFFE2E6EE)
private val FireColor = Color(0xFFFF8A3D)
private val ColdColor = Color(0xFFA8E8FA)
private val LightningColor = Color(0xFF5DB4FF)
private val ThunderColor = Color(0xFF8C93FF)
private val AcidColor = Color(0xFFD4E13A)
private val PoisonColor = Color(0xFF5CCB6E)
private val NecroticColor = Color(0xFF5ED6B8)
private val RadiantColor = Color(0xFFF7CF4A)
private val ForceColor = Color(0xFFE8456B)
private val PsychicColor = Color(0xFFFF8CC6)

fun DamageType.shieldColor(): Color =
    when (this) {
        DamageType.BLUDGEONING, DamageType.PIERCING, DamageType.SLASHING -> Steel
        DamageType.FIRE -> FireColor
        DamageType.COLD -> ColdColor
        DamageType.LIGHTNING -> LightningColor
        DamageType.THUNDER -> ThunderColor
        DamageType.ACID -> AcidColor
        DamageType.POISON -> PoisonColor
        DamageType.NECROTIC -> NecroticColor
        DamageType.RADIANT -> RadiantColor
        DamageType.FORCE -> ForceColor
        DamageType.PSYCHIC -> PsychicColor
    }

// svg path data on a 24 grid, from the design mockup
fun DamageType.shieldIconPath(): String =
    when (this) {
        DamageType.BLUDGEONING -> "M5 4h12v6H5z M11 10v11"
        DamageType.PIERCING -> "M4 20L19 5 M12 5h7v7"
        DamageType.SLASHING -> "M20 4l-1 4.5L9.5 18 6 14.5 15.5 5z M4.5 12.5l7 7 M7.2 16.8L3.5 20.5"
        DamageType.FIRE ->
            "M12 22c-4 0-7-2.8-7-6.8 0-2.6 1.2-4.4 2.6-5.8.2 1.6 1 2.8 2.2 3.3-.4-3.6 1-7.2 4.2-10.2-.3 3 1 4.8 " +
                "2.6 6.5.4-1 .4-2 .1-3.2C19 8 19 11 19 15.2 19 19.2 16 22 12 22z"
        DamageType.COLD ->
            "M12 2v20 M3.3 7l17.4 10 M3.3 17l17.4-10 M9.5 3.5L12 6l2.5-2.5 M9.5 20.5L12 18l2.5 2.5 M4.1 10.4L6.8 9.5 " +
                "6.1 6.8 M19.9 13.6l-2.7.9.7 2.7 M4.1 13.6l2.7.9-.7 2.7 M19.9 10.4l-2.7-.9.7-2.7"
        DamageType.LIGHTNING -> "M13 2L5 14h6l-2 8 9-12h-6l1-8z"
        DamageType.THUNDER -> "M4 12h.01 M8 8.5a5 5 0 0 1 0 7 M12 5.5a9 9 0 0 1 0 13 M16 2.5a13 13 0 0 1 0 19"
        DamageType.ACID ->
            "M9 3h6 M10 3v5L5.5 18.5A2 2 0 0 0 7.3 21.5h9.4a2 2 0 0 0 1.8-3L14 8V3 M7.5 15h9 M11 18.3v.01 M14 17.8v.01"
        DamageType.POISON -> "M12 3c3 5 6 8 6 12a6 6 0 0 1-12 0c0-4 3-7 6-12z M10 13l4 4 M14 13l-4 4"
        DamageType.NECROTIC ->
            "M12 3a8 8 0 0 0-8 8c0 3 2 5 3 6v4h10v-4c1-1 3-3 3-6a8 8 0 0 0-8-8z " +
                "M9 9.3a2.2 2.2 0 1 0 0 4.4a2.2 2.2 0 1 0 0-4.4z M15 9.3a2.2 2.2 0 1 0 0 4.4a2.2 2.2 0 1 0 0-4.4z M10 21v-3 M14 21v-3"
        DamageType.RADIANT ->
            "M12 2l1.8 8.2L22 12l-8.2 1.8L12 22l-1.8-8.2L2 12l8.2-1.8z M5.5 5.5l2.2 2.2 M18.5 5.5l-2.2 2.2 " +
                "M5.5 18.5l2.2-2.2 M18.5 18.5l-2.2-2.2"
        DamageType.FORCE ->
            "M12 2l2 5 5-2-2 5 5 2-5 2 2 5-5-2-2 5-2-5-5 2 2-5-5-2 5-2-2-5 5 2z"
        DamageType.PSYCHIC ->
            "M2 14s4-6 10-6 10 6 10 6-4 6-10 6S2 14 2 14z M12 11.5a2.5 2.5 0 1 0 0 5a2.5 2.5 0 1 0 0-5z M12 2v3 " +
                "M5.5 3.5l1.5 2.6 M18.5 3.5L17 6.1"
    }
