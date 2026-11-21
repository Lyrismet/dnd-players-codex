package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.presentation.summary.npcStatusLabels
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.NameChoice
import com.lyrismet.incadent.feature.combat.domain.round.damageTaken
import com.lyrismet.incadent.feature.combat.domain.round.nameChoices
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_name_carry_note
import incadent.shared.generated.resources.combat_name_existing_sub
import incadent.shared.generated.resources.combat_name_existing_sub_species
import incadent.shared.generated.resources.combat_name_new
import incadent.shared.generated.resources.combat_name_new_sub
import incadent.shared.generated.resources.combat_name_new_sub_species
import incadent.shared.generated.resources.combat_name_plain
import incadent.shared.generated.resources.combat_promo_confirm_known
import incadent.shared.generated.resources.combat_promo_confirm_new
import incadent.shared.generated.resources.combat_promo_hp
import incadent.shared.generated.resources.combat_promo_note
import incadent.shared.generated.resources.combat_promo_note_quoted
import incadent.shared.generated.resources.combat_promo_over_known
import incadent.shared.generated.resources.combat_promo_over_new
import incadent.shared.generated.resources.combat_promo_relation
import incadent.shared.generated.resources.combat_promo_role
import incadent.shared.generated.resources.combat_promo_species
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun namingState(
    combat: Combat,
    member: Combatant,
    typed: String,
    npcs: List<Npc>,
    species: List<Species>,
    focus: Boolean = false,
): NamingState? {
    val speciesRecord = species.firstOrNull { it.id == member.speciesId }
    if (member.tie() != MemberTie.UNNAMED) return null
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
        role = roleField(member, speciesRecord),
        carry = if (typed.isBlank()) emptyList() else carryChips(combat, member),
        carryRole = if (typed.isBlank()) "" else member.role,
        focus = focus,
    )
}

@Composable
internal fun carryChips(
    combat: Combat,
    member: Combatant,
): List<String> {
    val taken = combat.damageTaken(member)
    return listOfNotNull(
        if (taken > 0) tookText(taken) else null,
        if (member.state != CombatState.WHOLE) woundWord(member).first else null,
    ) + listOfNotNull(if (member.note.isNotBlank()) stringResource(Res.string.combat_name_carry_note) else null)
}

@Composable
internal fun promotionSheet(
    draft: PromotionDraft,
    combat: Combat,
    npc: Npc?,
    species: Species?,
): PromotionSheet? {
    val member = combat.combatants.firstOrNull { it.id == draft.trace.memberId } ?: return null
    return PromotionSheet(
        overline =
            stringResource(
                if (draft.isNew) Res.string.combat_promo_over_new else Res.string.combat_promo_over_known,
            ),
        name = draft.name,
        rows = promotionRows(draft, combat, member, npc, species),
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
    return listOfNotNull(
        PromotionRow(
            stringResource(Res.string.combat_promo_species),
            species?.name ?: "-",
            "",
        ),
        PromotionRow(
            stringResource(Res.string.combat_promo_hp),
            hp.ifEmpty { "-" },
            "",
        ),
        PromotionRow(
            stringResource(Res.string.combat_promo_relation),
            npc?.let { npcStatusLabels().getValue(it.status) } ?: "-",
            "",
        ),
        member.role.takeIf { it.isNotBlank() && draft.isNew }?.let {
            PromotionRow(stringResource(Res.string.combat_promo_role), it, "")
        },
        PromotionRow(
            stringResource(Res.string.combat_promo_note),
            if (note.isEmpty()) "-" else stringResource(Res.string.combat_promo_note_quoted, note),
            "",
        ),
    )
}
