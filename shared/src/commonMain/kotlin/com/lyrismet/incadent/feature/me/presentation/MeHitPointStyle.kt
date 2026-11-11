package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.feature.me.domain.HitPointMode

internal fun HitPointMode.accent(): Color? =
    when (this) {
        HitPointMode.HEAL -> AppPalette.EmeraldBright
        HitPointMode.DAMAGE -> AppPalette.MaroonBright
        HitPointMode.TEMP -> null
    }

// a true minus like the rest of the app, a temporary number has no sign
internal fun String.withSign(mode: HitPointMode): String =
    when (mode) {
        HitPointMode.HEAL -> "+$this"
        HitPointMode.DAMAGE -> "−$this"
        HitPointMode.TEMP -> this
    }
