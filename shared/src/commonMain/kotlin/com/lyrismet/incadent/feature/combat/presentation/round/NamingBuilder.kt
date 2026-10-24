package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.presentation.summary.damageStateLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.npcStatusLabels
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.LearnedVulnerability
import com.lyrismet.incadent.feature.combat.domain.round.NameChoice
import com.lyrismet.incadent.feature.combat.domain.round.nameChoices
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_mult_immunity
import dndplayerscodex.shared.generated.resources.combat_mult_resistance
import dndplayerscodex.shared.generated.resources.combat_mult_vulnerability
import dndplayerscodex.shared.generated.resources.combat_name_carry_note
import dndplayerscodex.shared.generated.resources.combat_name_carry_type
import dndplayerscodex.shared.generated.resources.combat_name_existing_sub
import dndplayerscodex.shared.generated.resources.combat_name_existing_sub_species
import dndplayerscodex.shared.generated.resources.combat_name_new
import dndplayerscodex.shared.generated.resources.combat_name_new_sub
import dndplayerscodex.shared.generated.resources.combat_name_new_sub_species
import dndplayerscodex.shared.generated.resources.combat_name_plain
import dndplayerscodex.shared.generated.resources.combat_promo_all
import dndplayerscodex.shared.generated.resources.combat_promo_confirm_known
import dndplayerscodex.shared.generated.resources.combat_promo_confirm_new
import dndplayerscodex.shared.generated.resources.combat_promo_hp
import dndplayerscodex.shared.generated.resources.combat_promo_hp_hint
import dndplayerscodex.shared.generated.resources.combat_promo_note
import dndplayerscodex.shared.generated.resources.combat_promo_note_hint
import dndplayerscodex.shared.generated.resources.combat_promo_note_quoted
import dndplayerscodex.shared.generated.resources.combat_promo_only
import dndplayerscodex.shared.generated.resources.combat_promo_over_known
import dndplayerscodex.shared.generated.resources.combat_promo_over_new
import dndplayerscodex.shared.generated.resources.combat_promo_relation
import dndplayerscodex.shared.generated.resources.combat_promo_species
import dndplayerscodex.shared.generated.resources.combat_promo_species_hint
import org.jetbrains.compose.resources.stringResource

/** a participant of the codex is not offered a name again, and neither is one of the party */
internal fun Combatant.canBeNamed() = side != CombatSide.PARTY && npcId == null

private fun DamageState.hasMultiplier() =
    this == DamageState.RESISTANCE || this == DamageState.IMMUNITY || this == DamageState.VULNERABILITY

@Composable
private fun DamageState.multiplierText() =
    when (this) {
        DamageState.RESISTANCE -> stringResource(Res.string.combat_mult_resistance)
        DamageState.IMMUNITY -> stringResource(Res.string.combat_mult_immunity)
        else -> stringResource(Res.string.combat_mult_vulnerability)
    }

/** the learned entries that still hold on the species card, so there is something to move to a new npc */
internal fun learnedOf(
    member: Combatant,
    species: Species?,
    learned: List<LearnedVulnerability>,
): List<LearnedVulnerability> =
    learned.filter { item ->
        val now = species?.vulnerabilities?.get(item.type)
        item.speciesId == member.speciesId && now?.hasMultiplier() == true && now != item.previous
    }

@Composable
internal fun namingState(
    combat: Combat,
    member: Combatant,
    typed: String,
    npcs: List<Npc>,
    species: List<Species>,
    learned: List<LearnedVulnerability>,
    typeNames: Map<DamageType, String>,
): NamingState? {
    if (!member.canBeNamed()) return null
    val speciesRecord = species.firstOrNull { it.id == member.speciesId }
    val linkedNpcIds = combat.combatants.mapNotNullTo(mutableSetOf()) { it.npcId }
    val speciesNames = species.associate { it.id to it.name.lowercase() }
    val options =
        nameChoices(typed, member.speciesId, npcs, linkedNpcIds).map { choice ->
            when (choice) {
                is NameChoice.New ->
                    NameOption(
                        choice,
                        stringResource(Res.string.combat_name_new, choice.name),
                        speciesRecord?.let {
                            stringResource(
                                Res.string.combat_name_new_sub_species,
                                it.name.lowercase(),
                            )
                        }
                            ?: stringResource(Res.string.combat_name_new_sub),
                    )
                is NameChoice.Existing -> {
                    val of = npcs.firstOrNull { it.id == choice.npcId }?.speciesId?.let(speciesNames::get)
                    NameOption(
                        choice,
                        choice.name,
                        of?.let { stringResource(Res.string.combat_name_existing_sub_species, it) }
                            ?: stringResource(Res.string.combat_name_existing_sub),
                    )
                }
                is NameChoice.Plain -> NameOption(choice, stringResource(Res.string.combat_name_plain), "")
            }
        }
    return NamingState(
        value = typed,
        placeholder = member.name,
        options = options,
        carry = if (typed.isBlank()) emptyList() else carryChips(combat, member, speciesRecord, learned, typeNames),
    )
}

