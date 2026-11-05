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
import com.lyrismet.incadent.feature.codex.domain.model.LongRest
import com.lyrismet.incadent.feature.me.domain.HitPointMode
import com.lyrismet.incadent.feature.me.domain.WalletPad
import com.lyrismet.incadent.feature.me.domain.hit
import kotlinx.coroutines.Job

/** what the screen remembers while open: the tab, the search, the open card and sheet, texts not saved yet */
internal class MeUiState(
    tab: MutableState<MeTab>,
) {
    var tab by tab
    var searchOpen by mutableStateOf(false)
    var query by mutableStateOf("")
    var popover by mutableStateOf<MeTile?>(null)
    var menuOpen by mutableStateOf(false)
    var edit by mutableStateOf<MeOverlay?>(null)
    var hitPad by mutableStateOf<HitPad?>(null)
    var slotLevel by mutableStateOf<Int?>(null)
    var rest by mutableStateOf<RestPad?>(null)
    var wallet by mutableStateOf<WalletPad?>(null)

    // a text is shown from here while it is typed, so the stored copy coming back does not move the cursor
    val textDrafts = mutableStateMapOf<MeText, String>()
    val textJobs = mutableMapOf<MeText, Job>()

    fun overlay(vitals: CharacterVitals): MeOverlay? = hitPad?.overlay(vitals) ?: edit
}

// saved by name so the tab survives on every platform
private val tabSaver =
    Saver<MutableState<MeTab>, String>(save = { it.value.name }, restore = { mutableStateOf(MeTab.valueOf(it)) })

@Composable
internal fun rememberMeUiState(): MeUiState {
    val tab = rememberSaveable(saver = tabSaver) { mutableStateOf(MeTab.SHEET) }
    return remember(tab) { MeUiState(tab) }
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

/** the long rest sheet: whether the note goes to the session going on */
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
