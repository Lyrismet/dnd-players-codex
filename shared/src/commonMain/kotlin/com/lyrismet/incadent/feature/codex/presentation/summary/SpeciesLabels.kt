package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.model.SpeciesType
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.creature_type_aberration
import incadent.shared.generated.resources.creature_type_beast
import incadent.shared.generated.resources.creature_type_celestial
import incadent.shared.generated.resources.creature_type_construct
import incadent.shared.generated.resources.creature_type_dragon
import incadent.shared.generated.resources.creature_type_elemental
import incadent.shared.generated.resources.creature_type_fey
import incadent.shared.generated.resources.creature_type_fiend
import incadent.shared.generated.resources.creature_type_giant
import incadent.shared.generated.resources.creature_type_humanoid
import incadent.shared.generated.resources.creature_type_monstrosity
import incadent.shared.generated.resources.creature_type_ooze
import incadent.shared.generated.resources.creature_type_plant
import incadent.shared.generated.resources.creature_type_undead
import incadent.shared.generated.resources.creature_type_unspecified
import incadent.shared.generated.resources.damage_state_immunity
import incadent.shared.generated.resources.damage_state_normal
import incadent.shared.generated.resources.damage_state_resistance
import incadent.shared.generated.resources.damage_state_vulnerability
import incadent.shared.generated.resources.damage_type_acid
import incadent.shared.generated.resources.damage_type_bludgeoning
import incadent.shared.generated.resources.damage_type_cold
import incadent.shared.generated.resources.damage_type_fire
import incadent.shared.generated.resources.damage_type_force
import incadent.shared.generated.resources.damage_type_lightning
import incadent.shared.generated.resources.damage_type_necrotic
import incadent.shared.generated.resources.damage_type_piercing
import incadent.shared.generated.resources.damage_type_poison
import incadent.shared.generated.resources.damage_type_psychic
import incadent.shared.generated.resources.damage_type_radiant
import incadent.shared.generated.resources.damage_type_slashing
import incadent.shared.generated.resources.damage_type_thunder
import incadent.shared.generated.resources.species_type_unset
import org.jetbrains.compose.resources.stringResource

/** resource-backed names of the species enums, one place for the list row, the card and the damage grid */
@Composable
fun creatureTypeLabels(): Map<CreatureType, String> =
    mapOf(
        CreatureType.ABERRATION to stringResource(Res.string.creature_type_aberration),
        CreatureType.BEAST to stringResource(Res.string.creature_type_beast),
        CreatureType.CELESTIAL to stringResource(Res.string.creature_type_celestial),
        CreatureType.CONSTRUCT to stringResource(Res.string.creature_type_construct),
        CreatureType.DRAGON to stringResource(Res.string.creature_type_dragon),
        CreatureType.ELEMENTAL to stringResource(Res.string.creature_type_elemental),
        CreatureType.FEY to stringResource(Res.string.creature_type_fey),
        CreatureType.FIEND to stringResource(Res.string.creature_type_fiend),
        CreatureType.GIANT to stringResource(Res.string.creature_type_giant),
        CreatureType.HUMANOID to stringResource(Res.string.creature_type_humanoid),
        CreatureType.MONSTROSITY to stringResource(Res.string.creature_type_monstrosity),
        CreatureType.OOZE to stringResource(Res.string.creature_type_ooze),
        CreatureType.PLANT to stringResource(Res.string.creature_type_plant),
        CreatureType.UNDEAD to stringResource(Res.string.creature_type_undead),
        CreatureType.UNSPECIFIED to stringResource(Res.string.creature_type_unspecified),
    )

/** the words of a species' type - a built-in name, the player's own name or a dash while it has none */
data class SpeciesTypeLabels(
    private val builtin: Map<CreatureType, String>,
    private val none: String,
) {
    fun of(type: SpeciesType): String =
        when (type) {
            is SpeciesType.Builtin -> builtin.getValue(type.type)
            is SpeciesType.Custom -> type.type.name
            SpeciesType.None -> none
        }

    fun of(species: Species): String = of(species.type)
}

@Composable
fun speciesTypeLabels(): SpeciesTypeLabels =
    SpeciesTypeLabels(creatureTypeLabels(), stringResource(Res.string.species_type_unset))

@Composable
fun damageTypeLabels(): Map<DamageType, String> =
    mapOf(
        DamageType.BLUDGEONING to stringResource(Res.string.damage_type_bludgeoning),
        DamageType.PIERCING to stringResource(Res.string.damage_type_piercing),
        DamageType.SLASHING to stringResource(Res.string.damage_type_slashing),
        DamageType.FIRE to stringResource(Res.string.damage_type_fire),
        DamageType.COLD to stringResource(Res.string.damage_type_cold),
        DamageType.LIGHTNING to stringResource(Res.string.damage_type_lightning),
        DamageType.THUNDER to stringResource(Res.string.damage_type_thunder),
        DamageType.ACID to stringResource(Res.string.damage_type_acid),
        DamageType.POISON to stringResource(Res.string.damage_type_poison),
        DamageType.NECROTIC to stringResource(Res.string.damage_type_necrotic),
        DamageType.RADIANT to stringResource(Res.string.damage_type_radiant),
        DamageType.FORCE to stringResource(Res.string.damage_type_force),
        DamageType.PSYCHIC to stringResource(Res.string.damage_type_psychic),
    )

@Composable
fun damageStateLabels(): Map<DamageState, String> =
    mapOf(
        DamageState.NORMAL to stringResource(Res.string.damage_state_normal),
        DamageState.RESISTANCE to stringResource(Res.string.damage_state_resistance),
        DamageState.IMMUNITY to stringResource(Res.string.damage_state_immunity),
        DamageState.VULNERABILITY to stringResource(Res.string.damage_state_vulnerability),
    )
