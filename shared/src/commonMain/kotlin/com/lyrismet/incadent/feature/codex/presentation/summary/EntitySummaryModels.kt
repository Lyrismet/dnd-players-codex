package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.PortraitMark
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.model.SpeciesType
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditField
import com.lyrismet.incadent.feature.codex.presentation.style.DEFAULT_STATUS_GLYPH
import com.lyrismet.incadent.feature.combat.domain.results.CombatFacts
import com.lyrismet.incadent.feature.combat.domain.results.HitRange
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionSegment
import com.lyrismet.incadent.feature.mention.presentation.MentionChipItem
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import org.jetbrains.compose.resources.StringResource

/** the one codex entity a chip, a mention or a codex list row can point at - never more than one kind at a time */
sealed interface EntityRef {
    val id: Long

    data class Npc(
        override val id: Long,
    ) : EntityRef

    data class Location(
        override val id: Long,
    ) : EntityRef

    data class Quest(
        override val id: Long,
    ) : EntityRef

    data class Party(
        override val id: Long,
    ) : EntityRef

    data class Species(
        override val id: Long,
    ) : EntityRef
}

enum class EntityEmblemShape { CIRCLE, ROUNDED }

/** self-contained visual identity for the sheet header, [mark] shows while there is no photo */
data class EntityEmblem(
    val mark: PortraitMark,
    val shape: EntityEmblemShape,
    val color: StatusColor,
    // only an npc or a party member can carry a portrait - see Players Codex v6.dc.html's `por` flag
    val portrait: Photo? = null,
    val isMonochrome: Boolean = false,
    // the sheet header draws the big arch instead of the round badge - npc and party only
    val hasPortraitSlot: Boolean = false,
)

/** one row's value in the facts grid - plain text or a chip that reopens the sheet on another entity */
sealed interface FactValue {
    data class Text(
        val text: String,
    ) : FactValue

    data class Link(
        val chip: MentionChipItem,
    ) : FactValue

    /** several chips in one row - an npc met at more than one place */
    data class Links(
        val chips: List<MentionChipItem>,
    ) : FactValue
}

data class FactRow(
    val label: StringResource,
    val value: FactValue,
    // null keeps the fact read-only even in quick-edit mode
    val edit: QuickEditField? = null,
    // the entities a link fact can be re-pointed at - only read when [edit] is a link field
    val linkOptions: List<MentionChipItem> = emptyList(),
    // a small up arrow at the end of the line, null shows none
    val onRaise: (() -> Unit)? = null,
)

/** one candidate of an editable relation group, ticked when it is linked to the card's entity right now */
data class RelationOption(
    val chip: MentionChipItem,
    val isSelected: Boolean,
)

/**
 * "выдал квесты" / "кто здесь" / "квесты" - a titled row of chips, each reopening the sheet in place. A group with an
 * [edit] field can be held and re-linked from [options], and it stays on the card while empty so it can be filled.
 */
data class RelationGroup(
    val title: StringResource,
    val items: List<MentionChipItem>,
    val edit: QuickEditField? = null,
    val options: List<RelationOption> = emptyList(),
    val emptyText: StringResource? = null,
)

data class RelatedNoteItem(
    val sessionNoteId: Long,
    // the first matching entry, so opening the note can scroll to and highlight it
    val entryId: Long,
    val numberLabel: String,
    val title: String,
    val snippet: String,
    val matchCount: Int,
)

data class StatusOption<T>(
    val value: T,
    val label: String,
    val color: StatusColor,
    val isSelected: Boolean,
    val glyph: String = DEFAULT_STATUS_GLYPH,
)

/** pre-formatted for direct rendering - shown inside an [AppBottomSheet], one shape per codex entity kind */
sealed interface EntitySummaryItem {
    data class NpcSummary(
        val ref: EntityRef.Npc,
        val name: String,
        val statusLabel: String,
        val isDead: Boolean,
        val description: String,
        val descriptionSegments: List<MentionSegment> = emptyList(),
        val emblem: EntityEmblem,
        val subtitle: String,
        val statusOptions: List<StatusOption<NpcStatus>>,
        val lifeOptions: List<StatusOption<NpcLifeState>>,
        val facts: List<FactRow>,
        val groups: List<RelationGroup>,
        val relatedNotes: List<RelatedNoteItem>,
        val vulnerabilities: NpcVulnerabilities = NpcVulnerabilities(),
    ) : EntitySummaryItem

