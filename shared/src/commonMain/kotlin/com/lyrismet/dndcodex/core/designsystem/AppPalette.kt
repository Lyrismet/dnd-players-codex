package com.lyrismet.dndcodex.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * raw palette from the "design system" reference panel in players codex v4.dc.html
 * never reference these hex values directly from feature code - go through [AppColors] or the status mappers instead
 */
object AppPalette {
    // фон / поверхности
    val Background = Color(0xFF121318)
    val NavBar = Color(0xFF0F1015)
    val Surface = Color(0xFF1A1B23)
    val SurfaceElevated = Color(0xFF2A2C37)
    val SurfaceVariant = Color(0xFF17181F)
    val SurfaceSunken = Color(0xFF15161C)
    val Border = Color(0xFF2C2F3A)
    val BorderSubtle = Color(0xFF262833)
    val BorderHover = Color(0xFF3A3D4B)

    // текст
    val TextPrimary = Color(0xFFE2E8F0)
    val TextHeading = Color(0xFFF1EBDD)
    val TextSecondary = Color(0xFF8A93A6)
    val TextTertiary = Color(0xFF6B7385)
    val TextMuted = Color(0xFFA7B0C0)

    // акценты
    val Gold = Color(0xFFD4AF37)
    val GoldBright = Color(0xFFE2C044)
    val GoldDim = Color(0xFFB8993A)
    val Maroon = Color(0xFF9B2C2C)
    val MaroonBright = Color(0xFFF08A8A)
    val Emerald = Color(0xFF3FA97C)
    val EmeraldBright = Color(0xFF6FCF97)
    val Parchment = Color(0xFFD8C9A3)
    val Azure = Color(0xFF8CB8EC)
    val Amethyst = Color(0xFFBFA3EE)

    // ранения (скрытый режим боя)
    val WoundCritical = Color(0xFFE5484D)
    val WoundHeavy = Color(0xFFE0794A)

    // мёртв
    val Dead = Color(0xFF8E939C)
    val DeadBackground = Color(0xFF07080A)
}
