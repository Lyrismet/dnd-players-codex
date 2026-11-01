package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.debounce.rememberDebounced
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.presentation.summary.speciesTypeLabels
import com.lyrismet.incadent.feature.combat.domain.grouping.baseName
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.StartReadiness
import com.lyrismet.incadent.feature.combat.domain.round.startReadiness
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.mentionEntitiesFrom
import com.lyrismet.incadent.feature.mention.domain.mentionsIn
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_party_presence_away
import dndplayerscodex.shared.generated.resources.codex_party_presence_in
import dndplayerscodex.shared.generated.resources.combat_hp_preview_now
import dndplayerscodex.shared.generated.resources.combat_hp_preview_over
import dndplayerscodex.shared.generated.resources.combat_hp_preview_temp
import dndplayerscodex.shared.generated.resources.combat_hp_preview_unset
import dndplayerscodex.shared.generated.resources.combat_prep_ally_npc
import dndplayerscodex.shared.generated.resources.combat_prep_ally_species
import dndplayerscodex.shared.generated.resources.combat_prep_ally_temp
import dndplayerscodex.shared.generated.resources.combat_prep_enemy_npc
import dndplayerscodex.shared.generated.resources.combat_prep_enemy_species
import dndplayerscodex.shared.generated.resources.combat_prep_enemy_temp
import dndplayerscodex.shared.generated.resources.combat_prep_found_already
import dndplayerscodex.shared.generated.resources.combat_prep_found_hp
import dndplayerscodex.shared.generated.resources.combat_prep_found_in_combat
import dndplayerscodex.shared.generated.resources.combat_prep_found_mentioned
import dndplayerscodex.shared.generated.resources.combat_prep_found_npc_enemy
import dndplayerscodex.shared.generated.resources.combat_prep_found_npc_friend
import dndplayerscodex.shared.generated.resources.combat_prep_found_npc_neutral
import dndplayerscodex.shared.generated.resources.combat_prep_found_species_type
import dndplayerscodex.shared.generated.resources.combat_prep_group_range
import dndplayerscodex.shared.generated.resources.combat_prep_hp_label
import dndplayerscodex.shared.generated.resources.combat_prep_hp_temp
import dndplayerscodex.shared.generated.resources.combat_prep_hp_unknown
import dndplayerscodex.shared.generated.resources.combat_prep_hp_value
import dndplayerscodex.shared.generated.resources.combat_prep_party_sub_in
import dndplayerscodex.shared.generated.resources.combat_prep_party_sub_out
import dndplayerscodex.shared.generated.resources.combat_prep_party_you
import dndplayerscodex.shared.generated.resources.combat_prep_start_go
import dndplayerscodex.shared.generated.resources.combat_prep_start_need_enemy
import dndplayerscodex.shared.generated.resources.combat_prep_start_need_order
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

internal class PrepRows(
    val party: List<PrepPartyRow>,
    val allies: List<PrepMemberRow>,
    val enemies: List<PrepMemberRow>,
)

@Composable
internal fun prepRows(
    combat: Combat?,
    party: List<PartyMember>,
): PrepRows {
    val combatants = combat?.combatants.orEmpty()
    val inLabel = stringResource(Res.string.codex_party_presence_in)
    val awayLabel = stringResource(Res.string.codex_party_presence_away)
    val partyRows =
        party.map { member ->
            val seat = combatants.firstOrNull { it.partyMemberId == member.id }
            val included = seat != null
            val presence = if (member.presence == PartyPresence.IN) inLabel else awayLabel
            PrepPartyRow(
                partyMemberId = member.id,
                name =
                    if (member.isPlayerCharacter) {
                        stringResource(
                            Res.string.combat_prep_party_you,
                            member.name,
                        )
                    } else {
                        member.name
                    },
                sub =
                    if (included) {
                        stringResource(Res.string.combat_prep_party_sub_in, member.characterClass)
                    } else {
                        stringResource(
                            Res.string.combat_prep_party_sub_out,
                            member.characterClass,
                            presence.lowercase(),
                        )
                    },
                included = included,
                hp = if (member.isPlayerCharacter) partyHp(member, seat?.tempHp ?: 0) else null,
            )
        }
    return PrepRows(partyRows, memberRows(combatants, CombatSide.ALLY), memberRows(combatants, CombatSide.ENEMY))
}

@Composable
private fun partyHp(
    member: PartyMember,
    tempHp: Int,
): PrepHp {
    val max = member.hpMax
    val value =
        if (max == null) {
            stringResource(Res.string.combat_prep_hp_unknown)
        } else {
            stringResource(Res.string.combat_prep_hp_value, member.hpCurrent ?: max, max)
        }
    val caption =
        if (tempHp >
            0
        ) {
            stringResource(Res.string.combat_prep_hp_temp, tempHp)
        } else {
            stringResource(Res.string.combat_prep_hp_label)
        }
    return PrepHp(value, caption)
}

