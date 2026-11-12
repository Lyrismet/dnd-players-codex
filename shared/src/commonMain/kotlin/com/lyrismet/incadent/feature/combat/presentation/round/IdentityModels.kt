package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.presentation.form.DuplicateNpc
import com.lyrismet.incadent.feature.combat.domain.model.Combat
import com.lyrismet.incadent.feature.combat.domain.model.LearnedVulnerability

enum class IdentityKind { CODEX, LABELLED }

/** what the panel offers to do with who a participant is */
data class IdentityState(
    val kind: IdentityKind,
    val rename: RenameState,
    // the role line of the record a participant of the codex is tied to
    val linkSub: String,
    // set for a label, the class and subclass told in one line
    val role: RoleField?,
    val labelSub: String,
    // the name that "В кодекс" makes a record of, null when the codex has it already
    val toCodex: String?,
    val carry: List<CarryChip>,
)

data class RenameState(
    val value: String,
    // the cursor goes into the field when the name in the header was pressed
    val focus: Boolean,
    val changed: Boolean,
    // empty while there is nothing to say under the field
    val hint: String,
    // the record of the codex that already has the typed name
    val duplicate: DuplicateOffer?,
)

/** the twin and what the button under it says it does */
data class DuplicateOffer(
    val twin: DuplicateNpc,
    val action: String,
    val note: String,
)

data class RoleField(
    val value: String,
    val isSet: Boolean,
    val sub: String,
)

data class CarryChip(
    val text: String,
    val gold: Boolean,
)

/** the npcs a participant can be tied to */
data class LinkSheet(
    val options: List<LinkOption>,
)

data class LinkOption(
    val npcId: Long,
    val name: String,
    val subtitle: String,
    // already in this fight under another row, shown grey and taking no press
    val busy: Boolean,
)

/** what the fight knows of a participant, to tell what it carries into the codex */
internal class IdentityFacts(
    val combat: Combat,
    val learned: List<LearnedVulnerability>,
    val typeNames: Map<DamageType, String>,
)
