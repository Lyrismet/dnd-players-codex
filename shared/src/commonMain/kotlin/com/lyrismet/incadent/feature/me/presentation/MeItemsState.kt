package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.CoinPurse
import com.lyrismet.incadent.feature.codex.presentation.item.ItemSheetState
import com.lyrismet.incadent.feature.me.domain.WalletMode
import com.lyrismet.incadent.feature.mention.presentation.MentionField

/** the "things" tab: what the character carries, the purse and the notes */
data class MeItems(
    val rows: List<MeItemRow>,
    val purse: CoinPurse,
    val notes: String,
    // the sheet to add or edit one item, closed while null
    val sheet: ItemSheetState?,
    val mention: MentionField,
)

data class MeItemRow(
    val id: Long,
    val name: String,
    val quantity: Int,
    val price: Int?,
    val coin: Coin,
)

sealed interface RestEvent {
    data object Opened : RestEvent

    data object LogToggled : RestEvent

    data object Confirmed : RestEvent

    data object Closed : RestEvent
}

sealed interface WalletEvent {
    data object Opened : WalletEvent

    data class ModeChanged(
        val mode: WalletMode,
    ) : WalletEvent

    data class CoinChosen(
        val coin: Coin,
    ) : WalletEvent

    data object MoreToggled : WalletEvent

    data class Digit(
        val digit: Int,
    ) : WalletEvent

    data object Backspace : WalletEvent

    data object Cleared : WalletEvent

    data object Applied : WalletEvent

    data object Closed : WalletEvent
}
