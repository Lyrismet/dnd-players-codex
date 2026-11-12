package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lyrismet.incadent.feature.codex.domain.grouping.duplicateNpcOf
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.presentation.form.twinOf
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.round.RoleParts
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_carry_kind
import incadent.shared.generated.resources.combat_carry_role
import incadent.shared.generated.resources.combat_identity_dup_link
import incadent.shared.generated.resources.combat_identity_dup_link_sub
import incadent.shared.generated.resources.combat_identity_dup_merge_sub
import incadent.shared.generated.resources.combat_identity_hint_codex
import incadent.shared.generated.resources.combat_identity_hint_label
import incadent.shared.generated.resources.combat_link_busy
import incadent.shared.generated.resources.combat_link_label_sub
import incadent.shared.generated.resources.combat_link_npc
import incadent.shared.generated.resources.combat_link_same_kind
import incadent.shared.generated.resources.combat_role_placeholder
import incadent.shared.generated.resources.combat_role_sub_species
import incadent.shared.generated.resources.combat_role_unset
import incadent.shared.generated.resources.npc_merge_button
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun identityState(
    member: Combatant,
    role: MemberRole,
    draft: IdentityDraft?,
    npcs: List<Npc>,
    species: List<Species>,
    facts: IdentityFacts,
): IdentityState? {
    val kind = identityKindOf(role.tie) ?: return null
    val kindRecord = species.firstOrNull { it.id == member.speciesId }
    val npc = member.npcId?.let { id -> npcs.firstOrNull { it.id == id } }
    val isCodex = kind == IdentityKind.CODEX
    val rename = renameState(member, npc, draft, npcs, species, isCodex)
    return if (isCodex) {
        IdentityState(kind, rename, role.parts.text(), roleField(member, kindRecord, npc), "", null, emptyList())
    } else {
        labelIdentity(member, rename, kindRecord, facts, freeCodexName(member, npcs))
    }
}

/** a name that is free in the codex, the only one that "В кодекс" can make a record of */
private fun freeCodexName(
    member: Combatant,
    npcs: List<Npc>,
): String? =
    member.name.trim().takeIf { it.isNotEmpty() && npcs.none { npc -> npc.name.equals(it, ignoreCase = true) } }

@Composable
private fun renameState(
    member: Combatant,
    npc: Npc?,
    draft: IdentityDraft?,
    npcs: List<Npc>,
    species: List<Species>,
    isCodex: Boolean,
): RenameState {
    val own = npc?.name ?: member.name
    val mine = draft?.takeIf { it.memberId == member.id }
    val typed = mine?.rename
    val changed = typed != null && typed.isNotBlank() && typed.trim() != own
    val twin = typed?.takeIf { changed }?.let { duplicateNpcOf(it, npcs, npc?.id) }
    return RenameState(
        value = typed ?: own,
        focus = mine?.focus == true,
        changed = changed,
        hint =
            if (changed && twin == null) {
                stringResource(
                    if (isCodex) Res.string.combat_identity_hint_codex else Res.string.combat_identity_hint_label,
                )
            } else {
                ""
            },
        duplicate =
            twin?.let {
                DuplicateOffer(
                    twin = twinOf(it, species),
                    action =
                        stringResource(
                            if (isCodex) Res.string.npc_merge_button else Res.string.combat_identity_dup_link,
                        ),
                    note =
                        if (isCodex) {
                            stringResource(Res.string.combat_identity_dup_merge_sub, own)
                        } else {
                            stringResource(Res.string.combat_identity_dup_link_sub)
                        },
                )
            },
    )
}

/** the class and subclass of the record a participant is tied to, or the ones it carries in the fight */
internal fun partsOf(
    member: Combatant,
    npc: Npc?,
): RoleParts =
    npc?.let { RoleParts(characterClass = it.characterClass, subclass = it.subclass) }
        ?: RoleParts.ofStored(member.role)

/** the class and subclass of a participant that is not a record of the codex, with where its kind comes from */
@Composable
internal fun roleField(
    member: Combatant,
    kindRecord: Species?,
    npc: Npc? = null,
): RoleField {
    val line = partsOf(member, npc).text()
    return RoleField(
        value = line.ifEmpty { stringResource(Res.string.combat_role_unset) },
        isSet = line.isNotEmpty(),
        sub =
            kindRecord?.let { stringResource(Res.string.combat_role_sub_species, it.name) }
                ?: stringResource(Res.string.combat_role_placeholder),
    )
}

private fun identityKindOf(tie: MemberTie): IdentityKind? =
    when (tie) {
        MemberTie.CODEX -> IdentityKind.CODEX
        MemberTie.LABELLED -> IdentityKind.LABELLED
        else -> null
    }

@Composable
private fun labelIdentity(
    member: Combatant,
    rename: RenameState,
    kindRecord: Species?,
    facts: IdentityFacts,
    toCodex: String?,
): IdentityState {
    val role = roleField(member, kindRecord)
    val roleChip =
        role.takeIf { it.isSet }?.let {
            CarryChip(
                stringResource(Res.string.combat_carry_role, it.value.lowercase()),
                true,
            )
        }
    val kindChip =
        kindRecord?.let {
            CarryChip(
                stringResource(Res.string.combat_carry_kind, it.name.lowercase()),
                false,
            )
        }
    val carry =
        listOfNotNull(roleChip, kindChip) +
            carryChips(facts.combat, member, kindRecord, facts.learned, facts.typeNames).map { CarryChip(it, false) }
    return IdentityState(
        kind = IdentityKind.LABELLED,
        rename = rename,
        linkSub = "",
        role = role,
        labelSub = stringResource(Res.string.combat_link_label_sub, member.name),
        toCodex = toCodex,
        carry = carry,
    )
}

/** the npcs to tie a participant to, the ones of its own kind first and the ones already in the fight grey */
@Composable
internal fun linkSheet(
    member: Combatant,
    combat: Combat,
    npcs: List<Npc>,
    species: List<Species>,
): LinkSheet {
    val inFight = combat.combatants.mapNotNullTo(mutableSetOf()) { it.npcId }
    val kinds = species.associate { it.id to it.name }
    val plain = stringResource(Res.string.combat_link_npc)
    val same = stringResource(Res.string.combat_link_same_kind)
    val busyText = stringResource(Res.string.combat_link_busy)
    val sorted =
        remember(npcs, member.speciesId) {
            npcs.sortedWith(
                compareByDescending<Npc> { member.speciesId != null && it.speciesId == member.speciesId }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
            )
        }
    return LinkSheet(
        sorted.map { npc ->
            val busy = npc.id in inFight
            val kind = npc.speciesId?.let(kinds::get)
            LinkOption(
                npcId = npc.id,
                name = npc.name,
                subtitle =
                    when {
                        busy -> busyText
                        member.speciesId != null && npc.speciesId == member.speciesId -> same
                        else -> kind?.let { "$plain · $it" } ?: plain
                    },
                busy = busy,
            )
        },
    )
}
