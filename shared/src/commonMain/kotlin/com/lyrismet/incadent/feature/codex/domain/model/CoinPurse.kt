package com.lyrismet.incadent.feature.codex.domain.model

/** the five coins of a character, each held between zero and [COIN_PURSE_RANGE] */
data class CoinPurse(
    val platinum: Int = 0,
    val gold: Int = 0,
    val electrum: Int = 0,
    val silver: Int = 0,
    val copper: Int = 0,
) {
    operator fun get(coin: Coin): Int =
        when (coin) {
            Coin.PLATINUM -> platinum
            Coin.GOLD -> gold
            Coin.ELECTRUM -> electrum
            Coin.SILVER -> silver
            Coin.COPPER -> copper
        }

    fun with(
        coin: Coin,
        amount: Int,
    ): CoinPurse {
        val value = amount.coerceIn(COIN_PURSE_RANGE)
        return when (coin) {
            Coin.PLATINUM -> copy(platinum = value)
            Coin.GOLD -> copy(gold = value)
            Coin.ELECTRUM -> copy(electrum = value)
            Coin.SILVER -> copy(silver = value)
            Coin.COPPER -> copy(copper = value)
        }
    }

    /** one coin moved by [delta], spending more than there is leaves none and earning past the limit stops at it */
    fun moved(
        coin: Coin,
        delta: Int,
    ): CoinPurse = with(coin, (this[coin].toLong() + delta).coerceIn(0L, COIN_PURSE_RANGE.last.toLong()).toInt())

    fun clamped(): CoinPurse = Coin.entries.fold(this) { purse, coin -> purse.with(coin, purse[coin]) }

    /** everything in whole gold coins, the small change below a gold coin is dropped */
    fun totalInGold(): Long = totalInCopper() / Coin.GOLD.copperValue

    fun totalInCopper(): Long = Coin.entries.sumOf { this[it].toLong() * it.copperValue }
}

val COIN_PURSE_RANGE = ITEM_PRICE_RANGE

/** how many copper pieces one coin is worth, the 5e rates: 1 pp = 10 gp, 1 gp = 10 sp, 1 ep = 5 sp, 1 sp = 10 cp */
val Coin.copperValue: Int
    get() =
        when (this) {
            Coin.PLATINUM -> 1000
            Coin.GOLD -> 100
            Coin.ELECTRUM -> 50
            Coin.SILVER -> 10
            Coin.COPPER -> 1
        }

/** a purse before and after one change, what "Total ≈ 36 → 21" shows */
data class CoinPurseChange(
    val before: CoinPurse,
    val after: CoinPurse,
) {
    val totalBefore: Long get() = before.totalInGold()
    val totalAfter: Long get() = after.totalInGold()
}

fun CoinPurse.change(
    coin: Coin,
    delta: Int,
): CoinPurseChange = CoinPurseChange(this, moved(coin, delta))
