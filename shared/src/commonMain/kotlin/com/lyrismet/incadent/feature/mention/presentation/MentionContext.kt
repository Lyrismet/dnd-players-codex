package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.debounce.rememberDebounced
import com.lyrismet.incadent.core.format.kindLabelRes
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.presentation.style.LocationMentionColor
import com.lyrismet.incadent.feature.codex.presentation.style.PartyMemberColor
import com.lyrismet.incadent.feature.codex.presentation.style.SpeciesMentionColor
import com.lyrismet.incadent.feature.codex.presentation.style.toStatusColor
import com.lyrismet.incadent.feature.mention.domain.MentionCandidate
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.MentionSection
import com.lyrismet.incadent.feature.mention.domain.createRowShowsHint
import com.lyrismet.incadent.feature.mention.domain.emptyMentionRows
import com.lyrismet.incadent.feature.mention.domain.mentionDetails
import com.lyrismet.incadent.feature.mention.domain.mentionEntitiesFrom
import com.lyrismet.incadent.feature.mention.domain.mentionNicknames
import com.lyrismet.incadent.feature.mention.domain.mentionsIn
import com.lyrismet.incadent.feature.mention.domain.newestRecords
import com.lyrismet.incadent.feature.mention.domain.searchMentions
import com.lyrismet.incadent.feature.mention.domain.token
import com.lyrismet.incadent.feature.mention.domain.trailingMentionQuery
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.mention_quest_giver_format
import dndplayerscodex.shared.generated.resources.mention_section_codex
import dndplayerscodex.shared.generated.resources.mention_section_previous_session
import dndplayerscodex.shared.generated.resources.mention_section_recent
import dndplayerscodex.shared.generated.resources.mention_section_this_session
import dndplayerscodex.shared.generated.resources.session_detail_mention_suggestions_header
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

internal data class MentionContext(
    val candidates: MentionIndex,
    val suggestions: List<MentionDropdownItem>,
    // the live records, in the order the "link to" list offers them
    val entities: List<MentionEntity>,
)

/** the grey second lines of the "@" rows - the nicknames a query can hit and the details under a name */
data class MentionLookups(
    val nicknames: Map<MentionRef, List<String>>,
    val details: Map<MentionRef, String>,
)

@Composable
fun rememberMentionLookups(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    parties: List<PartyMember>,
): MentionLookups {
    val giverNames =
        remember(npcs, quests) {
            val byId = npcs.associateBy { it.id }
            quests.mapNotNull { quest -> quest.givenByNpcId?.let { byId[it]?.name } }.distinct()
        }
    val giverLines = giverNames.associateWith { stringResource(Res.string.mention_quest_giver_format, it) }
    val details =
        remember(npcs, locations, quests, parties, giverLines) {
            mentionDetails(npcs, locations, quests, parties) { giverLines[it].orEmpty() }
        }
    val nicknames = remember(npcs) { mentionNicknames(npcs) }
    return MentionLookups(nicknames, details)
}

/** the bodies of the notes the empty "@" dropdown learns from - this session's and the one before it */
internal data class MentionSessionBodies(
    val current: List<String>,
    val previous: List<String>,
)

private const val RECENT_LIMIT = 3

/** resolves mention candidates once and, from the same data, the trailing-@-query dropdown */
@Composable
internal fun buildMentionContext(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    parties: List<PartyMember>,
    species: List<Species>,
    draftText: String,
    caret: Int,
    sessions: MentionSessionBodies,
): MentionContext {
    val mentionEntities =
        remember(npcs, locations, quests, parties, species) {
            mentionEntitiesFrom(npcs, locations, quests, parties, species)
        }
    val candidates = remember(mentionEntities) { MentionIndex(mentionEntities) }

    val lookups = rememberMentionLookups(npcs, locations, quests, parties)
    val dropdown =
        rememberMentionDropdown(
            mentionEntities,
            candidates,
            lookups.nicknames,
            lookups.details,
            sessions,
            draftText,
            caret,
        )
    return MentionContext(candidates, dropdown, mentionEntities)
}

/** the rows under the trailing "@query" of [draftText], searched off the main thread once the query settles */
@Composable
internal fun rememberMentionDropdown(
    entities: List<MentionEntity>,
    index: MentionIndex,
    nicknames: Map<MentionRef, List<String>>,
    details: Map<MentionRef, String>,
    sessions: MentionSessionBodies,
    draftText: String,
    caret: Int,
): List<MentionDropdownItem> {
    val typeLabels =
        MentionTypeLabels(
            npc = stringResource(EntityKind.NPC.kindLabelRes()),
            party = stringResource(EntityKind.PARTY.kindLabelRes()),
            location = stringResource(EntityKind.LOCATION.kindLabelRes()),
            quest = stringResource(EntityKind.QUEST.kindLabelRes()),
            species = stringResource(EntityKind.SPECIES.kindLabelRes()),
        )
    val dropdownTexts = mentionDropdownTexts()
    val query = trailingMentionQuery(draftText, caret)
    // a non-blank query is passed on after a short pause, a closed or empty one at once
    val searchQuery = rememberDebounced(query) { it.isNullOrBlank() }
    // the search runs off the main thread and only for the settled query, so typing never waits for it
    val found by produceState(
        MentionSearchResult(null, emptyList()),
        searchQuery,
        entities,
        nicknames,
        details,
        sessions,
        typeLabels,
        dropdownTexts,
    ) {
        val input = MentionDropdownInput(entities, index, nicknames, details, sessions)
        value = searchMentions(searchQuery, input, typeLabels, dropdownTexts)
    }

    // results of an older query stay hidden until the current one is searched, so a tap never picks a stale match
    return if (query == null || found.query != searchQuery || query != searchQuery) emptyList() else found.items
}

