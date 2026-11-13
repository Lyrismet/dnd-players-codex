package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_features_add
import incadent.shared.generated.resources.combat_features_done
import incadent.shared.generated.resources.combat_features_facts
import incadent.shared.generated.resources.combat_features_field
import incadent.shared.generated.resources.combat_features_hint
import incadent.shared.generated.resources.combat_features_remove
import incadent.shared.generated.resources.combat_features_shields_title
import incadent.shared.generated.resources.combat_features_title
import incadent.shared.generated.resources.combat_mark_preset_ac
import incadent.shared.generated.resources.combat_mark_preset_save
import incadent.shared.generated.resources.combat_mark_preset_speed
import org.jetbrains.compose.resources.stringResource

/** what the sheet of the features shows and how it answers, the fight and the npc card fill it from their own data */
internal class FeaturesSheetActions(
    val onTextChanged: (String) -> Unit,
    val onAdd: () -> Unit,
    val onRemove: (String) -> Unit,
    val onShieldChanged: (DamageType, DamageState) -> Unit,
    val onClose: () -> Unit,
)

/** the one sheet of the features of a creature: the facts with a field, the shields of every damage type, "Готово" */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun FeaturesBottomSheet(
    overline: String,
    facts: List<String>,
    text: String,
    shields: Map<DamageType, DamageState>,
    actions: FeaturesSheetActions,
) {
    val removeLabel = stringResource(Res.string.combat_features_remove)
    AppBottomSheet(onDismissRequest = actions.onClose) {
        AppSheetColumn(
            scroll = true,
            header = {
                AppSheetHeader(
                    overline = overline,
                    title = stringResource(Res.string.combat_features_title),
                    onClose = actions.onClose,
                )
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormFieldLabel(stringResource(Res.string.combat_features_facts).uppercase())
                AppTextInput(
                    value = text,
                    onValueChange = actions.onTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(Res.string.combat_features_field),
                    style = AppTextInputStyle(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)),
                    trailing = { AddInFieldButton(text.isNotBlank(), actions.onAdd) },
                )
                if (facts.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        facts.forEach { fact ->
                            Row(modifier = Modifier.semantics { contentDescription = removeLabel }) {
                                FeatureChip(fact, trailing = " ×") { actions.onRemove(fact) }
                            }
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        stringResource(Res.string.combat_mark_preset_save),
                        stringResource(Res.string.combat_mark_preset_ac),
                        stringResource(Res.string.combat_mark_preset_speed),
                    ).forEach { preset -> FeatureChip(preset) { actions.onTextChanged("$preset ") } }
                }
                Text(
                    stringResource(Res.string.combat_features_hint),
                    color = AppPalette.TextTertiary,
                    fontSize = AppFontSize.Caption,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormFieldLabel(stringResource(Res.string.combat_features_shields_title).uppercase())
                VulnerabilityGridEditor(shields, actions.onShieldChanged)
            }
            FormPrimaryButton(stringResource(Res.string.combat_features_done), actions.onClose)
        }
    }
}

/** a short fact as a chip, a tap is optional */
@Composable
internal fun FeatureChip(
    text: String,
    trailing: String = "",
    onClick: (() -> Unit)? = null,
) {
    Text(
        text = text + trailing,
        color = AppPalette.TextSecondary,
        fontSize = AppFontSize.Small,
        modifier =
            Modifier
                .defaultMinSize(minHeight = 28.dp)
                .appCard(
                    shape = AppShapes.Card,
                    background = AppPalette.SurfaceElevated,
                    border = AppPalette.Border,
                    onClick = onClick,
                ).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** the "Добавить" inside the field, gold once there is something to add */
@Composable
private fun AddInFieldButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = stringResource(Res.string.combat_features_add),
        color = if (enabled) AppPalette.Background else AppPalette.TextSecondary,
        fontSize = AppFontSize.Small,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier
                .padding(end = 8.dp)
                .appCard(
                    shape = AppShapes.Card,
                    background = if (enabled) AppPalette.Gold else AppPalette.SurfaceElevated,
                    border = null,
                    onClick = if (enabled) onClick else null,
                ).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
