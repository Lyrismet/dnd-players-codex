package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.CoinPurse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletPadTest {
    private val purse = CoinPurse(gold = 34, silver = 18, copper = 25)

    private fun typed(
        pad: WalletPad,
        vararg digits: Int,
    ) = digits.fold(pad) { acc, digit -> acc.digit(digit) }

    @Test
    fun `spending fifteen gold shows the total going from 36 to 21`() {
        val change = typed(WalletPad(mode = WalletMode.SPEND), 1, 5).change(purse)
        assertEquals(19, change.after.gold)
        assertEquals(36, change.totalBefore)
        assertEquals(21, change.totalAfter)
        assertTrue(change.showsTotal())
    }

    @Test
    fun `gaining two platinum moves the total from 36 to 56`() {
        val change = typed(WalletPad(mode = WalletMode.GAIN, coin = Coin.PLATINUM), 2).change(purse)
        assertEquals(2, change.after.platinum)
        assertEquals(56, change.totalAfter)
    }

    @Test
    fun `the pad opens on gaining`() {
        assertEquals(WalletMode.GAIN, WalletPad().mode)
    }

    @Test
    fun `nothing typed cannot be applied`() {
        assertFalse(WalletPad().canApply)
        assertTrue(typed(WalletPad(), 7).canApply)
    }

    @Test
    fun `the digits stop at seven so the purse limit holds`() {
        val pad = typed(WalletPad(), 9, 9, 9, 9, 9, 9, 9, 9)
        assertEquals("9999999", pad.typed)
        assertEquals(9_999_999, pad.amount)
    }

    @Test
    fun `earning past the limit stops at it and spending past zero leaves none`() {
        val rich = CoinPurse(gold = 9_999_990)
        assertEquals(9_999_999, typed(WalletPad(mode = WalletMode.GAIN), 5, 0).change(rich).after.gold)
        assertEquals(0, typed(WalletPad(mode = WalletMode.SPEND), 9, 9).change(CoinPurse(gold = 5)).after.gold)
    }

    @Test
    fun `a leading zero is replaced and a backspace removes the last digit`() {
        assertEquals("5", typed(WalletPad(), 0, 5).typed)
        assertEquals("1", typed(WalletPad(), 1, 2).backspace().typed)
    }

    @Test
    fun `a purse of gold alone has no total line`() {
        val change = typed(WalletPad(mode = WalletMode.SPEND), 1).change(CoinPurse(gold = 34))
        assertFalse(change.showsTotal())
    }
}
