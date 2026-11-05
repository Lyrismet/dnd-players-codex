package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.COIN_PURSE_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.CoinPurse
import com.lyrismet.incadent.feature.codex.domain.model.CoinPurseChange
import com.lyrismet.incadent.feature.codex.domain.model.change

enum class WalletMode { SPEND, GAIN }

/** the coins listed first in the keypad, the other two sit under "more" */
val QUICK_COINS = listOf(Coin.GOLD, Coin.SILVER, Coin.COPPER)
val MORE_COINS = listOf(Coin.PLATINUM, Coin.ELECTRUM)

private val MAX_DIGITS = COIN_PURSE_RANGE.last.toString().length

/** the wallet keypad: which way and which coin it moves and the digits typed so far */
data class WalletPad(
    val mode: WalletMode = WalletMode.GAIN,
    val coin: Coin = Coin.GOLD,
    val typed: String = "",
    val moreOpen: Boolean = false,
) {
    val amount: Int get() = typed.toIntOrNull() ?: 0

    val canApply: Boolean get() = amount > 0

    fun digit(digit: Int): WalletPad {
        // a lone zero is not kept as a leading digit
        val next = if (typed == "0") digit.toString() else typed + digit
        return if (next.length > MAX_DIGITS) this else copy(typed = next)
    }

    fun backspace(): WalletPad = copy(typed = typed.dropLast(1))

    /** the purse after the keys typed, spending more than there is leaves none and earning stops at the limit */
    fun change(purse: CoinPurse): CoinPurseChange =
        purse.change(coin, if (mode == WalletMode.SPEND) -amount else amount)
}

/** the total in gold is worth a line only while some coin other than gold takes part in it */
fun CoinPurseChange.showsTotal(): Boolean = Coin.entries.any { it != Coin.GOLD && (before[it] > 0 || after[it] > 0) }
