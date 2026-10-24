package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.domain.results.CombatFacts
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_party_you_label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/** the records plus resolved labels a summary is built from - composable only because the labels come from resources */
@Composable
fun entityLookupOf(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    sessionNotes: List<SessionNote>,
    sessionEntries: List<SessionEntry>,
    mentionIndex: MentionIndex,
    sessionNumbering: SessionNumbering,
    parties: List<PartyMember>,
    species: List<Species> = emptyList(),
    facts: CombatFacts = CombatFacts.EMPTY,
): EntityLookup =
    EntityLookup(
        npcs = npcs,
        locations = locations,
        quests = quests,
        npcStatusLabels = npcStatusLabels(),
        npcLifeLabels = npcLifeLabels(),
        questStatusLabels = questStatusLabels(),
        parties = parties,
        partyPresenceLabels = partyPresenceLabels(),
        partyYouLabel = stringResource(Res.string.entity_sheet_party_you_label),
        sessionNotes = sessionNotes,
        sessionEntries = sessionEntries,
        mentionIndex = mentionIndex,
        sessionNumbering = sessionNumbering,
        species = species,
        facts = facts,
    )

/** the shared "resolve whatever's tapped" used by every screen that owns a nullable [EntityRef] selection */
@Composable
fun selectedEntitySummary(
    ref: EntityRef?,
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    sessionNotes: List<SessionNote>,
    sessionEntries: List<SessionEntry>,
    mentionIndex: MentionIndex,
    sessionNumbering: SessionNumbering,
    parties: List<PartyMember>,
    species: List<Species> = emptyList(),
    facts: CombatFacts = CombatFacts.EMPTY,
): EntitySummaryItem? {
    if (ref == null) return null
    val lookup =
        entityLookupOf(
            npcs,
            locations,
            quests,
            sessionNotes,
            sessionEntries,
            mentionIndex,
            sessionNumbering,
            parties,
            species,
            facts,
        )
    return rememberEntitySummary(ref, lookup)
}

/** the summary of [ref] built off the main thread and only when the ref or the records change, null for no ref */
@Composable
fun rememberEntitySummary(
    ref: EntityRef?,
    lookup: EntityLookup,
): EntitySummaryItem? {
    val summary by produceState<EntitySummaryItem?>(null, ref, lookup) {
        value = ref?.let { withContext(Dispatchers.Default) { buildEntitySummary(it, lookup) } }
    }
    return if (ref == null) null else summary
}
