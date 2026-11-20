package com.lyrismet.incadent.feature.combat.presentation.round

import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.repository.NpcRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.entityWriteLock
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.repository.CombatRepository
import kotlinx.coroutines.sync.withLock

/** the shields and facts a participant has of its own: on the card of its record, or else kept on the participant */
internal class OwnFeatures(
    private val npcRepository: NpcRepository,
    private val combatRepository: CombatRepository,
) {
    suspend fun shields(member: Combatant): Map<DamageType, DamageState> =
        member.npcId?.let { npcRepository.getVulnerabilities(it) } ?: member.vulnerabilities

    suspend fun writeShields(
        member: Combatant,
        shields: Map<DamageType, DamageState>,
    ) {
        val written =
            member.npcId?.let { id -> entityWriteLock.withLock { npcRepository.setVulnerabilities(id, shields) } }
        if (written != true) combatRepository.setOwnShields(member.id, shields)
    }

    suspend fun facts(member: Combatant): List<String> =
        member.npcId?.let { npcRepository.getById(it) }?.facts ?: member.facts

    suspend fun writeFacts(
        member: Combatant,
        facts: List<String>,
    ) {
        val written =
            member.npcId?.let { id ->
                entityWriteLock.withLock {
                    npcRepository.getById(id)?.also { npcRepository.upsert(it.copy(facts = facts)) }
                }
            }
        if (written == null) combatRepository.setOwnFacts(member.id, facts)
    }
}
