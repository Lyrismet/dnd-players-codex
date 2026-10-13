package com.lyrismet.incadent.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Photo
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
    private val mentionRenamer: MentionRenamer,
) : PartyRepository {
    private val queries = database.partyMemberQueries

    override fun observeAll(): Flow<List<PartyMember>> =
        queries.selectAll(::toDomain).asFlow().mapToList(Dispatchers.Default)

    override suspend fun getById(id: Long): PartyMember? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomainFull).executeAsOneOrNull()
        }

    // a renamed member takes the "@mentions" of its old name along, in the same transaction as the update
    override suspend fun upsert(member: PartyMember): Long =
        withContext(Dispatchers.Default) {
            val oldName =
                if (member.id == 0L) null else queries.selectById(member.id, ::toDomainFull).executeAsOneOrNull()?.name
            if (oldName != null && oldName != member.name) {
                mentionRenamer.partyRenamed(member.id, oldName, member.name) { write(member) }
                member.id
            } else {
                write(member)
            }
        }

    // the "вы" flag is unique per campaign - saving a player character clears it from everyone else
    private fun write(member: PartyMember): Long =
        database.transactionWithResult {
            val id =
                if (member.id == 0L) {
                    queries.insert(
                        name = member.name,
                        character_class = member.characterClass,
                        race = member.race,
                        level = member.level?.toLong(),
                        player_name = member.playerName,
                        is_player_character = if (member.isPlayerCharacter) 1L else 0L,
                        presence = member.presence,
                        hp_max = member.hpMax?.toLong(),
                        hp_current = member.hpCurrent?.toLong(),
                        description = member.description,
                        portrait = member.portrait?.full,
                        portrait_thumb = member.portrait?.thumb,
                    )
                    queries.lastInsertRowId().executeAsOne()
                } else {
                    val updatedRows =
                        queries
                            .update(
                                name = member.name,
                                character_class = member.characterClass,
                                race = member.race,
                                level = member.level?.toLong(),
                                player_name = member.playerName,
                                is_player_character = if (member.isPlayerCharacter) 1L else 0L,
                                presence = member.presence,
                                hp_max = member.hpMax?.toLong(),
                                hp_current = member.hpCurrent?.toLong(),
                                description = member.description,
                                id = member.id,
                            ).value
                    check(updatedRows > 0L) { "party member ${member.id} no longer exists" }
                    writePortrait(member.id, member.portrait)
                    member.id
                }
            if (member.isPlayerCharacter) queries.clearOtherPlayerCharacters(id)
            id
        }

    // a photo without its full jpeg came from a list-level model and means "unchanged", null removes it
    private fun writePortrait(
        memberId: Long,
        portrait: Photo?,
    ) {
        when {
            portrait == null -> queries.setPortrait(null, null, memberId)
            portrait.full != null -> queries.setPortrait(portrait.full, portrait.thumb, memberId)
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
        level: Long?,
        playerName: String,
        isPlayerCharacter: Long,
        presence: PartyPresence,
        hpMax: Long?,
        hpCurrent: Long?,
        description: String,
        portraitThumb: ByteArray?,
    ) = PartyMember(
        id = id,
        name = name,
        characterClass = characterClass,
        race = race,
        level = level?.toInt(),
        playerName = playerName,
        isPlayerCharacter = isPlayerCharacter != 0L,
        presence = presence,
        hpMax = hpMax?.toInt(),
        hpCurrent = hpCurrent?.toInt(),
        description = description,
        portrait = portraitThumb?.let { Photo(it) },
    )

    // the by-id row also carries the full jpeg
    @Suppress("LongParameterList")
    private fun toDomainFull(
        id: Long,
        name: String,
        characterClass: String,
        race: String,
        level: Long?,
        playerName: String,
        isPlayerCharacter: Long,
        presence: PartyPresence,
        hpMax: Long?,
        hpCurrent: Long?,
        description: String,
        portraitThumb: ByteArray?,
        portrait: ByteArray?,
    ) = toDomain(
        id,
        name,
        characterClass,
        race,
        level,
        playerName,
        isPlayerCharacter,
        presence,
        hpMax,
        hpCurrent,
        description,
        portraitThumb,
    ).copy(portrait = portraitThumb?.let { Photo(it, portrait) })
}
