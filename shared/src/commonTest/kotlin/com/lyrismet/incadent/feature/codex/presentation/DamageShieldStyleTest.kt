package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.core.designsystem.component.ShieldStyle
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.presentation.style.shieldIconPath
import com.lyrismet.incadent.feature.codex.presentation.style.toShieldStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DamageShieldStyleTest {
    @Test
    fun `every damage state has its own shield style`() {
        assertEquals(
            listOf(
                ShieldStyle.Unknown,
                ShieldStyle.Plain,
                ShieldStyle.Resist,
                ShieldStyle.Immune,
                ShieldStyle.Vulnerable,
            ),
            DamageState.entries.map { it.toShieldStyle() },
        )
        assertEquals(
            DamageState.entries.size,
            DamageState.entries
                .map { it.toShieldStyle() }
                .toSet()
                .size,
        )
    }

    @Test
    fun `every damage type has an icon`() {
        assertTrue(DamageType.entries.all { it.shieldIconPath().isNotBlank() })
        assertEquals(
            DamageType.entries.size,
            DamageType.entries
                .map { it.shieldIconPath() }
                .toSet()
                .size,
        )
    }
}
