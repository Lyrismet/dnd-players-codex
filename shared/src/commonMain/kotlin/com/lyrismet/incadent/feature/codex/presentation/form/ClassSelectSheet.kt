package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSearchField
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.PillFill
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.core.localization.currentAppLanguage
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.feature.codex.domain.grouping.isSrdClass
import com.lyrismet.incadent.feature.codex.domain.grouping.srdClassNamed
import com.lyrismet.incadent.feature.codex.domain.grouping.srdClassesMatching
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.class_sheet_custom
import incadent.shared.generated.resources.class_sheet_done
import incadent.shared.generated.resources.class_sheet_no_class
import incadent.shared.generated.resources.class_sheet_not_in_srd
import incadent.shared.generated.resources.class_sheet_overline
import incadent.shared.generated.resources.class_sheet_own_badge
import incadent.shared.generated.resources.class_sheet_search
import incadent.shared.generated.resources.class_sheet_srd_source
import incadent.shared.generated.resources.class_sheet_subclass_example
import incadent.shared.generated.resources.class_sheet_subclass_free
import incadent.shared.generated.resources.class_sheet_subclass_label
import incadent.shared.generated.resources.class_sheet_title_empty
import org.jetbrains.compose.resources.stringResource

private const val GRID_COLUMNS = 3

/** the one sheet that sets the class and the subclass of a party member or an npc, own classes are typed in */
@Composable
fun ClassSelectSheet(
    select: ClassSelectState,
    onChosen: (characterClass: String, subclass: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember(select) { mutableStateOf(select.characterClass) }
    var subclass by remember(select) { mutableStateOf(select.subclass) }
    AppBottomSheet(onDismissRequest = onDismiss) {
        AppSheetColumn(
            spacing = 14.dp,
            scroll = true,
            header = {
                AppSheetHeader(
                    title = picked.ifBlank { stringResource(Res.string.class_sheet_title_empty) },
                    onClose = onDismiss,
                    overline = stringResource(Res.string.class_sheet_overline),
                )
            },
        ) {
            ClassPicker(picked) {
                picked = it
                if (it.isBlank()) subclass = ""
            }
            if (picked.isNotBlank()) SubclassField(picked, subclass) { subclass = it }
            FormPrimaryButton(
                text = stringResource(Res.string.class_sheet_done),
                onClick = { onChosen(picked.trim(), if (picked.isBlank()) "" else subclass.trim()) },
            )
        }
    }
}

// the search, the grid of the SRD classes, the own class typed in and the "no class" row
@Composable
private fun ColumnScope.ClassPicker(
    picked: String,
    onPicked: (String) -> Unit,
) {
    val russian = currentAppLanguage() == AppLanguage.RUSSIAN
    var query by remember { mutableStateOf("") }
    val matches = srdClassesMatching(query, russian)
    val typed = query.trim()
    val ownName = typed.capitalizeFirst()
    val offersOwn = typed.isNotEmpty() && matches.none { it.isNamed(typed) }
    AppSearchField(
        query = query,
        onQueryChange = { query = it },
        placeholder = stringResource(Res.string.class_sheet_search),
        modifier = Modifier.fillMaxWidth(),
        height = 46.dp,
    )
    if (picked.isNotBlank() && !isSrdClass(picked)) PickedOwnClass(picked) { onPicked("") }
    if (matches.isEmpty()) {
        Text(
            stringResource(Res.string.class_sheet_not_in_srd),
            color = AppPalette.TextSecondary,
            fontSize = AppFontSize.Body,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
    matches.chunked(GRID_COLUMNS).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { srd ->
                val selected = srd.isNamed(picked)
                SelectablePill(
                    text = srd.name(russian),
                    selected = selected,
                    onClick = { onPicked(if (selected) "" else srd.name(russian)) },
                    modifier = Modifier.weight(1f),
                    selectedFill = PillFill.GoldTint,
                )
            }
            repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
    if (offersOwn) {
        ClassTextRow(stringResource(Res.string.class_sheet_custom, ownName), AppPalette.Gold) {
            onPicked(ownName)
            query = ""
        }
    }
    ClassTextRow(
        stringResource(Res.string.class_sheet_no_class),
        if (picked.isBlank()) AppPalette.GoldBright else AppPalette.TextMuted,
    ) { onPicked("") }
}

// the subclass is typed in, the one the SRD describes for the class is offered as an example and a one-press fill
@Composable
private fun SubclassField(
    characterClass: String,
    subclass: String,
    onChanged: (String) -> Unit,
) {
    val russian = currentAppLanguage() == AppLanguage.RUSSIAN
    val srd = srdClassNamed(characterClass)?.subclassNames(russian).orEmpty()
    FormFieldLabel(stringResource(Res.string.class_sheet_subclass_label).uppercase())
    AppTextInput(
        value = subclass,
        onValueChange = onChanged,
        modifier = Modifier.fillMaxWidth(),
        placeholder =
            srd.firstOrNull()?.let { stringResource(Res.string.class_sheet_subclass_example, it) }
                ?: stringResource(Res.string.class_sheet_subclass_free),
    )
    if (srd.isNotEmpty() && subclass.isBlank()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            srd.forEach { name -> SelectablePill(text = name, selected = false, onClick = { onChanged(name) }) }
            Text(
                stringResource(Res.string.class_sheet_srd_source),
                color = AppPalette.TextTertiary,
                fontSize = AppFontSize.Caption,
            )
        }
    }
}

// the class typed in by hand, it has no tile in the grid, a press takes it back
@Composable
private fun PickedOwnClass(
    name: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.Gold.copy(alpha = 0.14f),
                    border = AppPalette.Gold.copy(alpha = 0.6f),
                    onClick = onClick,
                ).padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = AppPalette.Gold, fontSize = AppFontSize.Medium, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(Res.string.class_sheet_own_badge),
            color = AppPalette.TextSecondary,
            fontSize = AppFontSize.Caption,
        )
    }
}

@Composable
private fun ClassTextRow(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        text,
        color = color,
        fontSize = AppFontSize.Medium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
    )
}
