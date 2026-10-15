package com.lyrismet.incadent.feature.campaign.presentation.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSearchField
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.PillSize
import com.lyrismet.incadent.core.designsystem.component.RadioDot
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SelectCheckbox
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignCreateStep
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetState
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSourceItem
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignTransferRow
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignTransferTab
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.campaign_copy_phrases
import dndplayerscodex.shared.generated.resources.campaign_copy_tags
import dndplayerscodex.shared.generated.resources.campaign_create_button
import dndplayerscodex.shared.generated.resources.campaign_create_name_label
import dndplayerscodex.shared.generated.resources.campaign_create_name_placeholder
import dndplayerscodex.shared.generated.resources.campaign_create_next
import dndplayerscodex.shared.generated.resources.campaign_create_overline
import dndplayerscodex.shared.generated.resources.campaign_create_title
import dndplayerscodex.shared.generated.resources.campaign_create_world_label
import dndplayerscodex.shared.generated.resources.campaign_items_all
import dndplayerscodex.shared.generated.resources.campaign_items_back
import dndplayerscodex.shared.generated.resources.campaign_items_empty
import dndplayerscodex.shared.generated.resources.campaign_items_none
import dndplayerscodex.shared.generated.resources.campaign_items_note
import dndplayerscodex.shared.generated.resources.campaign_items_search
import dndplayerscodex.shared.generated.resources.campaign_items_title
import dndplayerscodex.shared.generated.resources.campaign_items_transfer
import dndplayerscodex.shared.generated.resources.campaign_world_new
import dndplayerscodex.shared.generated.resources.campaign_world_same
import dndplayerscodex.shared.generated.resources.campaign_world_same_hint
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CampaignCreateSheet(
    sheet: CampaignSheetState.Create,
    onEvent: (CampaignSheetEvent) -> Unit,
) {
    val onItems = sheet.step == CampaignCreateStep.ITEMS
    AppBottomSheet(onDismissRequest = { onEvent(CampaignSheetEvent.Dismissed) }) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppSheetHeader(
                overline = stringResource(Res.string.campaign_create_overline),
                title =
                    stringResource(if (onItems) Res.string.campaign_items_title else Res.string.campaign_create_title),
                onClose = { onEvent(CampaignSheetEvent.Dismissed) },
            )
            if (onItems) CampaignItemsStep(sheet, onEvent) else CampaignFormStep(sheet, onEvent)
        }
    }
}

@Composable
private fun CampaignFormStep(
    sheet: CampaignSheetState.Create,
    onEvent: (CampaignSheetEvent) -> Unit,
) {
    FormTextField(
        label = stringResource(Res.string.campaign_create_name_label),
        value = sheet.name,
        onChange = { onEvent(CampaignSheetEvent.NameChanged(it)) },
        placeholder = stringResource(Res.string.campaign_create_name_placeholder),
        required = true,
    )
    CampaignWorldSection(sheet, onEvent)
    FormPrimaryButton(
        text =
            stringResource(
                if (sheet.sameWorld) Res.string.campaign_create_next else Res.string.campaign_create_button,
            ),
        onClick = { onEvent(CampaignSheetEvent.FormNext) },
        enabled = sheet.canProceed,
    )
}

@Composable
private fun CampaignWorldSection(
    sheet: CampaignSheetState.Create,
    onEvent: (CampaignSheetEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionOverline(
            text = stringResource(Res.string.campaign_create_world_label),
            color = AppPalette.TextSecondary,
            letterSpacing = 0.14.em,
        )
        WorldOptionRow(
            title = stringResource(Res.string.campaign_world_new),
            selected = !sheet.sameWorld,
            onClick = { onEvent(CampaignSheetEvent.WorldSelected(false)) },
        )
        if (sheet.sources.isNotEmpty()) {
            WorldOptionRow(
                title = stringResource(Res.string.campaign_world_same),
                selected = sheet.sameWorld,
                onClick = { onEvent(CampaignSheetEvent.WorldSelected(true)) },
            )
        }
        if (sheet.sameWorld) {
            sheet.sources.forEach { source ->
                SourceRow(source, selected = source.id == sheet.sourceId) {
                    onEvent(CampaignSheetEvent.SourceSelected(source.id))
                }
            }
            CampaignNote(stringResource(Res.string.campaign_world_same_hint))
            CheckRow(
                text = stringResource(Res.string.campaign_copy_tags),
                checked = sheet.copyTags,
                onClick = { onEvent(CampaignSheetEvent.CopyTagsToggled) },
            )
            CheckRow(
                text = stringResource(Res.string.campaign_copy_phrases),
                checked = sheet.copyPhrases,
                onClick = { onEvent(CampaignSheetEvent.CopyPhrasesToggled) },
            )
        }
    }
}

