package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.repository.PartyRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PartyRepositoryImpl(
    private val database: AppDatabase,
) : PartyRepository {
    private val queries = database.partyMemberQueries

    override fun observeAll(): Flow<List<PartyMember>> =
        queries.selectAll(::toDomain).asFlow().mapToList(Dispatchers.Default)

    override suspend fun getById(id: Long): PartyMember? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomain).executeAsOneOrNull()
        }

    // the "вы" flag is unique per campaign - saving a player character clears it from everyone else
    override suspend fun upsert(member: PartyMember): Long =
        withContext(Dispatchers.Default) {
            database.transactionWithResult {
                val id =
                    if (member.id == 0L) {
                        queries.insert(
                            name = member.name,
                            character_class = member.characterClass,
                            race = member.race,
                            level = member.level.toLong(),
                            player_name = member.playerName,
                            is_player_character = if (member.isPlayerCharacter) 1L else 0L,
                            presence = member.presence,
                            hp_max = member.hpMax.toLong(),
                            hp_current = member.hpCurrent.toLong(),
                            armor_class = member.armorClass.toLong(),
                            initiative_bonus = member.initiativeBonus.toLong(),
                            description = member.description,
                            portrait_uri = member.portraitBase64,
                        )
                        queries.lastInsertRowId().executeAsOne()
                    } else {
                        val updatedRows =
                            queries
                                .update(
                                    name = member.name,
                                    character_class = member.characterClass,
                                    race = member.race,
                                    level = member.level.toLong(),
                                    player_name = member.playerName,
                                    is_player_character = if (member.isPlayerCharacter) 1L else 0L,
                                    presence = member.presence,
                                    hp_max = member.hpMax.toLong(),
                                    hp_current = member.hpCurrent.toLong(),
                                    armor_class = member.armorClass.toLong(),
                                    initiative_bonus = member.initiativeBonus.toLong(),
                                    description = member.description,
                                    portrait_uri = member.portraitBase64,
                                    id = member.id,
                                ).value
                        check(updatedRows > 0L) { "party member ${member.id} no longer exists" }
                        member.id
                    }
                if (member.isPlayerCharacter) queries.clearOtherPlayerCharacters(id)
                id
            }
        }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    // one param per party_member column - dictated by SQLDelight's generated query mapper shape, not real complexity
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        name: String,
        characterClass: String,
        race: String,
        level: Long,
        playerName: String,
        isPlayerCharacter: Long,
        presence: PartyPresence,
        hpMax: Long,
        hpCurrent: Long,
        armorClass: Long,
        initiativeBonus: Long,
        description: String,
        portraitBase64: String?,
    ) = PartyMember(
        id = id,
        name = name,
        characterClass = characterClass,
        race = race,
        level = level.toInt(),
        playerName = playerName,
        isPlayerCharacter = isPlayerCharacter != 0L,
        presence = presence,
        hpMax = hpMax.toInt(),
        hpCurrent = hpCurrent.toInt(),
        armorClass = armorClass.toInt(),
        initiativeBonus = initiativeBonus.toInt(),
        description = description,
        portraitBase64 = portraitBase64,
    )
}