private class MentionSearchResult(
    val query: String?,
    val items: List<MentionDropdownItem>,
)

private suspend fun searchMentions(
    query: String?,
    input: MentionDropdownInput,
    typeLabels: MentionTypeLabels,
    texts: MentionDropdownTexts,
): MentionSearchResult =
    MentionSearchResult(
        query,
        query?.let { withContext(Dispatchers.Default) { mentionDropdown(input, it, typeLabels, texts) } }.orEmpty(),
    )

@Composable
private fun mentionDropdownTexts(): MentionDropdownTexts =
    MentionDropdownTexts(
        search = stringResource(Res.string.session_detail_mention_suggestions_header),
        thisSession = stringResource(Res.string.mention_section_this_session),
        previousSession = stringResource(Res.string.mention_section_previous_session),
        recent = stringResource(Res.string.mention_section_recent),
        codex = stringResource(Res.string.mention_section_codex),
    )

internal data class MentionDropdownTexts(
    val search: String,
    val thisSession: String,
    val previousSession: String,
    val recent: String,
    val codex: String,
)

internal class MentionDropdownInput(
    val entities: List<MentionEntity>,
    val index: MentionIndex,
    val nicknames: Map<MentionRef, List<String>>,
    val details: Map<MentionRef, String>,
    val sessions: MentionSessionBodies,
)

/** the rows under an "@" - the sections of an empty one, the hits of a query, and the way to make a new record */
internal fun mentionDropdown(
    input: MentionDropdownInput,
    query: String,
    labels: MentionTypeLabels,
    texts: MentionDropdownTexts,
): List<MentionDropdownItem> {
    fun entry(candidate: MentionCandidate): MentionDropdownItem.Entry {
        val detail = input.details[candidate.entity.ref].orEmpty()
        return MentionDropdownItem.Entry(candidate.entity.toSuggestion(labels, detail, candidate.nickname.orEmpty()))
    }
    return when {
        input.entities.isEmpty() ->
            listOf(MentionDropdownItem.EmptyCodex) +
                listOfNotNull(query.trim().takeIf { it.isNotEmpty() }?.let(MentionDropdownItem::Create))
        query.isBlank() -> emptyRows(input, texts, ::entry) + createRecordRow(input)
        else -> {
            val search = searchMentions(input.entities, input.nicknames, query)
            val header =
                listOf(
                    MentionDropdownItem.Header(texts.search),
                ).takeIf { search.candidates.isNotEmpty() }.orEmpty()
            val create = search.createName?.let(MentionDropdownItem::Create) ?: createRecordRow(input)
            header + search.candidates.map(::entry) + create
        }
    }
}

private fun createRecordRow(input: MentionDropdownInput) =
    MentionDropdownItem.CreateRecord(createRowShowsHint(input.entities.size))

private fun emptyRows(
    input: MentionDropdownInput,
    texts: MentionDropdownTexts,
    entry: (MentionCandidate) -> MentionDropdownItem.Entry,
): List<MentionDropdownItem> {
    // a record that was deleted still has tokens in the notes, only live ones are offered
    // newest note first, so the cap drops the oldest mentions
    fun mentioned(bodies: List<String>) =
        mentionsIn(bodies.asReversed(), input.index).filter { input.index.has(it.ref) }
    val rows =
        emptyMentionRows(
            thisSession = mentioned(input.sessions.current),
            previousSession = mentioned(input.sessions.previous),
            recentlyCreated = newestRecords(input.entities, RECENT_LIMIT),
            all = input.entities,
        )
    return rows
        .groupBy { it.section }
        .flatMap { (section, group) ->
            val title =
                when (section) {
                    MentionSection.THIS_SESSION -> texts.thisSession
                    MentionSection.PREVIOUS_SESSION -> texts.previousSession
                    MentionSection.RECENT -> texts.recent
                    MentionSection.CODEX -> texts.codex
                }
            listOf(MentionDropdownItem.Header(title)) + group.map { entry(it.candidate) }
        }
}

/** the name of each record kind the autocomplete shows next to a suggestion */
internal data class MentionTypeLabels(
    val npc: String,
    val party: String,
    val location: String,
    val quest: String,
    val species: String,
)

internal fun MentionEntity.toSuggestion(
    labels: MentionTypeLabels,
    detail: String = "",
    nickname: String = "",
): MentionSuggestion {
    val (glyph, tint, typeLabel) =
        when (this) {
            is MentionEntity.NpcMention ->
                Triple(
                    MentionGlyph.Entity(EntityKind.NPC),
                    status.toStatusColor().foreground,
                    labels.npc,
                )
            is MentionEntity.LocationMention ->
                Triple(MentionGlyph.Entity(EntityKind.LOCATION), LocationMentionColor.foreground, labels.location)
            is MentionEntity.QuestMention ->
                Triple(MentionGlyph.Entity(EntityKind.QUEST), status.toStatusColor().foreground, labels.quest)
            is MentionEntity.PartyMention ->
                Triple(
                    MentionGlyph.Entity(EntityKind.PARTY),
                    PartyMemberColor.foreground,
                    labels.party,
                )
            is MentionEntity.SpeciesMention ->
                Triple(MentionGlyph.Entity(EntityKind.SPECIES), SpeciesMentionColor.foreground, labels.species)
            // a deleted record is never offered by the autocomplete, its candidates come from live records only
            is MentionEntity.DeletedMention -> Triple(MentionGlyph.Deleted, LocationMentionColor.foreground, "")
        }
    return MentionSuggestion(
        token = token(),
        glyph = glyph,
        tint = tint,
        name = name,
        typeLabel = typeLabel,
        detail = detail,
        nickname = nickname,
    )
}