@Composable
private fun WorldOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(RoundedCornerShape(14.dp), AppPalette.Background, AppPalette.BorderSubtle, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioDot(selected)
        Text(
            text = title,
            color = AppPalette.TextHeading,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun SourceRow(
    source: CampaignSourceItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = RoundedCornerShape(12.dp),
                    background = if (selected) AppPalette.Gold.copy(alpha = 0.1f) else Color.Transparent,
                    border = if (selected) AppPalette.Gold.copy(alpha = 0.5f) else AppPalette.Border,
                    onClick = onClick,
                ).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = (if (selected) "✓ " else "") + source.name,
            color = if (selected) AppPalette.GoldBright else AppPalette.TextDescription,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = source.subtitle,
            color = AppPalette.TextSecondary,
            maxLines = 1,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CheckRow(
    text: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(RoundedCornerShape(10.dp), Color.Transparent, null, onClick = onClick)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelectCheckbox(checked)
        Text(
            text = text,
            color = AppPalette.TextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        )
    }
}

@Composable
private fun CampaignItemsStep(
    sheet: CampaignSheetState.Create,
    onEvent: (CampaignSheetEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectablePill(
            text = sheet.placesLabel,
            selected = sheet.tab == CampaignTransferTab.PLACES,
            onClick = { onEvent(CampaignSheetEvent.TabSelected(CampaignTransferTab.PLACES)) },
            size = PillSize.Small,
            background = AppPalette.Surface,
            unselectedContent = AppPalette.TextDescription,
        )
        SelectablePill(
            text = sheet.npcsLabel,
            selected = sheet.tab == CampaignTransferTab.NPCS,
            onClick = { onEvent(CampaignSheetEvent.TabSelected(CampaignTransferTab.NPCS)) },
            size = PillSize.Small,
            background = AppPalette.Surface,
            unselectedContent = AppPalette.TextDescription,
        )
    }
    TransferSearchRow(sheet, onEvent)
    TransferList(sheet, onEvent)
    CampaignNote(stringResource(Res.string.campaign_items_note))
    FormPrimaryButton(
        text = pluralStringResource(Res.plurals.campaign_items_transfer, sheet.transferCount, sheet.transferCount),
        onClick = { onEvent(CampaignSheetEvent.FormNext) },
    )
    Text(
        text = stringResource(Res.string.campaign_items_back),
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(RoundedCornerShape(10.dp), Color.Transparent, null, onClick = {
                    onEvent(CampaignSheetEvent.BackToForm)
                })
                .padding(vertical = 10.dp),
        color = AppPalette.TextMuted,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun TransferSearchRow(
    sheet: CampaignSheetState.Create,
    onEvent: (CampaignSheetEvent) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppSearchField(
            query = sheet.query,
            onQueryChange = { onEvent(CampaignSheetEvent.QueryChanged(it)) },
            placeholder = stringResource(Res.string.campaign_items_search),
            modifier = Modifier.weight(1f),
        )
        Text(
            text =
                stringResource(
                    if (sheet.allChecked) Res.string.campaign_items_none else Res.string.campaign_items_all,
                ),
            modifier =
                Modifier
                    .appCard(RoundedCornerShape(10.dp), Color.Transparent, null, onClick = {
                        onEvent(CampaignSheetEvent.AllToggled)
                    })
                    .padding(8.dp),
            color = AppPalette.GoldBright,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
        )
    }
}

@Composable
private fun TransferList(
    sheet: CampaignSheetState.Create,
    onEvent: (CampaignSheetEvent) -> Unit,
) {
    if (sheet.rows.isEmpty()) {
        Text(
            text = stringResource(Res.string.campaign_items_empty),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            color = AppPalette.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        Column(
            modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            sheet.rows.forEach { row -> TransferRow(row) { onEvent(CampaignSheetEvent.ItemToggled(row.id)) } }
        }
    }
}

@Composable
private fun TransferRow(
    row: CampaignTransferRow,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = RoundedCornerShape(12.dp),
                    background = if (row.checked) AppPalette.Gold.copy(alpha = 0.08f) else AppPalette.Background,
                    border = if (row.checked) AppPalette.Gold.copy(alpha = 0.4f) else AppPalette.BorderSubtle,
                    onClick = onClick,
                ).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelectCheckbox(row.checked)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                color = AppPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            if (row.subtitle.isNotEmpty()) {
                Text(
                    text = row.subtitle,
                    color = AppPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun CampaignNote(text: String) {
    Text(
        text = text,
        color = AppPalette.TextSecondary,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
    )
}
