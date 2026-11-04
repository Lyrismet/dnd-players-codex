package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.core.identity.orNewOriginId
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.db.PartyMemberQueries
import com.lyrismet.incadent.db.SelectVitals
import com.lyrismet.incadent.db.characterSheet.SelectByMember
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals
import com.lyrismet.incadent.feature.codex.domain.model.CoinPurse
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency
import com.lyrismet.incadent.feature.codex.domain.model.clampedToRanges
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSheetRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CharacterSheetRepositoryImpl(
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : CharacterSheetRepository {
    private val sheets = database.characterSheetQueries
    private val party = database.partyMemberQueries

    override fun observeSheet(memberId: Long): Flow<CharacterSheet> =
        sheets
            .selectByMember(memberId)
            .asFlow()
            .mapToOneOrNull(DatabaseDispatcher)
            .map { it?.toDomain() ?: CharacterSheet() }

    override suspend fun getSheet(memberId: Long): CharacterSheet =
        withContext(DatabaseDispatcher) {
            sheets.selectByMember(memberId).executeAsOneOrNull()?.toDomain() ?: CharacterSheet()
        }

    override suspend fun saveSheet(
        memberId: Long,
        sheet: CharacterSheet,
    ) {
        withContext(DatabaseDispatcher) {
            database.transaction {
                val value = sheet.clampedToRanges()
                if (sheets.selectByMember(memberId).executeAsOneOrNull() != null) {
                    update(memberId, value)
                } else {
                    insert(memberId, value)
                }
            }
        }
    }

    // the generated statements take every column in order, one call site each for update and insert
    @Suppress("LongParameterList")
    private fun update(
        memberId: Long,
        value: CharacterSheet,
    ) {
        val coins = value.purse
        sheets.update(
            ability_str = value.score(Ability.STRENGTH),
            ability_dex = value.score(Ability.DEXTERITY),
            ability_con = value.score(Ability.CONSTITUTION),
            ability_int = value.score(Ability.INTELLIGENCE),
            ability_wis = value.score(Ability.WISDOM),
            ability_cha = value.score(Ability.CHARISMA),
            saves = value.saves,
            skill_marks = value.skillMarks.mapValues { it.value.multiplier },
            skill_adjustments = value.skillAdjustments,
            initiative_adjustment = value.initiativeAdjustment.toLong(),
            passive_perception_adjustment = value.passivePerceptionAdjustment.toLong(),
            spell_ability = value.spellAbility,
            speed = value.speed?.toLong(),
            inspiration = value.inspiration,
            subclass = value.subclass,
            background = value.background,
            alignment = value.alignment,
            personality = value.personality,
            languages = value.languages,
            features = value.features,
            notes = value.notes,
            coin_platinum = coins.platinum.toLong(),
            coin_gold = coins.gold.toLong(),
            coin_electrum = coins.electrum.toLong(),
            coin_silver = coins.silver.toLong(),
            coin_copper = coins.copper.toLong(),
            party_member_id = memberId,
        )
    }

    @Suppress("LongParameterList")
    private fun insert(
        memberId: Long,
        value: CharacterSheet,
    ) {
        val coins = value.purse
        sheets.insert(
            party_member_id = memberId,
            ability_str = value.score(Ability.STRENGTH),
            ability_dex = value.score(Ability.DEXTERITY),
            ability_con = value.score(Ability.CONSTITUTION),
            ability_int = value.score(Ability.INTELLIGENCE),
            ability_wis = value.score(Ability.WISDOM),
            ability_cha = value.score(Ability.CHARISMA),
            saves = value.saves,
            skill_marks = value.skillMarks.mapValues { it.value.multiplier },
            skill_adjustments = value.skillAdjustments,
            initiative_adjustment = value.initiativeAdjustment.toLong(),
            passive_perception_adjustment = value.passivePerceptionAdjustment.toLong(),
            spell_ability = value.spellAbility,
            speed = value.speed?.toLong(),
            inspiration = value.inspiration,
            subclass = value.subclass,
            background = value.background,
            alignment = value.alignment,
            personality = value.personality,
            languages = value.languages,
            features = value.features,
            notes = value.notes,
            coin_platinum = coins.platinum.toLong(),
            coin_gold = coins.gold.toLong(),
            coin_electrum = coins.electrum.toLong(),
            coin_silver = coins.silver.toLong(),
            coin_copper = coins.copper.toLong(),
            campaign_id = current.requireId(),
            origin_id = value.originId.orNewOriginId(),
        )
    }

    override fun observeVitals(memberId: Long): Flow<CharacterVitals?> =
        party
            .selectVitals(memberId)
            .asFlow()
            .mapToOneOrNull(DatabaseDispatcher)
            .map { it?.toDomain() }

    override suspend fun saveVitals(
        memberId: Long,
        vitals: CharacterVitals,
    ) {
        withContext(DatabaseDispatcher) { writeVitals(party, memberId, vitals) }
    }
}

private fun CharacterSheet.score(ability: Ability): Long? = abilities[ability]?.toLong()

private fun SelectByMember.toDomain(): CharacterSheet =
    CharacterSheet(
        abilities =
            mapOf(
                Ability.STRENGTH to ability_str,
                Ability.DEXTERITY to ability_dex,
                Ability.CONSTITUTION to ability_con,
                Ability.INTELLIGENCE to ability_int,
                Ability.WISDOM to ability_wis,
                Ability.CHARISMA to ability_cha,
            ).mapNotNull { (ability, score) -> score?.let { ability to it.toInt() } }.toMap(),
        saves = saves,
        skillMarks =
            skill_marks
                .mapNotNull { (skill, value) ->
                    Proficiency.entries
                        .firstOrNull {
                            it.multiplier == value && it != Proficiency.NONE
                        }?.let { skill to it }
                }.toMap(),
        skillAdjustments = skill_adjustments,
        initiativeAdjustment = initiative_adjustment.toInt(),
        passivePerceptionAdjustment = passive_perception_adjustment.toInt(),
        spellAbility = spell_ability,
        speed = speed?.toInt(),
        inspiration = inspiration,
        subclass = subclass,
        background = background,
        alignment = alignment,
        personality = personality,
        languages = languages,
        features = features,
        notes = notes,
        purse =
            CoinPurse(
                platinum = coin_platinum.toInt(),
                gold = coin_gold.toInt(),
                electrum = coin_electrum.toInt(),
                silver = coin_silver.toInt(),
                copper = coin_copper.toInt(),
            ),
        originId = origin_id,
    )

internal fun SelectVitals.toDomain(): CharacterVitals =
    CharacterVitals(
        hpMax = hp_max?.toInt(),
        hpCurrent = hp_current?.toInt(),
        tempHp = temp_hp.toInt(),
        armorClass = armor_class?.toInt(),
        armorClassFrom = armor_class_from,
        deathSavesSuccess = death_saves_success.toInt(),
        deathSavesFailure = death_saves_failure.toInt(),
    )

internal fun writeVitals(
    party: PartyMemberQueries,
    memberId: Long,
    vitals: CharacterVitals,
) {
    val value = vitals.clampedToRanges()
    party.setVitals(
        value.hpCurrent?.toLong(),
        value.tempHp.toLong(),
        value.armorClass?.toLong(),
        value.armorClassFrom,
        value.deathSavesSuccess.toLong(),
        value.deathSavesFailure.toLong(),
        memberId,
    )
}