@Composable
private fun carryChips(
    combat: Combat,
    member: Combatant,
    species: Species?,
    learned: List<LearnedVulnerability>,
    typeNames: Map<DamageType, String>,
): List<String> {
    val taken = combat.damageTaken(member)
    return listOfNotNull(
        if (taken > 0) tookText(taken) else null,
        if (member.state != CombatState.WHOLE) woundWord(member).first else null,
    ) +
        learnedOf(member, species, learned).mapNotNull { item ->
            species?.vulnerabilities?.get(item.type)?.let {
                stringResource(Res.string.combat_name_carry_type, typeNames.getValue(item.type), it.multiplierText())
            }
        } +
        listOfNotNull(if (member.note.isNotBlank()) stringResource(Res.string.combat_name_carry_note) else null)
}

@Composable
internal fun promotionSheet(
    draft: PromotionDraft,
    combat: Combat,
    npc: Npc?,
    species: Species?,
    typeNames: Map<DamageType, String>,
): PromotionSheet? {
    val member = combat.combatants.firstOrNull { it.id == draft.trace.memberId } ?: return null
    val stateLabels = damageStateLabels()
    return PromotionSheet(
        overline =
            stringResource(
                if (draft.isNew) Res.string.combat_promo_over_new else Res.string.combat_promo_over_known,
            ),
        name = draft.name,
        rows = promotionRows(draft, combat, member, npc, species),
        learned =
            draft.learned.mapNotNull { item ->
                val state = species?.vulnerabilities?.get(item.type) ?: return@mapNotNull null
                LearnedRow(
                    type = item.type,
                    state = state,
                    label = "${typeNames.getValue(item.type)} · ${stateLabels.getValue(state).lowercase()}",
                    onlyNpc = item.type in draft.onlyNpc,
                    allLabel = stringResource(Res.string.combat_promo_all, species.name.lowercase()),
                    onlyLabel = stringResource(Res.string.combat_promo_only),
                )
            },
        confirmLabel =
            stringResource(
                if (draft.isNew) Res.string.combat_promo_confirm_new else Res.string.combat_promo_confirm_known,
            ),
    )
}

@Composable
private fun promotionRows(
    draft: PromotionDraft,
    combat: Combat,
    member: Combatant,
    npc: Npc?,
    species: Species?,
): List<PromotionRow> {
    val taken = combat.damageTaken(member)
    val hp =
        listOfNotNull(
            if (taken > 0) tookText(taken) else null,
            if (member.state != CombatState.WHOLE) woundWord(member).first else null,
        ).joinToString(" · ")
    val note = member.note.trim()
    return listOf(
        PromotionRow(
            stringResource(Res.string.combat_promo_species),
            species?.name ?: "-",
            if (species != null) stringResource(Res.string.combat_promo_species_hint) else "",
        ),
        PromotionRow(
            stringResource(Res.string.combat_promo_hp),
            hp.ifEmpty { "-" },
            if (taken > 0) stringResource(Res.string.combat_promo_hp_hint) else "",
        ),
        PromotionRow(
            stringResource(Res.string.combat_promo_relation),
            npc?.let { npcStatusLabels().getValue(it.status) } ?: "-",
            "",
        ),
        PromotionRow(
            stringResource(Res.string.combat_promo_note),
            if (note.isEmpty()) "-" else stringResource(Res.string.combat_promo_note_quoted, note),
            if (draft.isNew && note.isNotEmpty()) stringResource(Res.string.combat_promo_note_hint) else "",
        ),
    )
}
