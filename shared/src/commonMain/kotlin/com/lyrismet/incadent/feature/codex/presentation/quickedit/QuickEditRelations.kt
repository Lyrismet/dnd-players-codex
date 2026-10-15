package com.lyrismet.incadent.feature.codex.presentation.quickedit

import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.Quest
// ticking a candidate of a relation group rewrites the candidate, not the card's own entity

/** adds [npcId] as the giver, or clears it when it already is - a quest given by someone else is taken over */
fun Quest.withGiverToggled(npcId: Long): Quest = copy(givenByNpcId = if (givenByNpcId == npcId) null else npcId)

/** sets [placeId] as the quest's place, or clears it when it already is */
fun Quest.withPlaceToggled(placeId: Long): Quest = copy(locationId = if (locationId == placeId) null else placeId)

/** adds or drops [placeId] among the places the npc is met at */
fun Npc.withPlaceToggled(placeId: Long): Npc =
    copy(locationIds = if (placeId in locationIds) locationIds - placeId else locationIds + placeId)
