package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.model.SpeciesType
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.creature_type_aberration
import dndplayerscodex.shared.generated.resources.creature_type_beast
import dndplayerscodex.shared.generated.resources.creature_type_celestial
import dndplayerscodex.shared.generated.resources.creature_type_construct
import dndplayerscodex.shared.generated.resources.creature_type_dragon
import dndplayerscodex.shared.generated.resources.creature_type_elemental
import dndplayerscodex.shared.generated.resources.creature_type_fey
import dndplayerscodex.shared.generated.resources.creature_type_fiend
import dndplayerscodex.shared.generated.resources.creature_type_giant
import dndplayerscodex.shared.generated.resources.creature_type_humanoid
import dndplayerscodex.shared.generated.resources.creature_type_monstrosity
import dndplayerscodex.shared.generated.resources.creature_type_ooze
import dndplayerscodex.shared.generated.resources.creature_type_plant
import dndplayerscodex.shared.generated.resources.creature_type_undead
import dndplayerscodex.shared.generated.resources.creature_type_unspecified
import dndplayerscodex.shared.generated.resources.damage_state_immunity
import dndplayerscodex.shared.generated.resources.damage_state_normal
import dndplayerscodex.shared.generated.resources.damage_state_resistance
import dndplayerscodex.shared.generated.resources.damage_state_unknown
import dndplayerscodex.shared.generated.resources.damage_state_vulnerability
import dndplayerscodex.shared.generated.resources.damage_type_acid
import dndplayerscodex.shared.generated.resources.damage_type_bludgeoning
import dndplayerscodex.shared.generated.resources.damage_type_cold
import dndplayerscodex.shared.generated.resources.damage_type_fire
import dndplayerscodex.shared.generated.resources.damage_type_force
import dndplayerscodex.shared.generated.resources.damage_type_lightning
import dndplayerscodex.shared.generated.resources.damage_type_necrotic
import dndplayerscodex.shared.generated.resources.damage_type_piercing
import dndplayerscodex.shared.generated.resources.damage_type_poison
import dndplayerscodex.shared.generated.resources.damage_type_psychic
import dndplayerscodex.shared.generated.resources.damage_type_radiant
import dndplayerscodex.shared.generated.resources.damage_type_slashing
import dndplayerscodex.shared.generated.resources.damage_type_thunder
import dndplayerscodex.shared.generated.resources.species_type_unset
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
        DamageState.UNKNOWN to stringResource(Res.string.damage_state_unknown),
        DamageState.NORMAL to stringResource(Res.string.damage_state_normal),
        DamageState.RESISTANCE to stringResource(Res.string.damage_state_resistance),
        DamageState.IMMUNITY to stringResource(Res.string.damage_state_immunity),
        DamageState.VULNERABILITY to stringResource(Res.string.damage_state_vulnerability),
    )
