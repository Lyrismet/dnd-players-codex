package com.lyrismet.incadent.core.calc

import kotlin.test.Test
import kotlin.test.assertEquals

class CalcExpressionTest {
    @Test
    fun `digit appends to an empty expression`() {
        assertEquals("7", CalcExpression().digit(7).raw)
    }

    @Test
    fun `digit appends to the current term`() {
        assertEquals("12", CalcExpression("1").digit(2).raw)
    }

    @Test
    fun `digit is ignored once the current term reaches 3 characters`() {
        assertEquals("123", CalcExpression("123").digit(4).raw)
    }

    @Test
    fun `digit after plus starts a fresh term instead of appending to the previous one`() {
        assertEquals("123+4", CalcExpression("123+").digit(4).raw)
    }

    @Test
    fun `plus is ignored on an empty expression`() {
        assertEquals("", CalcExpression().plus().raw)
    }

    @Test
    fun `plus is ignored when the expression already ends with plus`() {
        assertEquals("5+", CalcExpression("5+").plus().raw)
    }

    @Test
    fun `plus appends when the expression is non-empty and does not already end with plus`() {
        assertEquals("5+", CalcExpression("5").plus().raw)
    }

    @Test
    fun `backspace drops the last character`() {
        assertEquals("5", CalcExpression("53").backspace().raw)
    }

    @Test
    fun `backspace on an empty expression stays empty`() {
        assertEquals("", CalcExpression().backspace().raw)
    }

    @Test
    fun `clear resets to a blank expression`() {
        assertEquals("", CalcExpression("12+3").clear().raw)
    }

    @Test
    fun `value of an empty expression is zero`() {
        assertEquals(0, CalcExpression().value())
    }

    @Test
    fun `value of a lone plus is zero`() {
        assertEquals(0, CalcExpression("+").value())
    }

    @Test
    fun `value of a trailing plus ignores the empty trailing term`() {
        assertEquals(5, CalcExpression("5+").value())
    }

    @Test
    fun `value sums every term`() {
        assertEquals(15, CalcExpression("12+3").value())
    }

    @Test
    fun `value applies the multiplier and floors the result`() {
        assertEquals(7, CalcExpression("13+2").value(multiplier = 0.5))
    }

    @Test
    fun `value doubles with the x2 multiplier`() {
        assertEquals(30, CalcExpression("12+3").value(multiplier = 2.0))
    }

    @Test
    fun `display is blank for a single term`() {
        assertEquals("", CalcExpression("123").display())
    }

    @Test
    fun `display is blank for an empty expression`() {
        assertEquals("", CalcExpression().display())
    }

    @Test
    fun `display joins multiple terms with a plus`() {
        assertEquals("12 + 3", CalcExpression("12+3").display())
    }
}
