package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDashedTile
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.grouping.levelRoom
import com.lyrismet.incadent.feature.codex.domain.grouping.partyClassLine
import com.lyrismet.incadent.feature.codex.domain.model.PartyClass
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.campaign_player_level_format
import incadent.shared.generated.resources.class_list_add
import incadent.shared.generated.resources.class_list_title
import incadent.shared.generated.resources.class_sheet_overline
import org.jetbrains.compose.resources.stringResource

/**
 * the classes of a character as rows with their levels, two or more show as rows, one stays with the own field.
 * A press on a row or on the dashed gold tile opens the class sheet, [onChange] gets the whole new list.
 */
@Composable
fun ClassListBlock(
    entries: List<PartyClass>,
    addLabel: String,
    onChange: (List<PartyClass>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // the index of the row being edited, entries.size for a new class, null while the sheet is closed
    var editing by remember { mutableStateOf<Int?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (entries.size >= 2) entries.forEachIndexed { index, entry -> ClassRow(entry) { editing = index } }
        if (entries.levelRoom() > 0) {
            AppDashedTile(
                onClick = { editing = entries.size },
                modifier = Modifier.fillMaxWidth(),
                color = AppPalette.Gold.copy(alpha = 0.45f),
                cornerRadius = 11.dp,
            ) {
                Text(addLabel, fontSize = AppFontSize.Body, color = AppPalette.GoldBright)
            }
        }
    }
    editing?.let { index ->
        val current = entries.getOrNull(index)
        val others = entries.filterIndexed { i, _ -> i != index }
        ClassSelectSheet(
            select =
                ClassSelectState(
                    characterClass = current?.characterClass.orEmpty(),
                    subclass = current?.subclass.orEmpty(),
                    level =
                        ClassLevelState(
                            level =
                                (current?.level ?: 1).coerceAtMost(
                                    entries.levelRoom(index.takeIf { current != null }),
                                ),
                            max =
                                entries
                                    .levelRoom(
                                        index.takeIf { current != null },
                                    ).coerceAtLeast(PartyStatRanges.level.first),
                            taken = others.map { it.characterClass },
                            canRemove = current != null && entries.size > 1,
                        ),
                ),
            onChosen = { _, _ -> },
            onChosenWithLevel = { characterClass, subclass, level ->
                val entry = PartyClass(characterClass, subclass, level)
                onChange(
                    if (current ==
                        null
                    ) {
                        entries + entry
                    } else {
                        entries.mapIndexed { i, old -> if (i == index) entry else old }
                    },
                )
                editing = null
            },
            onRemove = {
                onChange(entries.filterIndexed { i, _ -> i != index })
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ClassRow(
    entry: PartyClass,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .appCard(AppShapes.Field, AppPalette.Background, AppPalette.BorderSubtle, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            partyClassLine(entry.characterClass, entry.subclass),
            modifier = Modifier.weight(1f),
            color = AppPalette.TextPrimary,
            fontSize = AppFontSize.Body,
        )
        Text(
            stringResource(Res.string.campaign_player_level_format, entry.level),
            color = AppPalette.TextSecondary,
            fontSize = AppFontSize.Body,
        )
    }
}

/** the sheet over a card that lists the classes of a party member who has several, a change closes it */
@Composable
fun ClassListSheet(
    classes: List<PartyClass>,
    onChange: (List<PartyClass>) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        AppSheetColumn(
            scroll = true,
            header = {
                AppSheetHeader(
                    title = stringResource(Res.string.class_list_title),
                    onClose = onDismiss,
                    overline = stringResource(Res.string.class_sheet_overline),
                )
            },
        ) {
            ClassListBlock(classes, stringResource(Res.string.class_list_add), onChange)
        }
    }
}
