package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlinx.coroutines.CoroutineScope

/** everything an entity card can ask its host for - each screen wraps it in one event of its own */
sealed interface EntitySheetEvent {
    data class EntityClicked(
        val ref: EntityRef,
    ) : EntitySheetEvent

    data class NpcStatusSelected(
        val npcId: Long,
        val status: NpcStatus,
    ) : EntitySheetEvent

    data class NpcLifeSelected(
        val npcId: Long,
        val lifeState: NpcLifeState,
    ) : EntitySheetEvent

    data class PartyPresenceSelected(
        val partyId: Long,
        val presence: PartyPresence,
    ) : EntitySheetEvent

    data class QuestStatusSelected(
        val questId: Long,
        val status: QuestStatus,
    ) : EntitySheetEvent

    data class SpeciesDamageChanged(
        val speciesId: Long,
        val type: DamageType,
        val state: DamageState,
    ) : EntitySheetEvent

    /** the card's "в этой заметке" row stepped the count of the tapped chip - only a session screen can act on it */
    data class SpeciesNoteQuantityStepped(
        val delta: Int,
    ) : EntitySheetEvent

    data class RelatedNoteClicked(
        val sessionNoteId: Long,
        val entryId: Long,
    ) : EntitySheetEvent

    data object Dismissed : EntitySheetEvent
}

/** true for the events that write a record - an archived campaign drops these */
fun EntitySheetEvent.writesCampaignData(): Boolean =
    when (this) {
        is EntitySheetEvent.NpcStatusSelected,
        is EntitySheetEvent.NpcLifeSelected,
        is EntitySheetEvent.PartyPresenceSelected,
        is EntitySheetEvent.QuestStatusSelected,
        is EntitySheetEvent.SpeciesDamageChanged,
        is EntitySheetEvent.SpeciesNoteQuantityStepped,
        -> true
        is EntitySheetEvent.EntityClicked,
        is EntitySheetEvent.RelatedNoteClicked,
        EntitySheetEvent.Dismissed,
        -> false
    }

/** runs one card event against the open sheet and its records */
fun EntitySheetInteractions.onEvent(
    scope: CoroutineScope,
    event: EntitySheetEvent,
) {
    when (event) {
        is EntitySheetEvent.EntityClicked -> onEntityClicked(event.ref)
        is EntitySheetEvent.NpcStatusSelected -> onNpcStatusSelected(scope, event.npcId, event.status)
        is EntitySheetEvent.NpcLifeSelected -> onNpcLifeSelected(scope, event.npcId, event.lifeState)
        is EntitySheetEvent.PartyPresenceSelected -> onPartyPresenceSelected(scope, event.partyId, event.presence)
        is EntitySheetEvent.QuestStatusSelected -> onQuestStatusSelected(scope, event.questId, event.status)
        // the session screen takes this one before it gets here, a card opened elsewhere has no note to count in
        is EntitySheetEvent.SpeciesNoteQuantityStepped -> Unit
        is EntitySheetEvent.SpeciesDamageChanged ->
            onSpeciesDamageChanged(
                scope,
                event.speciesId,
                event.type,
                event.state,
            )
        is EntitySheetEvent.RelatedNoteClicked -> onRelatedNoteClicked(event.sessionNoteId, event.entryId)
        EntitySheetEvent.Dismissed -> onDismissed()
    }
}

/**
 * the card's actions, every one sent to [send] as an [EntitySheetEvent]. Presence is offered only when
 * [partyPresence] is set, the rest are what only the codex manages.
 */
@Suppress("LongParameterList")
fun entitySheetActions(
    partyPresence: Boolean = false,
    speciesDamageEdit: Boolean = false,
    speciesNoteEdit: Boolean = true,
    onEditClicked: ((EntityRef) -> Unit)? = null,
    onDeleteClicked: ((EntityRef) -> Unit)? = null,
    statusesReadOnly: Boolean = false,
    quickEdit: QuickEditSheet? = null,
    send: (EntitySheetEvent) -> Unit,
): EntitySummarySheetActions =
    EntitySummarySheetActions(
        onEntityRefClicked = { send(EntitySheetEvent.EntityClicked(it)) },
        onNpcStatusSelected = { id, status -> send(EntitySheetEvent.NpcStatusSelected(id, status)) },
        onNpcLifeSelected = { id, life -> send(EntitySheetEvent.NpcLifeSelected(id, life)) },
        onQuestStatusSelected = { id, status -> send(EntitySheetEvent.QuestStatusSelected(id, status)) },
        onRelatedNoteClicked = { id, entryId -> send(EntitySheetEvent.RelatedNoteClicked(id, entryId)) },
        onEditClicked = onEditClicked,
        onDeleteClicked = onDeleteClicked,
        onPartyPresenceSelected =
            if (partyPresence) {
                { id, presence -> send(EntitySheetEvent.PartyPresenceSelected(id, presence)) }
            } else {
                null
            },
        onSpeciesDamageChanged =
            if (speciesDamageEdit) {
                { id, type, state -> send(EntitySheetEvent.SpeciesDamageChanged(id, type, state)) }
            } else {
                null
            },
        onSpeciesNoteQuantityStep =
            if (speciesNoteEdit) {
                (
                    {
                        send(
                            EntitySheetEvent.SpeciesNoteQuantityStepped(it),
                        )
                    }
                )
            } else {
                null
            },
        statusesReadOnly = statusesReadOnly,
        quickEdit = quickEdit,
    )