@Composable
private fun memberRows(
    combatants: List<Combatant>,
    side: CombatSide,
): List<PrepMemberRow> {
    val ally = side == CombatSide.ALLY
    val npcSub = stringResource(if (ally) Res.string.combat_prep_ally_npc else Res.string.combat_prep_enemy_npc)
    val speciesSub =
        stringResource(if (ally) Res.string.combat_prep_ally_species else Res.string.combat_prep_enemy_species)
    val tempSub = stringResource(if (ally) Res.string.combat_prep_ally_temp else Res.string.combat_prep_enemy_temp)
    return combatants
        .filter { it.side == side }
        .groupBy { it.groupKey() }
        .map { (key, members) ->
            val first = members.first()
            val base = first.name.baseName()
            val kind =
                when {
                    first.npcId != null -> npcSub
                    first.speciesId != null -> speciesSub
                    else -> tempSub
                }
            PrepMemberRow(
                key = key,
                name = base,
                sub =
                    if (members.size > 1) {
                        stringResource(Res.string.combat_prep_group_range, kind, base, members.size.toRomanNumeral())
                    } else {
                        kind
                    },
                count = members.size,
                canIncrease = first.npcId == null,
            )
        }
}

/** what the search of one side shows: the records that fit it, the named ones in the live session first */
@Composable
internal fun searchState(
    draft: SearchDraft,
    combat: Combat?,
    npcs: List<Npc>,
    species: List<Species>,
    mentioned: Set<String>,
    reinforcement: Boolean = false,
): FoeSearchState {
    val side = draft.side.toCombatSide()
    val ally = draft.side == PrepSide.ALLY
    val members = combat?.combatants.orEmpty().filter { it.side == side }
    val query = rememberDebounced(draft.query.normalized()) { it.isEmpty() }
    val typeLabels = speciesTypeLabels()
    val npcRows =
        npcs
            .filter { it.lifeState == NpcLifeState.ALIVE && (it.status == NpcStatus.ENEMY) != ally }
            .filter { query.isEmpty() || it.name.normalized().contains(query) }
            .map { npc ->
                val kind =
                    when {
                        !ally -> stringResource(Res.string.combat_prep_found_npc_enemy)
                        npc.status == NpcStatus.FRIEND -> stringResource(Res.string.combat_prep_found_npc_friend)
                        else -> stringResource(Res.string.combat_prep_found_npc_neutral)
                    }
                val count = members.count { it.npcId == npc.id }
                FoeRow(
                    npcKey(npc.id),
                    npc.name,
                    foundSub(kind, null, npcKey(npc.id) in mentioned, count > 0, reinforcement),
                    isSpecies = false,
                    count = count,
                )
            }.sortedWith(compareByDescending<FoeRow> { it.key in mentioned }.thenBy { it.name.lowercase() })
    val speciesRows =
        species
            .filter { query.isEmpty() || it.name.normalized().contains(query) }
            .map { kind ->
                val key = speciesKey(kind.id)
                val base =
                    stringResource(Res.string.combat_prep_found_species_type, typeLabels.of(kind))
                FoeRow(
                    key,
                    kind.name,
                    foundSub(base, kind.hitPoints, key in mentioned, false, reinforcement),
                    isSpecies = true,
                    members.count {
                        it.speciesId ==
                            kind.id
                    },
                )
            }.sortedWith(compareByDescending<FoeRow> { it.key in mentioned }.thenBy { it.name.lowercase() })
    val exact =
        query.isNotEmpty() &&
            (npcs.any { it.name.normalized() == query } || species.any { it.name.normalized() == query })
    return FoeSearchState(
        side = draft.side,
        query = draft.query,
        rows = npcRows + speciesRows,
        canCreate = query.isNotEmpty() && !exact,
        done = members.size,
        reinforcement = reinforcement,
    )
}

@Composable
private fun foundSub(
    base: String,
    hitPoints: Int?,
    mentioned: Boolean,
    inCombat: Boolean,
    reinforcement: Boolean,
): String {
    var text = base
    if (hitPoints != null) text = stringResource(Res.string.combat_prep_found_hp, text, hitPoints)
    if (mentioned) text = stringResource(Res.string.combat_prep_found_mentioned, text)
    if (inCombat) {
        text =
            stringResource(
                if (reinforcement) Res.string.combat_prep_found_already else Res.string.combat_prep_found_in_combat,
                text,
            )
    }
    return text
}

