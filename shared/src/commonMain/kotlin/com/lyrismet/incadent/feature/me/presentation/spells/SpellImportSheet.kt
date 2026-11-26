package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppNoticeBar
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormFieldHeight
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.HoldHintCard
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.StatusKind
import com.lyrismet.incadent.core.designsystem.component.StatusMark
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.feature.codex.domain.repository.SpellImportFailure
import com.lyrismet.incadent.feature.codex.domain.spellimport.SpellSource
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_edit_overline
import incadent.shared.generated.resources.me_spells_import_button
import incadent.shared.generated.resources.me_spells_import_clear
import incadent.shared.generated.resources.me_spells_import_device_note
import incadent.shared.generated.resources.me_spells_import_error_not_found
import incadent.shared.generated.resources.me_spells_import_error_offline
import incadent.shared.generated.resources.me_spells_import_error_unreadable
import incadent.shared.generated.resources.me_spells_import_error_unsupported
import incadent.shared.generated.resources.me_spells_import_hint
import incadent.shared.generated.resources.me_spells_import_hint_dice
import incadent.shared.generated.resources.me_spells_import_hint_dismiss
import incadent.shared.generated.resources.me_spells_import_language_en
import incadent.shared.generated.resources.me_spells_import_link
import incadent.shared.generated.resources.me_spells_import_link_hint
import incadent.shared.generated.resources.me_spells_import_manual
import incadent.shared.generated.resources.me_spells_import_paste
import incadent.shared.generated.resources.me_spells_import_sites
import incadent.shared.generated.resources.me_spells_import_submit
import incadent.shared.generated.resources.me_spells_import_title
import org.jetbrains.compose.resources.stringResource

private const val LINK_PATH_A = "M10 13a5 5 0 0 0 7.5.5l3-3a5 5 0 0 0-7-7l-1.7 1.7"
private const val LINK_PATH_B = "M14 11a5 5 0 0 0-7.5-.5l-3 3a5 5 0 0 0 7 7l1.7-1.7"

private val LinkIcon: ImageVector =
    ImageVector
        .Builder(
            name = "SpellLink",
            defaultWidth = 18.dp,
            defaultHeight = 18.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            listOf(LINK_PATH_A, LINK_PATH_B).forEach { path ->
                addPath(
                    pathData = addPathNodes(path),
                    stroke = SolidColor(AppPalette.Gold),
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

/** the round button next to the close one of the form of a new spell, it opens the sheet that takes a link */
@Composable
internal fun SpellImportButton(onClick: () -> Unit) {
    val label = stringResource(Res.string.me_spells_import_button)
    IconBadge(
        modifier = Modifier.semantics { contentDescription = label },
        size = 40.dp,
        background = AppPalette.BorderSubtle,
        onClick = onClick,
    ) {
        Icon(LinkIcon, contentDescription = null, tint = Color.Unspecified)
    }
}

/** the line over the form of an imported spell that says what the import did not fill and names the dice it saw */
@Composable
internal fun SpellImportHint(
    hint: ImportHint,
    onDismiss: () -> Unit,
) {
    val base = stringResource(Res.string.me_spells_import_hint, hint.site)
    val text =
        if (hint.dice.isEmpty()) {
            base
        } else {
            "$base ${stringResource(Res.string.me_spells_import_hint_dice, hint.dice.joinToString(", "))}"
        }
    HoldHintCard(
        text = text,
        dismissLabel = stringResource(Res.string.me_spells_import_hint_dismiss),
        onDismiss = onDismiss,
    )
}

/** the sheet over the form that takes the link, shows the sites that fit and says why a try failed */
@Composable
internal fun SpellImportSheet(
    sheet: SpellImportSheetState,
    send: (SpellsEvent) -> Unit,
) {
    val close = { send(SpellsEvent.ImportClosed) }
    val loading = sheet.phase is ImportPhase.Loading
    AppBottomSheet(onDismissRequest = close) {
        AppSheetColumn(
            scroll = true,
            header = {
                AppSheetHeader(
                    title = stringResource(Res.string.me_spells_import_title),
                    onClose = close,
                    overline = stringResource(Res.string.me_spells_edit_overline),
                )
            },
        ) {
            LinkRow(sheet, send)
            (sheet.phase as? ImportPhase.Failed)?.let { ImportError(it.failure, close) }
            SupportedSites()
            Text(
                stringResource(Res.string.me_spells_import_device_note),
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextMuted,
            )
            AppButton(
                text = stringResource(Res.string.me_spells_import_submit),
                onClick = { send(SpellsEvent.ImportSubmitted) },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Solid,
                size = ButtonSize.Primary,
                enabled = sheet.link.isNotBlank() && !loading,
                leading = if (loading) ({ StatusMark(StatusKind.Working, size = 18.dp) }) else null,
            )
        }
    }
}

/** the field of the link with "Вставить" taking it from the clipboard and a cross that clears it */
@Composable
private fun LinkRow(
    sheet: SpellImportSheetState,
    send: (SpellsEvent) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val loading = sheet.phase is ImportPhase.Loading
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        FormTextField(
            label = stringResource(Res.string.me_spells_import_link),
            value = sheet.link,
            onChange = { send(SpellsEvent.ImportLinkChanged(it)) },
            modifier = Modifier.weight(1f),
            placeholder = stringResource(Res.string.me_spells_import_link_hint),
            maxLength = LINK_LIMIT,
        )
        val empty = sheet.link.isEmpty()
        AppButton(
            text = stringResource(if (empty) Res.string.me_spells_import_paste else Res.string.me_spells_import_clear),
            onClick = {
                val link =
                    if (empty) {
                        clipboard
                            .getText()
                            ?.text
                            .orEmpty()
                            .trim()
                    } else {
                        ""
                    }
                send(SpellsEvent.ImportLinkChanged(link))
            },
            height = FormFieldHeight,
            tone = ButtonTone.Neutral,
            enabled = !loading,
        )
    }
}

@Composable
private fun ImportError(
    failure: SpellImportFailure,
    onManual: () -> Unit,
) {
    val text =
        stringResource(
            when (failure) {
                SpellImportFailure.UNSUPPORTED_SITE -> Res.string.me_spells_import_error_unsupported
                SpellImportFailure.OFFLINE -> Res.string.me_spells_import_error_offline
                SpellImportFailure.NOT_FOUND -> Res.string.me_spells_import_error_not_found
                SpellImportFailure.UNREADABLE -> Res.string.me_spells_import_error_unreadable
            },
        )
    AppNoticeBar(AppPalette.Maroon) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = AppPalette.MaroonBright,
        )
        if (failure == SpellImportFailure.UNREADABLE) {
            AppButton(
                text = stringResource(Res.string.me_spells_import_manual),
                onClick = onManual,
                tone = ButtonTone.Neutral,
            )
        }
    }
}

@Composable
private fun SupportedSites() {
    val englishMark = stringResource(Res.string.me_spells_import_language_en)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormFieldLabel(stringResource(Res.string.me_spells_import_sites))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SpellSource.entries.forEach { source ->
                val english = source.english
                TagChip(
                    text = if (english) "${source.siteName} · $englishMark" else source.siteName,
                )
            }
        }
    }
}

private const val LINK_LIMIT = 300