    data class LocationSummary(
        val ref: EntityRef.Location,
        val name: String,
        val description: String,
        val descriptionSegments: List<MentionSegment> = emptyList(),
        val cover: Photo?,
        val emblem: EntityEmblem,
        val subtitle: String,
        val facts: List<FactRow>,
        val groups: List<RelationGroup>,
        val relatedNotes: List<RelatedNoteItem>,
        // false when no other place exists to move into
        val canMove: Boolean = false,
    ) : EntitySummaryItem

    data class QuestSummary(
        val ref: EntityRef.Quest,
        val title: String,
        val statusLabel: String,
        val reward: String,
        val description: String,
        val descriptionSegments: List<MentionSegment> = emptyList(),
        val emblem: EntityEmblem,
        val subtitle: String,
        val statusOptions: List<StatusOption<QuestStatus>>,
        val facts: List<FactRow>,
        val groups: List<RelationGroup>,
        val relatedNotes: List<RelatedNoteItem>,
    ) : EntitySummaryItem

    /** a kind of creature - the damage it takes and the sessions it turned up in, no status to pick */
    data class SpeciesSummary(
        val ref: EntityRef.Species,
        val name: String,
        val type: SpeciesType,
        val notes: String,
        val descriptionSegments: List<MentionSegment> = emptyList(),
        val emblem: EntityEmblem,
        val vulnerabilities: Map<DamageType, DamageState>,
        val relatedNotes: List<RelatedNoteItem>,
        // the sessions with a note that mentions it
        val seenInSessions: Int,
        // how many the tapped chip stands for, set only on a card opened from a note
        val noteQuantity: Int? = null,
        val hitPoints: Int? = null,
        // the saved fights it was in and what they hint about its hit points
        val fights: Int = 0,
        val hitRange: HitRange? = null,
    ) : EntitySummaryItem

    data class PartySummary(
        val ref: EntityRef.Party,
        val name: String,
        val overlineValue: String,
        val subtitle: String,
        val description: String,
        val descriptionSegments: List<MentionSegment> = emptyList(),
        val emblem: EntityEmblem,
        val presenceOptions: List<StatusOption<PartyPresence>>,
        val facts: List<FactRow>,
        val relatedNotes: List<RelatedNoteItem>,
        // set for a character who fell, the card then shows the epitaph, the chronicle and the way back
        val fall: PartyFall? = null,
        // the player's own level can still go up
        val canRaiseLevel: Boolean = false,
    ) : EntitySummaryItem
}

/** how a character fell and what they went through, [sessionLabel] and [foes] are empty when the fight is not known */
data class PartyFall(
    val sessionNoteId: Long?,
    val sessionLabel: String?,
    val foes: String,
    val sessions: Int,
    val fights: Int,
    val damage: Int,
)

/** everything [buildEntitySummary] needs to resolve any [EntityRef] - one instance covers a whole screen */
data class EntityLookup(
    val npcs: List<Npc>,
    val locations: List<Location>,
    val quests: List<Quest>,
    val npcStatusLabels: Map<NpcStatus, String>,
    val npcLifeLabels: Map<NpcLifeState, String>,
    val questStatusLabels: Map<QuestStatus, String>,
    val parties: List<PartyMember>,
    val partyPresenceLabels: Map<PartyPresence, String>,
    val partyYouLabel: String,
    val sessionNotes: List<SessionNote>,
    val sessionEntries: List<SessionEntry>,
    val mentionIndex: MentionIndex,
    val sessionNumbering: SessionNumbering,
    val species: List<Species> = emptyList(),
    val facts: CombatFacts = CombatFacts.EMPTY,
)

/** what an npc has of its own and what its species gives it, the card shows both and tells them apart */
data class NpcVulnerabilities(
    val own: Map<DamageType, DamageState> = emptyMap(),
    val fromSpecies: Map<DamageType, DamageState> = emptyMap(),
    val speciesName: String? = null,
)
