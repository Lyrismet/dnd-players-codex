package com.lyrismet.incadent.persistence

import app.cash.sqldelight.db.SqlDriver
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.db.Campaign
import com.lyrismet.incadent.db.Character_sheet
import com.lyrismet.incadent.db.Combat
import com.lyrismet.incadent.db.Combat_event
import com.lyrismet.incadent.db.Combatant
import com.lyrismet.incadent.db.Deleted_origin
import com.lyrismet.incadent.db.Item
import com.lyrismet.incadent.db.Kept_apart
import com.lyrismet.incadent.db.Npc
import com.lyrismet.incadent.db.Origin_alias
import com.lyrismet.incadent.db.Party_member
import com.lyrismet.incadent.db.Quest
import com.lyrismet.incadent.db.Session_entry
import com.lyrismet.incadent.db.Session_note
import com.lyrismet.incadent.db.Species

fun createAppDatabase(driver: SqlDriver): AppDatabase {
    // SQLite ignores every ON DELETE CASCADE in the schema unless this is set per connection
    driver.execute(identifier = null, sql = "PRAGMA foreign_keys=ON;", parameters = 0)

    return AppDatabase(
        driver = driver,
        campaignAdapter = Campaign.Adapter(statusAdapter = campaignStatusAdapter),
        combatAdapter =
            Combat.Adapter(
                phaseAdapter = combatPhaseAdapter,
                started_atAdapter = localDateTimeAdapter,
                outcomeAdapter = combatOutcomeAdapter,
                finished_atAdapter = localDateTimeAdapter,
            ),
        combatantAdapter =
            Combatant.Adapter(
                sideAdapter = combatSideAdapter,
                stateAdapter = combatStateAdapter,
                stanceAdapter = combatStanceAdapter,
                conditionsAdapter = combatConditionsAdapter,
            ),
        combat_eventAdapter =
            Combat_event.Adapter(
                kindAdapter = combatEventKindAdapter,
                damage_typeAdapter = damageTypeAdapter,
                effectAdapter = hitEffectAdapter,
                stateAdapter = combatStateAdapter,
                scaleAdapter = hitScaleAdapter,
            ),
        origin_aliasAdapter = Origin_alias.Adapter(kindAdapter = exchangeKindAdapter),
        deleted_originAdapter = Deleted_origin.Adapter(kindAdapter = exchangeKindAdapter),
        kept_apartAdapter = Kept_apart.Adapter(kindAdapter = exchangeKindAdapter),
        npcAdapter = Npc.Adapter(statusAdapter = npcStatusAdapter, life_stateAdapter = npcLifeStateAdapter),
        party_memberAdapter = Party_member.Adapter(presenceAdapter = partyPresenceAdapter),
        questAdapter = Quest.Adapter(statusAdapter = questStatusAdapter),
        character_sheetAdapter =
            Character_sheet.Adapter(
                savesAdapter = abilitySetAdapter,
                skill_marksAdapter = skillValuesAdapter,
                skill_adjustmentsAdapter = skillValuesAdapter,
                skill_notesAdapter = skillNotesAdapter,
                adjustment_notesAdapter = sheetAdjustmentNotesAdapter,
                spell_abilityAdapter = abilityAdapter,
            ),
        itemAdapter = Item.Adapter(coinAdapter = coinAdapter, owner_kindAdapter = entityKindAdapter),
        speciesAdapter = Species.Adapter(creature_typeAdapter = creatureTypeAdapter),
        session_entryAdapter = Session_entry.Adapter(created_atAdapter = localDateTimeAdapter),
        session_noteAdapter =
            Session_note.Adapter(
                session_dateAdapter = localDateTimeAdapter,
                ended_atAdapter = localDateTimeAdapter,
            ),
    )
}
