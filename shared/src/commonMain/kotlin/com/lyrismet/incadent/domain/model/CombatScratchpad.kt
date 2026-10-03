package com.lyrismet.incadent.domain.model

data class CombatScratchpad(
    val id: Long,
    val sessionNoteId: Long?,
    val bossName: String,
    val bossHpCurrent: Int,
    val bossHpMax: Int,
    val notes: String,
    val combatants: List<Combatant>,
)

data class Combatant(
    val id: Long,
    val name: String,
    val initiative: Int,
    val hpCurrent: Int,
    val hpMax: Int,
    val isPlayerCharacter: Boolean,
    val npcId: Long?,
)
