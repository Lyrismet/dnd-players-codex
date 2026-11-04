package com.lyrismet.incadent.feature.codex.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CoinPurseTest {
    private val purse = CoinPurse(gold = 34, silver = 18, copper = 25)

    @Test
    fun `the total counts every coin in whole gold`() {
        assertEquals(36, purse.totalInGold())
    }

    @Test
    fun `the rates are the ones of 5e`() {
        assertEquals(10, Coin.PLATINUM.copperValue / Coin.GOLD.copperValue)
        assertEquals(10, Coin.GOLD.copperValue / Coin.SILVER.copperValue)
        assertEquals(5, Coin.ELECTRUM.copperValue / Coin.SILVER.copperValue)
        assertEquals(10, Coin.SILVER.copperValue / Coin.COPPER.copperValue)
        assertEquals(2, CoinPurse(electrum = 4).totalInGold())
    }

    @Test
    fun `spending gold shows the total before and after`() {
        val change = purse.change(Coin.GOLD, -15)
        assertEquals(19, change.after.gold)
        assertEquals(36, change.totalBefore)
        assertEquals(21, change.totalAfter)
    }

    @Test
    fun `earning platinum shows the total before and after`() {
        val change = purse.change(Coin.PLATINUM, 2)
        assertEquals(36, change.totalBefore)
        assertEquals(56, change.totalAfter)
    }

    @Test
    fun `a coin cannot go below zero or past the limit`() {
        assertEquals(0, purse.moved(Coin.COPPER, -100).copper)
        assertEquals(9_999_999, purse.moved(Coin.GOLD, Int.MAX_VALUE).gold)
        assertEquals(9_999_999, CoinPurse(gold = 20_000_000).clamped().gold)
        assertEquals(0, CoinPurse(silver = -4).clamped().silver)
    }
}
