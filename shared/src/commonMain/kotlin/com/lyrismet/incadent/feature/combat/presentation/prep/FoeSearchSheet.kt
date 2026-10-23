package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppSearchField
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.AppTitleSubtitle
import com.lyrismet.incadent.core.designsystem.component.Dot
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.presentation.style.SpeciesMentionColor
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_prep_create_codex
import dndplayerscodex.shared.generated.resources.combat_prep_create_hint_ally
import dndplayerscodex.shared.generated.resources.combat_prep_create_hint_enemy
import dndplayerscodex.shared.generated.resources.combat_prep_done
import dndplayerscodex.shared.generated.resources.combat_prep_done_count
import dndplayerscodex.shared.generated.resources.combat_prep_empty_ally
import dndplayerscodex.shared.generated.resources.combat_prep_empty_enemy
import dndplayerscodex.shared.generated.resources.combat_prep_less
import dndplayerscodex.shared.generated.resources.combat_prep_more
import dndplayerscodex.shared.generated.resources.combat_prep_no_match_ally
import dndplayerscodex.shared.generated.resources.combat_prep_no_match_enemy
import dndplayerscodex.shared.generated.resources.combat_prep_search_ally
import dndplayerscodex.shared.generated.resources.combat_prep_search_enemy
import dndplayerscodex.shared.generated.resources.combat_prep_search_hint
import dndplayerscodex.shared.generated.resources.combat_prep_sheet_allies
import dndplayerscodex.shared.generated.resources.combat_prep_sheet_enemies
import dndplayerscodex.shared.generated.resources.combat_prep_sheet_overline
import dndplayerscodex.shared.generated.resources.combat_prep_temp
import dndplayerscodex.shared.generated.resources.combat_prep_temp_hint_ally
import dndplayerscodex.shared.generated.resources.combat_prep_temp_hint_enemy
import dndplayerscodex.shared.generated.resources.combat_search_overline_reinforcement
import org.jetbrains.compose.resources.stringResource

private const val FADED_ALPHA = 0.5f

/** the records of the codex and the bestiary to put on one side, or a typed name for this fight only */
@Composable
internal fun FoeSearchSheet(
    search: FoeSearchState,
    onEvent: (CombatPrepEvent) -> Unit,
) {
    val enemy = search.side == PrepSide.ENEMY
    val accent = if (enemy) AppPalette.MaroonBright else AppPalette.EmeraldBright
    AppBottomSheet(onDismissRequest = { onEvent(CombatPrepEvent.SearchClosed) }) {
        AppSheetColumn(scroll = true) {
            AppSheetHeader(
                title =
                    stringResource(
                        if (enemy) Res.string.combat_prep_sheet_enemies else Res.string.combat_prep_sheet_allies,
                    ),
                onClose = { onEvent(CombatPrepEvent.SearchClosed) },
                overline =
                    stringResource(
                        if (search.reinforcement) {
                            Res.string.combat_search_overline_reinforcement
                        } else {
                            Res.string.combat_prep_sheet_overline
                        },
                    ),
                overlineColor = accent,
            )
            AppSearchField(
                query = search.query,
                onQueryChange = { onEvent(CombatPrepEvent.QueryChanged(it)) },
                placeholder =
                    stringResource(
                        if (enemy) Res.string.combat_prep_search_enemy else Res.string.combat_prep_search_ally,
                    ),
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp,
                textSize = AppFontSize.MediumPlus,
                background = AppPalette.Background,
            )
            Text(
                text = stringResource(Res.string.combat_prep_search_hint),
                color = AppPalette.TextSecondary,
                fontSize = AppFontSize.Small,
                lineHeight = AppLineHeight.Row,
            )
            if (search.rows.isEmpty()) SearchNote(enemy, hasQuery = search.query.isNotBlank())
            if (search.rows.isNotEmpty()) FoeRowsCard(search.rows, accent, search.reinforcement, onEvent)
            if (search.canCreate) CreateOptions(search.query.trim(), enemy, onEvent)
            DoneButton(search.done) { onEvent(CombatPrepEvent.SearchClosed) }
        }
    }
}

@Composable
private fun SearchNote(
    enemy: Boolean,
    hasQuery: Boolean,
) {
    val text =
        when {
            hasQuery && enemy -> stringResource(Res.string.combat_prep_no_match_enemy)
            hasQuery -> stringResource(Res.string.combat_prep_no_match_ally)
            enemy -> stringResource(Res.string.combat_prep_empty_enemy)
            else -> stringResource(Res.string.combat_prep_empty_ally)
        }
    Text(text = text, color = AppPalette.TextSecondary, fontSize = AppFontSize.Body, lineHeight = AppLineHeight.Row)
}