internal data class SearchDraft(
    val side: PrepSide,
    val query: String = "",
)

internal fun PrepSide.toCombatSide(): CombatSide =
    when (this) {
        PrepSide.ALLY -> CombatSide.ALLY
        PrepSide.ENEMY -> CombatSide.ENEMY
    }

private fun String.normalized(): String = trim().lowercase().replace('ё', 'е')

/** the keypad of the hit points: what it shows for the typed number, the character's numbers now and the limit */
@Composable
internal fun hpPadState(
    draft: HpPadDraft?,
    party: List<PartyMember>,
    combat: Combat?,
): HpPadState? {
    val member = draft?.let { d -> party.firstOrNull { it.id == d.partyMemberId } } ?: return null
    val value = draft.expr.value()
    val max = member.hpMax
    val current = member.hpCurrent ?: max
    val preview =
        when {
            draft.mode == HpMode.TEMP ->
                stringResource(
                    Res.string.combat_hp_preview_temp,
                    combat?.combatants?.firstOrNull { it.partyMemberId == member.id }?.tempHp ?: 0,
                )
            max == null || current == null -> stringResource(Res.string.combat_hp_preview_unset)
            value > max -> stringResource(Res.string.combat_hp_preview_over, current, max)
            else -> stringResource(Res.string.combat_hp_preview_now, current, max)
        }
    return HpPadState(
        name = member.name,
        mode = draft.mode,
        result = if (draft.expr.raw.isEmpty()) "0" else value.toString(),
        preview = preview,
        canApply = draft.expr.raw.isNotEmpty(),
    )
}

/** the participants in the order the master named them, the party first, then allies and enemies */
@Composable
internal fun prepOrder(
    combat: Combat?,
    party: List<PartyMember>,
): List<PrepOrderRow>? {
    val members = combat?.combatants.orEmpty()
    if (members.none { it.side == CombatSide.ENEMY }) return null
    val allyNpc = stringResource(Res.string.combat_prep_ally_npc)
    val allySpecies = stringResource(Res.string.combat_prep_ally_species)
    val allyTemp = stringResource(Res.string.combat_prep_ally_temp)
    val enemyNpc = stringResource(Res.string.combat_prep_enemy_npc)
    val enemySpecies = stringResource(Res.string.combat_prep_enemy_species)
    val enemyTemp = stringResource(Res.string.combat_prep_enemy_temp)
    return members.sortedWith(compareBy({ it.side.ordinal }, { it.id })).map { member ->
        val ally = member.side == CombatSide.ALLY
        val own = party.firstOrNull { it.id == member.partyMemberId }
        val sub =
            when {
                own != null -> own.characterClass
                member.npcId != null -> if (ally) allyNpc else enemyNpc
                member.speciesId != null -> if (ally) allySpecies else enemySpecies
                else -> if (ally) allyTemp else enemyTemp
            }
        PrepOrderRow(
            memberId = member.id,
            name =
                if (own?.isPlayerCharacter ==
                    true
                ) {
                    stringResource(Res.string.combat_prep_party_you, member.name)
                } else {
                    member.name
                },
            sub = sub,
            side =
                if (member.side ==
                    CombatSide.PARTY
                ) {
                    OrderSide.PARTY
                } else if (ally) {
                    OrderSide.ALLY
                } else {
                    OrderSide.ENEMY
                },
            number = member.turnOrder,
        )
    }
}

/** what keeps the fight from starting, or how many take part */
@Composable
internal fun prepStart(combat: Combat?): PrepStart =
    when (val readiness = startReadiness(combat?.combatants.orEmpty())) {
        StartReadiness.NeedEnemy -> PrepStart(stringResource(Res.string.combat_prep_start_need_enemy), ready = false)
        is StartReadiness.NeedOrder ->
            PrepStart(stringResource(Res.string.combat_prep_start_need_order, readiness.left), ready = false)
        is StartReadiness.Ready ->
            PrepStart(
                pluralStringResource(Res.plurals.combat_prep_start_go, readiness.count, readiness.count),
                ready = true,
            )
    }

/** the keys of the records the live session's notes mention, the search puts them first */
internal fun mentionedKeys(
    bodies: List<String>,
    npcs: List<Npc>,
    species: List<Species>,
    party: List<PartyMember>,
): Set<String> {
    val index = MentionIndex(mentionEntitiesFrom(npcs, emptyList(), emptyList(), party, species))
    return mentionsIn(bodies, index)
        .mapNotNull { entity ->
            when (entity.ref.type) {
                EntityKind.NPC -> npcKey(entity.ref.id)
                EntityKind.SPECIES -> speciesKey(entity.ref.id)
                else -> null
            }
        }.toSet()
}
