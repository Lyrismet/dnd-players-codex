package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_party_you_label
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
        )
    return buildEntitySummary(ref, lookup)
}
