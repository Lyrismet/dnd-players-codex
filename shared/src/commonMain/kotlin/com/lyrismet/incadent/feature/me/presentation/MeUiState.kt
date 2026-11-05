package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.lyrismet.incadent.core.calc.CalcExpression
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.LongRest
import com.lyrismet.incadent.feature.me.domain.HitPointMode
import com.lyrismet.incadent.feature.me.domain.WalletMode
import com.lyrismet.incadent.feature.me.domain.WalletPad
import com.lyrismet.incadent.feature.me.domain.hit
import kotlinx.coroutines.Job

/** what the screen remembers while open: the tab, the search, the open card and sheet, texts not saved yet */
internal class MeUiState(
    tab: MutableState<MeTab>,
    rest: MutableState<RestPad?>,
    wallet: MutableState<WalletPad?>,
) {
    var tab by tab
    var searchOpen by mutableStateOf(false)
    var query by mutableStateOf("")
    var popover by mutableStateOf<MeTile?>(null)
    var menuOpen by mutableStateOf(false)
    var edit by mutableStateOf<MeOverlay?>(null)
    var hitPad by mutableStateOf<HitPad?>(null)
    var slotLevel by mutableStateOf<Int?>(null)
    var rest by rest
    var wallet by wallet

    // a text is shown from here while it is typed, so the stored copy coming back does not move the cursor
    val textDrafts = mutableStateMapOf<MeText, String>()
    val textJobs = mutableMapOf<MeText, Job>()

    fun overlay(vitals: CharacterVitals): MeOverlay? = hitPad?.overlay(vitals) ?: edit
}

// the pads are saved as plain text so an open sheet survives a recreate
private val restSaver =
    Saver<MutableState<RestPad?>, String>(
        save = {
            it.value
                ?.log
                ?.toString()
                .orEmpty()
        },
        restore = { mutableStateOf(it.toBooleanStrictOrNull()?.let(::RestPad)) },
    )

private val walletSaver =
    Saver<MutableState<WalletPad?>, String>(
        save = { state -> state.value?.let { "${it.mode.name}|${it.coin.name}|${it.moreOpen}|${it.typed}" }.orEmpty() },
        restore = { mutableStateOf(parseWalletPad(it)) },
    )

private fun parseWalletPad(text: String): WalletPad? {
    val parts = text.split("|", limit = WALLET_PARTS)
    if (parts.size != WALLET_PARTS) return null
    val (mode, coin, more) = parts
    return runCatching {
        WalletPad(WalletMode.valueOf(mode), Coin.valueOf(coin), parts.last(), more.toBoolean())
    }.getOrNull()
}

private const val WALLET_PARTS = 4

// saved by name so the tab survives on every platform
private val tabSaver =
    Saver<MutableState<MeTab>, String>(save = { it.value.name }, restore = { mutableStateOf(MeTab.valueOf(it)) })

@Composable
internal fun rememberMeUiState(): MeUiState {
    val tab = rememberSaveable(saver = tabSaver) { mutableStateOf(MeTab.SHEET) }
    val rest = rememberSaveable(saver = restSaver) { mutableStateOf<RestPad?>(null) }
    val wallet = rememberSaveable(saver = walletSaver) { mutableStateOf<WalletPad?>(null) }
    return remember(tab, rest, wallet) { MeUiState(tab, rest, wallet) }
}

/** the keypad of the hit points: what was typed and which way it changes them */
internal data class HitPad(
    val mode: HitPointMode = HitPointMode.DAMAGE,
    val expression: CalcExpression = CalcExpression(),
) {
    fun overlay(vitals: CharacterVitals): MeOverlay.HitPoints {
        val after = vitals.hit(mode, expression.value())
        val temp = mode == HitPointMode.TEMP
        return MeOverlay.HitPoints(
            mode,
            expression.raw.ifEmpty { "0" },
            before = if (temp) vitals.tempHp else vitals.hpCurrent ?: 0,
            after = if (temp) after.tempHp else after.hpCurrent ?: 0,
            canApply = expression.raw.isNotEmpty(),
        )
    }
}

/** whether the long rest note goes to the session going on */
internal data class RestPad(
    val log: Boolean = false,
) {
    fun overlay(
        vitals: CharacterVitals,
        resources: List<CharacterResource>,
        liveSessionTitle: String?,
    ) = MeOverlay.Rest(
        summary = LongRest.plan(vitals, resources).summary,
        hasTemp = vitals.tempHp > 0,
        liveSessionTitle = liveSessionTitle,
        log = log && liveSessionTitle != null,
    )
}
