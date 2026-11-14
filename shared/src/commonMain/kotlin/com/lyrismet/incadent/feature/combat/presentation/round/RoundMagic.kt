package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.FifthEditionRules
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.isSpellTabVisible
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSpellRepository
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.MarkKind
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import com.lyrismet.incadent.feature.combat.domain.round.markFor
import com.lyrismet.incadent.feature.me.domain.SpellPay
import com.lyrismet.incadent.feature.me.domain.slotColumns
import com.lyrismet.incadent.feature.me.domain.spellLevels
import com.lyrismet.incadent.feature.me.presentation.spells.SpellsBlockState
import com.lyrismet.incadent.feature.me.presentation.spells.SpellsContext
import com.lyrismet.incadent.feature.me.presentation.spells.SpellsEvent
import com.lyrismet.incadent.feature.me.presentation.spells.SpellsHandler
import com.lyrismet.incadent.feature.me.presentation.spells.SpellsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf

/** the stores the magic tab reads and writes, the same ones the sheet of the character uses */
class MagicSources(
    val spells: CharacterSpellRepository,
    val resources: CharacterResourceRepository,
    val sheets: CharacterSheetRepository,
)

/** the spells of the player's own character in this fight, empty when the fight has none or it has no magic */
internal class RoundMagic(
    private val ownerId: Long?,
    private val state: SpellsBlockState?,
    private val context: SpellsContext?,
    private val handler: SpellsHandler,
) {
    /** only the participant that is "me" has the tab */
    fun stateFor(memberId: Long): SpellsBlockState? = state?.takeIf { memberId == ownerId }

    fun handle(event: SpellsEvent) {
        context?.let { handler.handle(event, it) }
    }
}

@Suppress("LongParameterList")
@Composable
internal fun rememberRoundMagic(
    sources: MagicSources,
    party: List<PartyMember>,
    own: Combatant?,
    combat: Combat?,
    combatRepository: CombatRepository,
    undoController: UndoController,
    scope: CoroutineScope,
): RoundMagic {
    val ui = remember { SpellsUiState() }
    val character = party.firstOrNull { it.id == own?.partyMemberId && it.isPlayerCharacter }
    val id = character?.id
    val sheet by remember(id) { id?.let(sources.sheets::observeSheet) ?: flowOf(CharacterSheet()) }
        .collectAsState(initial = CharacterSheet())
    val resources by remember(id) { id?.let(sources.resources::observe) ?: flowOf(emptyList()) }
        .collectAsState(initial = emptyList())
    val spells by remember(id) { id?.let(sources.spells::observe) ?: flowOf(emptyList()) }
        .collectAsState(initial = emptyList())
    val latestOwn by rememberUpdatedState(own)
    val latestCombat by rememberUpdatedState(combat)
    val characterLevel by rememberUpdatedState(character?.level ?: 1)
    val handler =
        remember(ui) {
            SpellsHandler(ui, scope, sources.resources, sources.spells, undoController) { spell, pay ->
                val member = latestOwn
                val round = latestCombat?.round
                if (member != null &&
                    round != null
                ) {
                    putMark(
                        spell,
                        pay,
                        characterLevel,
                    ) { kind, rounds -> combatRepository.addMark(member.id, kind, spell.name, rounds, round) }
                }
            }
        }
    if (character == null) return RoundMagic(null, null, null, handler)
    val columns = slotColumns(resources)
    val shown = isSpellTabVisible(sheet) && (columns.isNotEmpty() || spells.isNotEmpty())
    val level = character.level ?: 1
    val state =
        SpellsBlockState(
            ability = sheet.spellAbility,
            saveDc = FifthEditionRules.spellSaveDc(sheet, level),
            attack = FifthEditionRules.spellAttack(sheet, level),
            characterLevel = level,
            columns = columns,
            spells = spells,
            openLevel = ui.openLevel?.takeIf { it in spellLevels(spells, columns) },
            card = spells.firstOrNull { it.id == ui.cardId },
            draft = ui.draft,
            readOnly = false,
            tilesEditable = false,
        )
    return RoundMagic(
        own?.id,
        state.takeIf { shown },
        SpellsContext(character.id, spells, columns, readOnly = false),
        handler,
    )
}

// a spell that lasts puts a counted mark on the caster, a concentration one replaces the last, the other stays violet
private suspend fun putMark(
    spell: CharacterSpell,
    pay: SpellPay,
    characterLevel: Int,
    add: suspend (MarkKind, Int?) -> Unit,
) {
    val circle = (pay as? SpellPay.Slot)?.level ?: spell.level
    spell.markFor(circle, characterLevel)?.let { add(it.kind, it.rounds) }
}
