package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.me.domain.AbilityStrip
import com.lyrismet.incadent.feature.me.domain.isDown
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_features_placeholder
import dndplayerscodex.shared.generated.resources.me_features_title
import dndplayerscodex.shared.generated.resources.me_fill_banner_action
import dndplayerscodex.shared.generated.resources.me_fill_banner_text
import dndplayerscodex.shared.generated.resources.me_fill_banner_title
import dndplayerscodex.shared.generated.resources.me_help_skills_body
import dndplayerscodex.shared.generated.resources.me_help_skills_title
import dndplayerscodex.shared.generated.resources.me_inspiration
import dndplayerscodex.shared.generated.resources.me_languages_placeholder
import dndplayerscodex.shared.generated.resources.me_languages_title
import dndplayerscodex.shared.generated.resources.me_personality_placeholder
import dndplayerscodex.shared.generated.resources.me_personality_title
import dndplayerscodex.shared.generated.resources.me_search_empty
import dndplayerscodex.shared.generated.resources.me_sheet_section
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** the parts of the sheet in the order they are drawn, a part is only there when it has something to show */
private sealed interface SheetItem {
    data object Fill : SheetItem

    data object Section : SheetItem

    data object Chips : SheetItem

    data object Search : SheetItem

    data object Empty : SheetItem

    data class Strip(
        val strip: AbilityStrip,
    ) : SheetItem

    data object Languages : SheetItem

    data object Features : SheetItem

    data object About : SheetItem

    data object Personality : SheetItem
}

private fun sheetItems(sheet: MeSheetState): List<SheetItem> {
    val searching = sheet.searchOpen && sheet.query.isNotBlank()
    return buildList {
        if (!sheet.filled) add(SheetItem.Fill)
        if (!searching) add(SheetItem.Section)
        add(SheetItem.Chips)
        if (sheet.searchOpen) add(SheetItem.Search)
        if (searching && sheet.strips.isEmpty()) add(SheetItem.Empty)
        sheet.strips.forEach { add(SheetItem.Strip(it)) }
        if (!searching) {
            add(SheetItem.Languages)
            add(SheetItem.Features)
            if (sheet.background.isNotBlank() || sheet.alignment.isNotBlank()) {
                add(SheetItem.About)
            }
            add(SheetItem.Personality)
        }
    }
}

