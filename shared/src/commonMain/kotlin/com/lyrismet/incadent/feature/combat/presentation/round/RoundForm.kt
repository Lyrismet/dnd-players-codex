package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormController
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormState
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.presentation.prep.FoeFormHost

/** the codex form a reinforcement is made in: what drives it and what it shows now */
internal class RoundForm(
    val host: FoeFormHost,
    val state: CodexEntryFormState?,
    val kinds: List<EntityKind>,
)

@Composable
internal fun rememberRoundForm(
    codexRepositories: CodexRepositories,
    editor: RoundEditor,
    imageCompressor: ImageCompressor,
    party: List<PartyMember>,
    npcs: List<Npc>,
    species: List<Species>,
): RoundForm {
    val locations by codexRepositories.locationRepository.observeAll().collectAsState(initial = emptyList())
    val target: MutableState<CombatSide?> = remember { mutableStateOf(null) }
    val controller =
        CodexEntryFormController.rememberController(
            codexRepositories.npcRepository,
            codexRepositories.partyRepository,
            codexRepositories.questRepository,
            codexRepositories.locationRepository,
            codexRepositories.speciesRepository,
            remember { mutableStateOf(emptyList()) },
            party.firstOrNull { it.isPlayerCharacter }?.level,
            party.any { it.isPlayerCharacter },
        )
    val host = FoeFormHost(controller, target, editor, imageCompressor)
    return RoundForm(host, controller.buildState(npcs, locations, species), host.kinds())
}
