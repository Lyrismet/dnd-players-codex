package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDeleteLink
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppListRow
import com.lyrismet.incadent.core.designsystem.component.AppSectionTitleHint
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.FormChipOption
import com.lyrismet.incadent.core.designsystem.component.FormChipPicker
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.FormTextArea
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.CharacterFeat
import com.lyrismet.incadent.feature.codex.domain.model.FEAT_NAME_MAX
import com.lyrismet.incadent.feature.codex.domain.model.FeatSource
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_feats_add
import incadent.shared.generated.resources.me_feats_add_short
import incadent.shared.generated.resources.me_feats_delete
import incadent.shared.generated.resources.me_feats_gives_label
import incadent.shared.generated.resources.me_feats_gives_placeholder
import incadent.shared.generated.resources.me_feats_name_label
import incadent.shared.generated.resources.me_feats_name_placeholder
import incadent.shared.generated.resources.me_feats_save
import incadent.shared.generated.resources.me_feats_sheet_new
import incadent.shared.generated.resources.me_feats_source_background
import incadent.shared.generated.resources.me_feats_source_class
import incadent.shared.generated.resources.me_feats_source_feat
import incadent.shared.generated.resources.me_feats_source_label
import incadent.shared.generated.resources.me_feats_source_other
import incadent.shared.generated.resources.me_feats_source_race
import incadent.shared.generated.resources.me_feats_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** a feat the sheet is adding is told by id 0 */
internal val CharacterFeat.isNew: Boolean get() = id == 0L

internal val CharacterFeat.canSave: Boolean get() = name.isNotBlank()

sealed interface FeatEvent {
    data object AddClicked : FeatEvent

    data class Opened(
        val id: Long,
    ) : FeatEvent

    data class NameChanged(
        val name: String,
    ) : FeatEvent

    /** a press on the picked source again takes it off */
    data class SourcePicked(
        val source: FeatSource,
    ) : FeatEvent

    data class DescriptionChanged(
        val text: String,
    ) : FeatEvent

    data object Saved : FeatEvent

    data object Deleted : FeatEvent

    data object Dismissed : FeatEvent
}

private fun FeatSource.label(): StringResource =
    when (this) {
        FeatSource.FEAT -> Res.string.me_feats_source_feat
        FeatSource.RACE -> Res.string.me_feats_source_race
        FeatSource.CLASS -> Res.string.me_feats_source_class
        FeatSource.BACKGROUND -> Res.string.me_feats_source_background
        FeatSource.OTHER -> Res.string.me_feats_source_other
    }

/** the cards of the feats under the features, a press opens one, the last row adds a new one */
@Composable
internal fun FeatsBlock(
    feats: List<CharacterFeat>,
    readOnly: Boolean,
    onEvent: (FeatEvent) -> Unit,
) {
    if (feats.isEmpty() && readOnly) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSectionTitleHint(stringResource(Res.string.me_feats_title))
        Column(Modifier.fillMaxWidth().appCard().padding(start = 14.dp, end = 8.dp)) {
            feats.forEachIndexed { index, feat ->
                if (index > 0) AppDivider()
                FeatRow(feat, onClick = if (readOnly) null else ({ onEvent(FeatEvent.Opened(feat.id)) }))
            }
            if (!readOnly) {
                if (feats.isNotEmpty()) AppDivider()
                MeAddRow(stringResource(Res.string.me_feats_add)) { onEvent(FeatEvent.AddClicked) }
            }
        }
    }
}

@Composable
private fun FeatRow(
    feat: CharacterFeat,
    onClick: (() -> Unit)?,
) {
    val source = feat.source?.let { stringResource(it.label()) }
    AppListRow(
        title = feat.name,
        subtitle =
            listOfNotNull(
                source,
                feat.description.takeIf { it.isNotBlank() },
            ).joinToString(" · ").ifEmpty { null },
        onClick = onClick,
        minHeight = 56.dp,
        titleLineHeight = 20.sp,
        subtitleLineHeight = 16.sp,
    )
}

/** the one sheet that adds a feat and edits it, the delete link shows for a feat that already exists */
@Composable
internal fun FeatSheet(
    draft: CharacterFeat,
    onEvent: (FeatEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { onEvent(FeatEvent.Dismissed) }) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppSheetHeader(
                title = draft.name.ifBlank { stringResource(Res.string.me_feats_sheet_new) },
                onClose = { onEvent(FeatEvent.Dismissed) },
                overline = stringResource(Res.string.me_feats_title).uppercase(),
            )
            FormTextField(
                label = stringResource(Res.string.me_feats_name_label),
                value = draft.name,
                onChange = { onEvent(FeatEvent.NameChanged(it.take(FEAT_NAME_MAX))) },
                placeholder = stringResource(Res.string.me_feats_name_placeholder),
                required = true,
            )
            FormChipPicker(
                label = stringResource(Res.string.me_feats_source_label),
                options =
                    FeatSource.entries.map {
                        FormChipOption(it, stringResource(it.label()), selected = it == draft.source)
                    },
                onClick = { onEvent(FeatEvent.SourcePicked(it)) },
            )
            FormTextArea(
                label = stringResource(Res.string.me_feats_gives_label),
                value = draft.description,
                onChange = { onEvent(FeatEvent.DescriptionChanged(it)) },
                placeholder = stringResource(Res.string.me_feats_gives_placeholder),
            )
            if (!draft.isNew) {
                AppDeleteLink(stringResource(Res.string.me_feats_delete), onClick = { onEvent(FeatEvent.Deleted) })
            }
            FormPrimaryButton(
                text = stringResource(if (draft.isNew) Res.string.me_feats_add_short else Res.string.me_feats_save),
                onClick = { onEvent(FeatEvent.Saved) },
                enabled = draft.canSave,
            )
        }
    }
}
