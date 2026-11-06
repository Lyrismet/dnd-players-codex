package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.domain.model.Skill
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

const val FILE_FORMAT = "incadent"

// 2 added the saved fights, 3 the origin ids, 4 the export mode, a file of an older version reads as a friend copy
// 5 added the items, 6 the character sheet, the counters and the vitals of a personal copy
// 7 added the fight that is under way
const val FILE_VERSION = 7

/** the first line of a file, what the preview shows is read from here before anything else */
@Serializable
data class FileHeader(
    @SerialName("format") val mark: String,
    val version: Int,
    val name: String = "",
    val withPhotos: Boolean = false,
    val records: Int = 0,
    val sessions: Int = 0,
    val originId: String? = null,
    val mode: ExportMode = ExportMode.FRIEND,
    // a friend copy leaves the notes about the party out unless asked, a file without the flag carries them
    val partyNotes: Boolean = true,
)

/**
 * one line of a file after the header - ids are the file's own numbers, one range per kind, never database ids,
 * and a record only points at ones written above it
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
sealed interface FileRecord {
    @Serializable
    @SerialName("species")
    data class Species(
        val id: Long,
        val name: String,
        val creatureType: CreatureType,
        @JsonNames("damage")
        val vulnerabilities: String = "",
        val notes: String = "",
        val hitPoints: Int? = null,
        val originId: String? = null,
        // the player's own type by name, a file made before them has none
        val customType: String? = null,
    ) : FileRecord

    @Serializable
    @SerialName("place")
    data class Place(
        val id: Long,
        val name: String,
        val description: String = "",
        val parentId: Long? = null,
        val originId: String? = null,
    ) : FileRecord

    @Serializable
    @SerialName("npc")
    data class Npc(
        val id: Long,
        val name: String,
        val status: NpcStatus,
        val description: String = "",
        val race: String = "",
        val lifeState: NpcLifeState = NpcLifeState.ALIVE,
        val nicknames: String = "",
        val speciesId: Long? = null,
        val vulnerabilities: String = "",
        val placeIds: List<Long> = emptyList(),
        val originId: String? = null,
        val characterClass: String = "",
        val subclass: String = "",
    ) : FileRecord

    @Serializable
    @SerialName("quest")
    data class Quest(
        val id: Long,
        val title: String,
        val status: QuestStatus,
        val reward: String = "",
        val giverNpcId: Long? = null,
        val placeId: Long? = null,
        val description: String = "",
        val originId: String? = null,
    ) : FileRecord

    @Serializable
    @SerialName("party")
    data class Party(
        val id: Long,
        val name: String,
        val characterClass: String = "",
        val race: String = "",
        val level: Int? = null,
        val playerName: String = "",
        val isPlayerCharacter: Boolean = false,
        val presence: PartyPresence = PartyPresence.IN,
        val hpMax: Int? = null,
        val hpCurrent: Int? = null,
        val description: String = "",
        val fallen: Boolean = false,
        val originId: String? = null,
        val subclass: String = "",
        // the numbers beside the hit points, a personal copy only
        val tempHp: Int = 0,
        val armorClass: Int? = null,
        val armorClassFrom: String = "",
        val deathSuccesses: Int = 0,
        val deathFailures: Int = 0,
    ) : FileRecord

    /** the sheet of a party member of a personal copy, the wallet is part of it */
    @Serializable
    @SerialName("sheet")
    data class Sheet(
        val partyId: Long,
        val abilities: Map<Ability, Int> = emptyMap(),
        val saves: Set<Ability> = emptySet(),
        val skillMarks: Map<Skill, Int> = emptyMap(),
        val skillAdjustments: Map<Skill, Int> = emptyMap(),
        val initiativeAdjustment: Int = 0,
        val passivePerceptionAdjustment: Int = 0,
        val spellAbility: Ability? = null,
        val speed: Int? = null,
        val inspiration: Boolean = false,
        val spellsHidden: Boolean = false,
        val background: String = "",
        val alignment: String = "",
        val personality: String = "",
        val languages: String = "",
        val features: String = "",
        val notes: String = "",
        val platinum: Int = 0,
        val gold: Int = 0,
        val electrum: Int = 0,
        val silver: Int = 0,
        val copper: Int = 0,
        val originId: String? = null,
    ) : FileRecord

    /** a counter or a spell slot level of a party member of a personal copy */
    @Serializable
    @SerialName("resource")
    data class Resource(
        val partyId: Long,
        val name: String,
        val max: Int = 1,
        val current: Int = 0,
        val slotLevel: Int? = null,
        val position: Int = 0,
        val originId: String? = null,
    ) : FileRecord

    /** a thing somebody carries, [ownerKind] and [ownerId] are the file's own id of the party member, npc or place */
    @Serializable
    @SerialName("item")
    data class Item(
        val id: Long,
        val name: String,
        val quantity: Int = 1,
        val price: Int? = null,
        val coin: Coin = Coin.GOLD,
        val ownerKind: ExchangeKind? = null,
        val ownerId: Long? = null,
        val notes: String = "",
        val originId: String? = null,
    ) : FileRecord

    @Serializable
    @SerialName("tag")
    data class Tag(
        val name: String,
    ) : FileRecord

    @Serializable
    @SerialName("phrase")
    data class Phrase(
        val text: String,
    ) : FileRecord

    @Serializable
    @SerialName("session")
    data class Session(
        val id: Long,
        val title: String,
        val date: LocalDateTime,
        val endedAt: LocalDateTime? = null,
        val createdAt: Long = 0,
        val tags: List<String> = emptyList(),
        val originId: String? = null,
    ) : FileRecord

    @Serializable
    @SerialName("entry")
    data class Entry(
        val sessionId: Long,
        val createdAt: LocalDateTime,
        val body: String,
        val edited: Boolean = false,
        val mentionCheck: Boolean = false,
        val originId: String? = null,
    ) : FileRecord

    /**
     * a fight saved to [sessionId], its fighters, blows and species observations follow below the notes.
     * A fight that is not over has no outcome to speak of and may have no session yet.
     */
    @Serializable
    @SerialName("combat")
    data class Combat(
        val id: Long,
        val sessionId: Long?,
        val outcome: CombatOutcome,
        val phase: CombatPhase = CombatPhase.FINISHED,
        val startedAt: LocalDateTime,
        val finishedAt: LocalDateTime? = null,
        val rounds: Int = 1,
        val turn: Int = 0,
        val originId: String? = null,
    ) : FightRecord

    /** a participant of one fight, [id] is its number in that fight, the links are the file's ids or none */
    @Serializable
    @SerialName("fighter")
    data class Fighter(
        val combatId: Long,
        val id: Long,
        val name: String,
        val side: CombatSide,
        val npcId: Long? = null,
        val speciesId: Long? = null,
        val partyId: Long? = null,
        val enteredWhole: Boolean = true,
        val state: CombatState = CombatState.WHOLE,
        val joinedRound: Int = 1,
        val deathSuccesses: Int = 0,
        val deathFailures: Int = 0,
        val tempHp: Int = 0,
        val stance: CombatStance = CombatStance.ENEMY,
        val captured: Boolean = false,
        val dead: Boolean = false,
        val conditions: List<CombatCondition> = emptyList(),
        val note: String = "",
        val hpCurrent: Int? = null,
        val hpMax: Int? = null,
        val turnOrder: Int? = null,
        val reactionAvailable: Boolean = true,
    ) : FightRecord

    /** one event of a fight, the source and the target are fighter numbers of that fight */
    @Serializable
    @SerialName("blow")
    data class Blow(
        val combatId: Long,
        val round: Int,
        val kind: CombatEventKind,
        val sourceId: Long? = null,
        val targetId: Long,
        val amount: Int = 0,
        val damageType: DamageType? = null,
        val effect: HitEffect = HitEffect.FULL,
        val state: CombatState? = null,
        val detail: String? = null,
        val turn: Int = 0,
        val hit: Int = 0,
        val raw: Int = 0,
        val scale: HitScale = HitScale.ONE,
    ) : FightRecord

    /** what a fight taught about the hit points of a species */
    @Serializable
    @SerialName("observation")
    data class Observation(
        val combatId: Long,
        val speciesId: Long,
        val damage: Int,
        val died: Boolean,
    ) : FightRecord

    /** the main picture of one record, base64 of the stored jpeg, the thumbnail is made again on import */
    @Serializable
    @SerialName("photo")
    data class Photo(
        val owner: PhotoOwner,
        val ownerId: Long,
        val data: String,
    ) : FileRecord
}

/** the records of a saved fight, they sit below the notes and point at the fight's own numbers */
@Serializable
sealed interface FightRecord : FileRecord

@Serializable
enum class PhotoOwner { SPECIES, PLACE, NPC, PARTY, ITEM }