@Composable
internal fun MeTabContent(
    character: MeCharacter,
    listState: LazyListState,
    onEvent: (MeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (character.tab) {
        MeTab.SHEET -> MeSheetTab(character, listState, onEvent, modifier)
        MeTab.SPELLS -> MeSpellsTab(character, listState, onEvent, modifier)
        MeTab.RESOURCES -> MeResourcesTab(character, listState, onEvent, modifier)
        MeTab.ITEMS -> MeItemsTab(character, listState, onEvent, modifier)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun MeSheetTab(
    character: MeCharacter,
    listState: LazyListState,
    onEvent: (MeEvent) -> Unit,
    modifier: Modifier,
) {
    val sheet = character.sheet
    val items = remember(sheet) { sheetItems(sheet) }
    val scope = rememberCoroutineScope()
    val firstStrip = items.indexOfFirst { it is SheetItem.Strip }
    // the chip of the strip at the top of the screen is lit
    val active by remember(items) {
        derivedStateOf {
            val top = listState.firstVisibleItemIndex.coerceAtLeast(firstStrip)
            (items.getOrNull(top) as? SheetItem.Strip)?.strip?.ability ?: Ability.STRENGTH
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 40.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (character.vitals.isDown) item(key = "death") { MeDeathSaves(character, onEvent) }
        items.forEachIndexed { index, part ->
            when (part) {
                SheetItem.Chips ->
                    stickyHeader(key = "chips") {
                        MeAbilityChips(character, onEvent, active) { ability ->
                            val target = items.indexOfFirst { it is SheetItem.Strip && it.strip.ability == ability }
                            if (target >= 0) scope.launch { listState.animateScrollToItem(target + 1) }
                        }
                    }
                else -> item(key = itemKey(part, index)) { SheetPart(part, character, onEvent) }
            }
        }
    }
}

private fun itemKey(
    part: SheetItem,
    index: Int,
): String = if (part is SheetItem.Strip) "strip-${part.strip.ability}" else "part-$index"

@Composable
private fun SheetPart(
    part: SheetItem,
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
) {
    val sheet = character.sheet
    when (part) {
        SheetItem.Fill -> FillBanner { onEvent(MeEvent.Sheet(SheetEvent.FillRequested)) }
        SheetItem.Section -> SectionHeader(sheet.inspiration) { onEvent(MeEvent.Sheet(SheetEvent.InspirationToggled)) }
        SheetItem.Chips -> Unit
        SheetItem.Search -> SearchBlock(sheet, onEvent)
        SheetItem.Empty ->
            Text(
                stringResource(Res.string.me_search_empty),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
        is SheetItem.Strip -> StripCard(part.strip, onEvent)
        SheetItem.Languages ->
            TextBlock(Res.string.me_languages_title, Res.string.me_languages_placeholder, sheet.languages) {
                onEvent(MeEvent.Sheet(SheetEvent.TextChanged(MeText.LANGUAGES, it)))
            }
        SheetItem.Features ->
            TextBlock(Res.string.me_features_title, Res.string.me_features_placeholder, sheet.features) {
                onEvent(MeEvent.Sheet(SheetEvent.TextChanged(MeText.FEATURES, it)))
            }
        SheetItem.About -> AboutBlock(sheet) { onEvent(MeEvent.Sheet(SheetEvent.AboutTapped)) }
        SheetItem.Personality ->
            TextBlock(Res.string.me_personality_title, Res.string.me_personality_placeholder, sheet.personality) {
                onEvent(MeEvent.Sheet(SheetEvent.TextChanged(MeText.PERSONALITY, it)))
            }
    }
}

@Composable
private fun FillBanner(onClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(background = AppPalette.Gold.copy(alpha = 0.08f), border = AppPalette.Gold.copy(alpha = 0.35f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(Res.string.me_fill_banner_title),
            style = MaterialTheme.typography.titleSmall,
            color = AppPalette.TextHeading,
        )
        Text(stringResource(Res.string.me_fill_banner_text), style = AppText.footnote, color = AppPalette.TextMuted)
        AppButton(
            text = stringResource(Res.string.me_fill_banner_action),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Solid,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionHeader(
    inspiration: Boolean,
    onInspiration: () -> Unit,
) {
    // a big system font drops the inspiration switch under the title instead of squeezing it
    FlowRow(
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).trimBottom(SECTION_GAP_TRIM),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.Center,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MeSectionTitle(
            stringResource(Res.string.me_sheet_section),
            helpTitle = stringResource(Res.string.me_help_skills_title),
            helpBody = stringResource(Res.string.me_help_skills_body),
        )
        InspirationToggle(inspiration, onInspiration)
    }
}

@Composable
private fun InspirationToggle(
    inspiration: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (inspiration) AppPalette.GoldBright else Color.Transparent)
                .border(1.5.dp, if (inspiration) AppPalette.GoldBright else AppPalette.RadioIdle, CircleShape),
        )
        Text(
            stringResource(Res.string.me_inspiration),
            style = MaterialTheme.typography.labelLarge,
            color = if (inspiration) AppPalette.GoldBright else AppPalette.TextSecondary,
        )
    }
}

private val SECTION_GAP_TRIM = 14.dp

// reports less height than drawn so the title sits closer to the chips under it
private fun Modifier.trimBottom(amount: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, (placeable.height - amount.roundToPx()).coerceAtLeast(0)) { placeable.place(0, 0) }
    }
