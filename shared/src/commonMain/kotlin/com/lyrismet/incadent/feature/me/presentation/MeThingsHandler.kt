package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.presentation.item.ItemsController
import com.lyrismet.incadent.feature.me.domain.WalletPad
import kotlinx.coroutines.CoroutineScope

/** reacts to the presses of the rest sheet, the wallet keypad and the things tab */
internal class MeThingsHandler(
    private val ui: MeUiState,
    private val actions: MeActions,
    private val rests: MeRestActions,
    private val itemActions: MeItemActions,
    private val items: ItemsController,
    private val scope: CoroutineScope,
) {
    fun handle(
        event: MeEvent,
        member: PartyMember,
        data: MeEventScope,
    ) {
        when (event) {
            is MeEvent.Rest -> rest(event.event, member, data.liveSessionId)
            is MeEvent.Wallet -> wallet(event.event, member)
            is MeEvent.Items -> items.handle(event.event, scope, data.records)
            is MeEvent.ItemStepped -> step(event, data)
            is MeEvent.ItemDeleted -> itemActions.delete(event.id)
            else -> Unit
        }
    }

    private fun rest(
        event: RestEvent,
        member: PartyMember,
        liveSessionId: Long?,
    ) {
        val pad = ui.rest
        when (event) {
            RestEvent.Opened -> ui.rest = RestPad()
            RestEvent.LogToggled -> ui.rest = pad?.copy(log = !pad.log)
            RestEvent.Closed -> ui.rest = null
            RestEvent.Confirmed -> {
                val open = pad ?: return
                ui.rest = null
                rests.rest(member, liveSessionId, open.log)
            }
        }
    }

    private fun wallet(
        event: WalletEvent,
        member: PartyMember,
    ) {
        val pad = ui.wallet
        when (event) {
            WalletEvent.Opened -> ui.wallet = WalletPad()
            WalletEvent.Closed -> ui.wallet = null
            WalletEvent.Applied -> {
                if (pad != null && pad.canApply) {
                    actions.sheet(member.id) { it.copy(purse = pad.change(it.purse).after) }
                }
                ui.wallet = null
            }
            else -> ui.wallet = pad?.press(event)
        }
    }

    // the last piece of a stack deletes the item, the codex card does the same
    private fun step(
        event: MeEvent.ItemStepped,
        data: MeEventScope,
    ) {
        val quantity =
            data.records.items
                .find { it.id == event.id }
                ?.quantity ?: return
        if (event.delta < 0 && quantity <= 1) {
            itemActions.delete(event.id)
        } else {
            items.stepQuantity(scope, event.id, event.delta)
        }
    }
}

private fun WalletPad.press(event: WalletEvent): WalletPad =
    when (event) {
        is WalletEvent.ModeChanged -> copy(mode = event.mode)
        is WalletEvent.CoinChosen -> copy(coin = event.coin)
        WalletEvent.MoreToggled -> copy(moreOpen = !moreOpen)
        is WalletEvent.Digit -> digit(event.digit)
        WalletEvent.Backspace -> backspace()
        WalletEvent.Cleared -> copy(typed = "")
        else -> this
    }
