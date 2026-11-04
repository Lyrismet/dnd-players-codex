package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HitPointsTest {
    private val vitals = CharacterVitals(hpMax = 28, hpCurrent = 22, tempHp = 3)

    @Test
    fun `damage is soaked by temporary hit points first`() {
        val after = vitals.hit(HitPointMode.DAMAGE, 5)
        assertEquals(20, after.hpCurrent)
        assertEquals(0, after.tempHp)
    }

    @Test
    fun `damage smaller than the temporary hit points leaves the real ones`() {
        val after = vitals.hit(HitPointMode.DAMAGE, 2)
        assertEquals(22, after.hpCurrent)
        assertEquals(1, after.tempHp)
    }

    @Test
    fun `damage cannot take the hit points below zero`() {
        assertEquals(0, vitals.hit(HitPointMode.DAMAGE, 999).hpCurrent)
    }

    @Test
    fun `healing stops at the maximum`() {
        assertEquals(28, vitals.hit(HitPointMode.HEAL, 100).hpCurrent)
    }

    @Test
    fun `healing from zero starts the death saves over`() {
        val down = vitals.copy(hpCurrent = 0, deathSavesSuccess = 2, deathSavesFailure = 1)
        val up = down.hit(HitPointMode.HEAL, 4)
        assertEquals(4, up.hpCurrent)
        assertEquals(0, up.deathSavesSuccess)
        assertEquals(0, up.deathSavesFailure)
    }

    @Test
    fun `temporary hit points are set and not added`() {
        assertEquals(7, vitals.hit(HitPointMode.TEMP, 7).tempHp)
    }

    @Test
    fun `three failures mean dead and three successes mean stable`() {
        assertEquals(DeathSaveState.DYING, vitals.deathSaveState())
        assertEquals(DeathSaveState.DEAD, vitals.copy(deathSavesFailure = 3).deathSaveState())
        assertEquals(DeathSaveState.STABLE, vitals.copy(deathSavesSuccess = 3).deathSaveState())
    }

    @Test
    fun `a character is down only with a maximum and nothing left`() {
        assertTrue(vitals.copy(hpCurrent = 0).isDown)
        assertFalse(vitals.isDown)
        assertFalse(CharacterVitals(hpMax = null, hpCurrent = 0).isDown)
    }

    @Test
    fun `a death save dot moves the count to it and the last one empties it`() {
        assertEquals(2, 0.withDeathSaveDot(1))
        assertEquals(1, 2.withDeathSaveDot(1))
        assertEquals(0, 1.withDeathSaveDot(0))
    }
}
