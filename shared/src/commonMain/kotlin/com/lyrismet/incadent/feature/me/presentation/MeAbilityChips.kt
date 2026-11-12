package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppSearchField
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.SearchIcon
import com.lyrismet.incadent.core.designsystem.numeral
import com.lyrismet.incadent.core.format.signed
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_search_close
import incadent.shared.generated.resources.me_search_description
import incadent.shared.generated.resources.me_search_placeholder
import org.jetbrains.compose.resources.stringResource

/** the six ability chips pinned over the list, a press scrolls to the strip, the loupe opens the search */
@Composable
internal fun MeAbilityChips(
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
    active: Ability,
    onJump: (Ability) -> Unit,
) {
    val sheet = character.sheet
    val big = LocalDensity.current.fontScale > LARGE_FONT_SCALE
    val searching = sheet.searchOpen && sheet.query.isNotBlank()
    val description = stringResource(Res.string.me_search_description)
    val bonuses = remember(sheet.strips) { sheet.strips.associate { it.ability to it.bonus } }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(AppPalette.Background)
                .padding(top = 2.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (searching) {
            Box(Modifier.weight(1f))
        } else {
            val abilities = Ability.entries
            Column(
                modifier = Modifier.weight(1f).appCard(shape = AppShapes.Card).padding(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                (if (big) abilities.chunked(ABILITIES_PER_BIG_ROW) else listOf(abilities)).forEach { rowAbilities ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        rowAbilities.forEach { ability ->
                            AbilityChip(
                                ability = ability,
                                bonus = bonuses[ability],
                                active = ability == active,
                                modifier = Modifier.weight(1f),
                            ) { onJump(ability) }
                        }
                    }
                }
            }
        }
        SearchButton(sheet.searchOpen, description) { onEvent(MeEvent.Sheet(SheetEvent.SearchToggled)) }
    }
}

@Composable
private fun SearchButton(
    open: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxHeight()
                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                // as wide as the chips row is tall, so the loupe sits in a square
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .clip(AppShapes.Field)
                .background(AppPalette.Surface)
                .border(1.dp, if (open) AppPalette.Gold else AppPalette.BorderSubtle, AppShapes.Field)
                .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SearchIcon(size = 22.dp)
    }
}

@Composable
private fun AbilityChip(
    ability: Ability,
    bonus: Int?,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .heightIn(min = 44.dp)
                .clip(AppShapes.Control)
                .background(if (active) AppPalette.SurfaceElevated else Color.Transparent)
                .border(1.dp, if (active) AppPalette.Gold.copy(alpha = 0.55f) else Color.Transparent, AppShapes.Control)
                .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(ability.abbrRes()),
            style = AppText.fieldOverline,
            color = if (active) AppPalette.GoldBright else AppPalette.TextSecondary,
        )
        Text(bonus.signed(), style = AppText.numeral(15.sp), color = AppPalette.TextHeading)
    }
}

@Composable
internal fun SearchBlock(
    sheet: MeSheetState,
    onEvent: (MeEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppSearchField(
            query = sheet.query,
            onQueryChange = { onEvent(MeEvent.Sheet(SheetEvent.QueryChanged(it))) },
            placeholder = stringResource(Res.string.me_search_placeholder),
            modifier = Modifier.fillMaxWidth(),
            height = 44.dp,
        )
        if (sheet.query.isBlank()) {
            Text(
                stringResource(Res.string.me_search_close),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
        }
    }
}
