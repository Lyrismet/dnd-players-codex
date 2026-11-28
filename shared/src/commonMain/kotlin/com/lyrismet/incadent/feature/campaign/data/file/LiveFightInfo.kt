package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.LiveFight
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.round.gameTime
import com.lyrismet.incadent.db.Combat as CombatRow

/** the one fight of a campaign that is not over, the one under way before one still being prepared */
internal fun List<CombatRow>.liveRow(): CombatRow? =
    firstOrNull { it.phase == CombatPhase.ONGOING } ?: firstOrNull { it.phase == CombatPhase.PREPARING }

/** the fight of [campaignId] that is not over, as the export card tells it */
internal fun readLiveFight(
    database: AppDatabase,
    campaignId: String,
): LiveFight? {
    val row =
        database.combatQueries
            .selectAll(campaignId)
            .executeAsList()
            .liveRow() ?: return null
    return LiveFight(
        ongoing = row.phase == CombatPhase.ONGOING,
        round = row.round.toInt(),
        minutes = gameTime(row.round.toInt()).minutes.takeIf { it > 0 },
        fighters =
            database.combatantQueries
                .selectByCombat(row.id)
                .executeAsList()
                .size,
    )
}

/** a fight under way blocks any other, a fight still being prepared only another preparation */
internal fun liveFightBlocked(
    database: AppDatabase,
    campaignId: String,
    incoming: CombatPhase,
): Boolean {
    val phases =
        database.combatQueries
            .selectAll(campaignId)
            .executeAsList()
            .map { it.phase }
    return CombatPhase.ONGOING in phases || (incoming == CombatPhase.PREPARING && CombatPhase.PREPARING in phases)
}
