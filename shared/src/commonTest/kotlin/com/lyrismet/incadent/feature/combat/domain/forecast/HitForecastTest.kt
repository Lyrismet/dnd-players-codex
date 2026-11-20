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
    fun `a past death gives a forecast from the first hit`() {
        val input = HitForecastInput(0, speciesObservations = listOf(SpeciesHitObservation(30, died = true)))
        assertEquals(HitForecast.Estimate(max = 30, remaining = 30, ForecastSource.SPECIES), forecastHits(input))
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

    @Test
    fun `typed hit points move with the damage and healing told after them`() {
        val exact = ExactMark(remaining = 1, damageAtMark = 40)
        val input = HitForecastInput(damageTaken = 40, healed = 17, exact = exact)
        assertEquals(HitForecast.Exact(18, 41), forecastHits(input))
        assertEquals(HitForecast.Exact(13, 41), forecastHits(input.copy(damageTaken = 45)))
    }

    @Test
    fun `typed hit points after a heal that was told once are the paladin case`() {
        val exact = ExactMark(remaining = 1, damageAtMark = 55)
        val input = HitForecastInput(damageTaken = 55, healed = 55, exact = exact)
        assertEquals(HitForecast.Exact(56, 56), forecastHits(input))
    }

    @Test
    fun `typed hit points never go below zero`() {
        val input = HitForecastInput(damageTaken = 50, exact = ExactMark(remaining = 3, damageAtMark = 10))
        assertEquals(HitForecast.Exact(0, 13), forecastHits(input))
    }

    @Test
    fun `fifty damage and fifty typed left give a hundred maximum`() {
        val input = HitForecastInput(damageTaken = 50, exact = ExactMark(remaining = 50, damageAtMark = 50))
        assertEquals(HitForecast.Exact(50, 100), forecastHits(input))
    }

    @Test
    fun `typed hit points count with no damage taken`() {
        val input =
            HitForecastInput(damageTaken = 0, exact = ExactMark(remaining = 7, damageAtMark = 0))
        assertEquals(HitForecast.Exact(7, 7), forecastHits(input))
    }

    @Test
    fun `healing an enemy lowers what it took and keeps the mark estimate`() {
        val input =
            HitForecastInput(
                damageTaken = 50,
                healed = 10,
                marks = listOf(StateMark(CombatState.BLOODIED, 40)),
            )
        assertEquals(HitForecast.Estimate(max = 80, remaining = 40), forecastHits(input))
    }

    @Test
    fun `healing before a mark is taken off the damage at the mark`() {
        val mark = StateMark(CombatState.BLOODIED, damageAtMark = 50, healedAtMark = 10)
        val input = HitForecastInput(damageTaken = 50, healed = 10, marks = listOf(mark))
        assertEquals(HitForecast.Estimate(max = 80, remaining = 40), forecastHits(input))
    }

    @Test
    fun `a place on the scale gives the maximum from the damage`() {
        val mark = StateMark(CombatState.WOUNDED, damageAtMark = 30, permille = 750)
        assertEquals(
            HitForecast.Estimate(max = 120, remaining = 90),
            forecastHits(HitForecastInput(30, marks = listOf(mark))),
        )
    }

    @Test
    fun `the right end of the scale says only that there are more`() {
        val mark = StateMark(CombatState.WHOLE, damageAtMark = 55, permille = 1000)
        assertEquals(HitForecast.AtLeast(55), forecastHits(HitForecastInput(55, marks = listOf(mark))))
    }

    @Test
    fun `the scale has a species estimate source when no mark gave it`() {
        val input = HitForecastInput(10, speciesObservations = listOf(SpeciesHitObservation(30, died = true)))
        assertEquals(ForecastSource.SPECIES, (forecastHits(input) as HitForecast.Estimate).source)
    }

    @Test
    fun `the words of the scale are found by place`() {
        assertEquals(CombatState.DOWN, stateAtPermille(0))
        assertEquals(CombatState.BLOODIED, stateAtPermille(480))
        assertEquals(CombatState.WOUNDED, stateAtPermille(800))
        assertEquals(CombatState.WHOLE, stateAtPermille(1000))
        assertEquals(500, permilleOf(CombatState.BLOODIED))
    }

    @Test
    fun `a typed number takes its place from what was lost`() {
        assertEquals(24, permilleOfExact(remaining = 1, netDamage = 40))
        assertEquals(0, permilleOfExact(remaining = 0, netDamage = 0))
        assertEquals(500, permilleOfExact(remaining = 40, netDamage = 40))
    }

    @Test
    fun `a tie on the scale goes to the healthier word`() {
        assertEquals(CombatState.BARELY_STANDING, stateAtPermille(125))
        assertEquals(CombatState.BLOODIED, stateAtPermille(375))
    }

    @Test
    fun `a mark at the whole end does not hide an earlier one`() {
        val marks =
            listOf(
                StateMark(CombatState.BLOODIED, 22, permille = 500),
                StateMark(CombatState.WHOLE, 22, permille = 1000),
            )
        assertEquals(HitForecast.Estimate(max = 44, remaining = 22), forecastHits(HitForecastInput(22, marks = marks)))
    }
}
