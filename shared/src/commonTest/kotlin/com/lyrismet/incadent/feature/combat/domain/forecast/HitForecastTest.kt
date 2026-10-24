package com.lyrismet.incadent.feature.combat.domain.forecast

import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import kotlin.test.Test
import kotlin.test.assertEquals

class HitForecastTest {
    @Test
    fun `no facts means unknown`() {
        assertEquals(HitForecast.Unknown, forecastHits(HitForecastInput(damageTaken = 0)))
    }

    @Test
    fun `taken damage and alive is a lower bound`() {
        assertEquals(HitForecast.AtLeast(22), forecastHits(HitForecastInput(damageTaken = 22)))
    }

    @Test
    fun `bloodied mark doubles the damage`() {
        val input = HitForecastInput(22, marks = listOf(StateMark(CombatState.BLOODIED, 22)))
        assertEquals(HitForecast.Estimate(max = 44, remaining = 22), forecastHits(input))
    }

    @Test
    fun `barely standing mark gives about 29 and 7 left`() {
        val input = HitForecastInput(22, marks = listOf(StateMark(CombatState.BARELY_STANDING, 22)))
        assertEquals(HitForecast.Estimate(max = 29, remaining = 7), forecastHits(input))
    }

    @Test
    fun `more damage than the state estimate cancels it`() {
        val input = HitForecastInput(46, marks = listOf(StateMark(CombatState.BLOODIED, 22)))
        assertEquals(HitForecast.AtLeast(46), forecastHits(input))
    }

    @Test
    fun `the last mark wins`() {
        val marks = listOf(StateMark(CombatState.BLOODIED, 22), StateMark(CombatState.BARELY_STANDING, 30))
        assertEquals(HitForecast.Estimate(max = 40, remaining = 5), forecastHits(HitForecastInput(35, marks = marks)))
    }

    @Test
    fun `not entering whole drops the forecast`() {
        val input =
            HitForecastInput(22, enteredWhole = false, marks = listOf(StateMark(CombatState.BLOODIED, 22)))
        assertEquals(HitForecast.Unknown, forecastHits(input))
    }

    @Test
    fun `a past death gives a forecast from the first hit`() {
        val input = HitForecastInput(0, speciesObservations = listOf(SpeciesHitObservation(30, died = true)))
        assertEquals(HitForecast.Estimate(max = 30, remaining = 30), forecastHits(input))
    }

    @Test
    fun `a past survival raises the lower bound`() {
        val input = HitForecastInput(5, speciesObservations = listOf(SpeciesHitObservation(40, died = false)))
        assertEquals(HitForecast.AtLeast(40), forecastHits(input))
    }

    @Test
    fun `a mark beats the species estimate`() {
        val input =
            HitForecastInput(
                10,
                marks = listOf(StateMark(CombatState.BLOODIED, 10)),
                speciesObservations = listOf(SpeciesHitObservation(60, died = true)),
            )
        assertEquals(HitForecast.Estimate(max = 20, remaining = 10), forecastHits(input))
    }
}