@Composable
private fun DoneButton(
    count: Int,
    onClick: () -> Unit,
) {
    FormPrimaryButton(
        text =
            if (count > 0) {
                stringResource(Res.string.combat_prep_done_count, count)
            } else {
                stringResource(Res.string.combat_prep_done)
            },
        onClick = onClick,
    )
}

@Composable
private fun FoeRowsCard(
    rows: List<FoeRow>,
    accent: Color,
    reinforcement: Boolean,
    onEvent: (CombatPrepEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().appCard(background = AppPalette.Background).padding(horizontal = 12.dp)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) AppDivider(color = AppPalette.SurfacePopover)
            FoeRowItem(row, accent, reinforcement, onEvent)
        }
    }
}

@Composable
private fun FoeRowItem(
    row: FoeRow,
    accent: Color,
    reinforcement: Boolean,
    onEvent: (CombatPrepEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable { onEvent(CombatPrepEvent.FoeTapped(row.key)) }
                    .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                val color = if (row.isSpecies) SpeciesMentionColor.foreground else accent
                Dot(
                    size = 8.dp,
                    color = if (row.isSpecies) Color.Transparent else color,
                    borderColor = color.takeIf { row.isSpecies },
                )
            }
            AppTitleSubtitle(title = row.name, subtitle = row.sub, modifier = Modifier.weight(1f))
        }
        if (reinforcement && row.count > 0) {
            CountBadge(row)
        } else if (row.isSpecies && row.count > 0) {
            AppStepper(
                count = row.count,
                onDecrease = { onEvent(CombatPrepEvent.FoeDecreased(row.key)) },
                onIncrease = { onEvent(CombatPrepEvent.FoeTapped(row.key)) },
                decreaseDescription = stringResource(Res.string.combat_prep_less),
                increaseDescription = stringResource(Res.string.combat_prep_more),
            )
        } else {
            AddMark(done = !row.isSpecies && row.count > 0) { onEvent(CombatPrepEvent.FoeTapped(row.key)) }
        }
    }
}

/** how many of a kind joined the fight, a named npc shows a faded tick since it cannot join twice */
@Composable
private fun CountBadge(row: FoeRow) {
    Text(
        text = if (row.isSpecies) "×${row.count}" else "✓",
        color = AppPalette.Background,
        fontSize = AppFontSize.Small,
        fontWeight = FontWeight.Bold,
        modifier =
            Modifier
                .alpha(if (row.isSpecies) 1f else FADED_ALPHA)
                .appCard(shape = AppShapes.Control, background = AppPalette.MaroonBright, border = null)
                .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun AddMark(
    done: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.size(44.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .appCard(
                        shape = CircleShape,
                        background = if (done) AppPalette.Gold else AppPalette.Gold.copy(alpha = 0.14f),
                        border = null,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (done) "✓" else "+",
                color = if (done) AppPalette.Background else AppPalette.GoldBright,
                fontSize = AppFontSize.Medium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CreateOptions(
    name: String,
    enemy: Boolean,
    onEvent: (CombatPrepEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().appCard(background = AppPalette.Background).padding(horizontal = 12.dp)) {
        CreateOption(
            title = stringResource(Res.string.combat_prep_create_codex, name),
            hint =
                stringResource(
                    if (enemy) Res.string.combat_prep_create_hint_enemy else Res.string.combat_prep_create_hint_ally,
                ),
            titleColor = AppPalette.GoldBright,
        ) { onEvent(CombatPrepEvent.CreateRequested) }
        AppDivider(color = AppPalette.SurfacePopover)
        CreateOption(
            title = stringResource(Res.string.combat_prep_temp, name),
            hint =
                stringResource(
                    if (enemy) Res.string.combat_prep_temp_hint_enemy else Res.string.combat_prep_temp_hint_ally,
                ),
            titleColor = AppPalette.TextPrimary,
        ) { onEvent(CombatPrepEvent.TempAdded) }
    }
}

@Composable
private fun CreateOption(
    title: String,
    hint: String,
    titleColor: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        AppTitleSubtitle(title = title, subtitle = hint, titleColor = titleColor, maxLines = 2)
    }
}
