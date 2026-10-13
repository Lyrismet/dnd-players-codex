package com.lyrismet.incadent.core.entitysummary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository

/** the ref of whatever the card shows */
val EntitySummaryItem.entityRef: EntityRef
    get() =
        when (this) {
            is EntitySummaryItem.NpcSummary -> ref
            is EntitySummaryItem.LocationSummary -> ref
            is EntitySummaryItem.QuestSummary -> ref
            is EntitySummaryItem.PartySummary -> ref
        }

/** the photo the card draws - only a thumbnail from the list models, until [withFullPhoto] swaps the full one in */
val EntitySummaryItem.photo: Photo?
    get() =
        when (this) {
            is EntitySummaryItem.NpcSummary -> emblem.portrait
            is EntitySummaryItem.LocationSummary -> cover
            is EntitySummaryItem.PartySummary -> emblem.portrait
            is EntitySummaryItem.QuestSummary -> null
        }

/** puts the loaded [full] photo into the card, only when it is the one this card's thumbnail belongs to */
fun EntitySummaryItem.withFullPhoto(full: Photo?): EntitySummaryItem {
    val current = photo
    if (full == null || current == null || !current.thumb.contentEquals(full.thumb)) return this
    return when (this) {
        is EntitySummaryItem.NpcSummary -> copy(emblem = emblem.copy(portrait = full))
        is EntitySummaryItem.LocationSummary -> copy(cover = full)
        is EntitySummaryItem.PartySummary -> copy(emblem = emblem.copy(portrait = full))
        is EntitySummaryItem.QuestSummary -> this
    }
}

/**
 * the card's full-size photo, read from the database while the card is open - the lists only carry thumbnails.
 * Null until it is loaded or when the card has no photo; it reloads when the thumbnail changes (a new pick).
 */
@Composable
fun rememberFullPhoto(
    item: EntitySummaryItem?,
    npcRepository: NpcRepository,
    partyRepository: PartyRepository,
    locationRepository: LocationRepository,
): Photo? {
    val ref = item?.entityRef
    val listPhoto = item?.photo
    val loaded by produceState<Photo?>(initialValue = null, ref, listPhoto) {
        value =
            if (listPhoto == null) {
                null
            } else {
                when (ref) {
                    is EntityRef.Npc -> npcRepository.getById(ref.id)?.portrait
                    is EntityRef.Party -> partyRepository.getById(ref.id)?.portrait
                    is EntityRef.Location -> locationRepository.getById(ref.id)?.cover
                    is EntityRef.Quest, null -> null
                }
            }
    }
    return loaded
}
