package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppPickSheet
import com.lyrismet.incadent.core.designsystem.component.AppRemoveButton
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormInlineField
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.MAX_DAMAGE_ROWS
import com.lyrismet.incadent.feature.codex.domain.model.PROJECTILES_RANGE
import com.lyrismet.incadent.feature.codex.domain.model.SpellDamage
import com.lyrismet.incadent.feature.codex.domain.model.SpellEffect
import com.lyrismet.incadent.feature.codex.domain.model.isValidDice
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_damage_add
import incadent.shared.generated.resources.me_spells_damage_dice_hint
import incadent.shared.generated.resources.me_spells_damage_more
import incadent.shared.generated.resources.me_spells_damage_type_empty
import incadent.shared.generated.resources.me_spells_damage_type_hint
import incadent.shared.generated.resources.me_spells_damage_type_none
import incadent.shared.generated.resources.me_spells_damage_type_search
import incadent.shared.generated.resources.me_spells_damage_type_title
import incadent.shared.generated.resources.me_spells_edit_damage
import incadent.shared.generated.resources.me_spells_projectiles
import incadent.shared.generated.resources.me_spells_projectiles_hint
import incadent.shared.generated.resources.me_spells_step_less
import incadent.shared.generated.resources.me_spells_step_more
import org.jetbrains.compose.resources.stringResource

private val DiceWidth = 96.dp
private val ROW_HEIGHT = 46.dp

// healing and temporary hit points sit on top of the thirteen damage types
private val Effects: List<SpellEffect> =
    listOf(SpellEffect.Healing, SpellEffect.TempHp) + DamageType.entries.map { SpellEffect.Damage(it) }

/** the damage rows of the form, each one dice and a type, and the projectile count that multiplies them all */
@Composable
internal fun DamageFields(
    spell: CharacterSpell,
    change: (CharacterSpell) -> Unit,
) {
    var typeOf by remember { mutableStateOf<Int?>(null) }
    val rows = spell.damage
    val setRows = { next: List<SpellDamage> -> change(spell.copy(damage = next)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(stringResource(Res.string.me_spells_edit_damage))
        rows.forEachIndexed { index, row ->
            DamageRow(
                row,
                onDice = { dice ->
                    setRows(rows.replaceAt(index) { it.copy(dice = dice) })
                },
                onType = { typeOf = index },
                onRemove = { setRows(rows.removeAt(index)) },
            )
        }
        if (rows.size < MAX_DAMAGE_ROWS) {
            SpellAddTile(
                stringResource(
                    if (rows.isEmpty()) Res.string.me_spells_damage_add else Res.string.me_spells_damage_more,
                ),
            ) { setRows(rows + SpellDamage("")) }
        }
        if (rows.isNotEmpty()) Projectiles(spell.projectiles) { change(spell.copy(projectiles = it)) }
    }
    typeOf?.let { index ->
        EffectSheet(
            chosen = rows.getOrNull(index)?.effect,
            onClose = { typeOf = null },
            onPick = { effect ->
                setRows(rows.replaceAt(index) { it.copy(effect = effect) })
                typeOf = null
            },
        )
    }
}

@Composable
private fun DamageRow(
    row: SpellDamage,
    onDice: (String) -> Unit,
    onType: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FormInlineField(
            value = row.dice,
            onChange = onDice,
            modifier = Modifier.width(DiceWidth),
            placeholder = stringResource(Res.string.me_spells_damage_dice_hint),
            maxLength = DICE_MAX_LENGTH,
            invalid = !isValidDice(row.dice),
            counter = false,
        )
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = ROW_HEIGHT)
                .appCard(AppShapes.Field, AppPalette.Background, AppPalette.Border, onClick = onType)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpellEffectGlyph(row.effect, size = 17.dp)
            Text(
                effectTitle(row.effect).ifBlank { stringResource(Res.string.me_spells_damage_type_hint) },
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.effect == null) AppPalette.TextTertiary else AppPalette.TextPrimary,
                maxLines = 1,
            )
            Text("›", color = AppPalette.TextTertiary)
        }
        AppRemoveButton(onClick = onRemove, size = ROW_HEIGHT)
    }
}

@Composable
private fun Projectiles(
    count: Int,
    onChange: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .appCard(AppShapes.Field, AppPalette.Background, AppPalette.BorderSubtle)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.me_spells_projectiles),
                style = MaterialTheme.typography.titleSmall,
                color = AppPalette.TextPrimary,
            )
            Text(
                stringResource(Res.string.me_spells_projectiles_hint),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
        }
        AppStepper(
            count = count,
            onDecrease = { onChange((count - 1).coerceAtLeast(PROJECTILES_RANGE.first)) },
            onIncrease = { onChange((count + 1).coerceAtMost(PROJECTILES_RANGE.last)) },
            decreaseDescription = stringResource(Res.string.me_spells_step_less),
            increaseDescription = stringResource(Res.string.me_spells_step_more),
            tiles = true,
            removesAtOne = false,
        )
    }
}

private const val DICE_MAX_LENGTH = 16

@Composable
private fun EffectSheet(
    chosen: SpellEffect?,
    onClose: () -> Unit,
    onPick: (SpellEffect?) -> Unit,
) {
    AppPickSheet(
        items = Effects,
        chosen = chosen,
        title = stringResource(Res.string.me_spells_damage_type_title),
        emptyText = stringResource(Res.string.me_spells_damage_type_empty),
        searchPlaceholder = stringResource(Res.string.me_spells_damage_type_search),
        clearLabel = stringResource(Res.string.me_spells_damage_type_none),
        label = { effectTitle(it) },
        onPick = onPick,
        onDismiss = onClose,
        itemIcon = { SpellEffectGlyph(it, size = 17.dp) },
    )
}
