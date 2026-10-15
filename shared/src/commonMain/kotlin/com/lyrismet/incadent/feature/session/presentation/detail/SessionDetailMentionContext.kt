package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.presentation.style.LocationMentionColor
import com.lyrismet.incadent.feature.codex.presentation.style.PartyMemberColor
import com.lyrismet.incadent.feature.codex.presentation.style.toStatusColor
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.matchingMentionEntities
import com.lyrismet.incadent.feature.mention.domain.mentionEntitiesFrom
import com.lyrismet.incadent.feature.mention.domain.token
import com.lyrismet.incadent.feature.mention.domain.trailingMentionQuery
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_location
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_npc
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_party
import dndplayerscodex.shared.generated.resources.session_detail_mention_type_quest
import org.jetbrains.compose.resources.stringResource

internal data class MentionContext(
    val candidates: MentionIndex,
    val suggestions: List<SessionMentionSuggestion>,
    // the live records, in the order the "link to" list offers them
    val entities: List<MentionEntity>,
    // snapshots whose name a live record has taken again
)

/** resolves mention candidates once and, from the same data, the trailing-@-query autocomplete suggestions */
@Composable
internal fun buildMentionContext(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    parties: List<PartyMember>,
    draftText: String,
): MentionContext {
    val mentionEntities = mentionEntitiesFrom(npcs, locations, quests, parties)
    val candidates = MentionIndex(mentionEntities)

    val typeLabels =
        MentionTypeLabels(
            npc = stringResource(Res.string.session_detail_mention_type_npc),
            party = stringResource(Res.string.session_detail_mention_type_party),
            location = stringResource(Res.string.session_detail_mention_type_location),
            quest = stringResource(Res.string.session_detail_mention_type_quest),
        )
    val suggestions =
        trailingMentionQuery(draftText)
            ?.let { matchingMentionEntities(mentionEntities, it) }
            .orEmpty()
            .map { it.toSuggestion(typeLabels) }

    return MentionContext(candidates, suggestions, mentionEntities)
}

/** the name of each record kind the autocomplete shows next to a suggestion */
internal class MentionTypeLabels(
    val npc: String,
    val party: String,
    val location: String,
    val quest: String,
)

internal fun MentionEntity.toSuggestion(labels: MentionTypeLabels): SessionMentionSuggestion {
    val (glyph, tint, typeLabel) =
        when (this) {
            is MentionEntity.NpcMention -> Triple(MentionGlyph.NPC, status.toStatusColor().foreground, labels.npc)
            is MentionEntity.LocationMention ->
                Triple(MentionGlyph.LOCATION, LocationMentionColor.foreground, labels.location)
            is MentionEntity.QuestMention ->
                Triple(MentionGlyph.QUEST, status.toStatusColor().foreground, labels.quest)
            is MentionEntity.PartyMention -> Triple(MentionGlyph.PARTY, PartyMemberColor.foreground, labels.party)
            // a deleted record is never offered by the autocomplete, its candidates come from live records only
            is MentionEntity.DeletedMention -> Triple(MentionGlyph.DELETED, LocationMentionColor.foreground, "")
        }
    return SessionMentionSuggestion(
        token = token(),
        glyph = glyph,
        tint = tint,
        name = name,
        typeLabel = typeLabel,
    )
}
