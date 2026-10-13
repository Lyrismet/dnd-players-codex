package com.lyrismet.incadent.presentation.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppCoverField
import com.lyrismet.incadent.core.designsystem.component.AppPhotoCropDraftWindow
import com.lyrismet.incadent.core.designsystem.component.AppPhotoSlot
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.PhotoSlotActions
import com.lyrismet.incadent.core.designsystem.component.PortraitFrame
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.photoSizeKb
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_cover_form_hint
import dndplayerscodex.shared.generated.resources.codex_cover_form_placeholder
import dndplayerscodex.shared.generated.resources.codex_cover_remove
import dndplayerscodex.shared.generated.resources.codex_entry_label_cover
import dndplayerscodex.shared.generated.resources.codex_entry_label_portrait
import dndplayerscodex.shared.generated.resources.codex_entry_portrait_hint_empty
import dndplayerscodex.shared.generated.resources.codex_entry_portrait_hint_filled
import dndplayerscodex.shared.generated.resources.codex_entry_portrait_remove
import dndplayerscodex.shared.generated.resources.photo_crop_again
import dndplayerscodex.shared.generated.resources.photo_kind_cover
import dndplayerscodex.shared.generated.resources.photo_kind_portrait
import org.jetbrains.compose.resources.stringResource

private val PortraitCardModifier =
    Modifier.fillMaxWidth().appCard(RoundedCornerShape(14.dp), AppPalette.Background, AppPalette.Border).padding(12.dp)

private fun photoActions(
    slot: PhotoSlot,
    eventSink: (CodexEntryFormEvent) -> Unit,
) = PhotoSlotActions(
    onPicked = { eventSink(CodexEvent.EntryPhotoPicked(slot, it)) },
    onCrop = { eventSink(CodexEvent.EntryPhotoRecropRequested(slot)) },
    onDelete = { eventSink(CodexEvent.EntryPhotoRemoved(slot)) },
)

/** the arch portrait slot shared by npc and party forms, see Players Codex v6.dc.html's form `photo()` */
@Composable
internal fun PortraitField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionOverline(
            text = stringResource(Res.string.codex_entry_label_portrait),
            color = AppPalette.TextTertiary,
            fontSize = 10.sp,
            letterSpacing = 0.14.em,
        )
        Row(
            modifier = PortraitCardModifier,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val portrait = form.portrait
            AppPhotoSlot(
                photo = portrait,
                kind = stringResource(Res.string.photo_kind_portrait).uppercase(),
                title = form.name,
                monochrome = false,
                actions = photoActions(PhotoSlot.PORTRAIT, eventSink),
                showEditHint = false,
                // the form's frame always re-picks, crop and remove are the buttons beside it
                viewerOnTap = false,
            ) { onClick ->
                PortraitFrame(portrait = portrait, onClick = { onClick?.invoke() })
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val hint =
                    if (portrait != null) {
                        stringResource(Res.string.codex_entry_portrait_hint_filled, photoSizeKb(portrait.display))
                    } else {
                        stringResource(Res.string.codex_entry_portrait_hint_empty)
                    }
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                    color = AppPalette.TextMuted,
                )
                if (portrait != null) {
                    PhotoFieldButtons(
                        removeText = stringResource(Res.string.codex_entry_portrait_remove),
                        onCrop = { eventSink(CodexEvent.EntryPhotoRecropRequested(PhotoSlot.PORTRAIT)) },
                        onRemove = { eventSink(CodexEvent.EntryPhotoRemoved(PhotoSlot.PORTRAIT)) },
                    )
                }
            }
        }
    }
}

/** the wide place cover of the location form, see the mockup's `f.isCover` - a 150dp box, crop and remove below */
@Composable
internal fun CoverField(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    val cover = form.cover
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionOverline(
            text = stringResource(Res.string.codex_entry_label_cover),
            color = AppPalette.TextTertiary,
            fontSize = 10.sp,
            letterSpacing = 0.14.em,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppPhotoSlot(
                photo = cover,
                kind = stringResource(Res.string.photo_kind_cover).uppercase(),
                title = form.name,
                monochrome = false,
                actions = photoActions(PhotoSlot.COVER, eventSink),
                showEditHint = false,
                viewerOnTap = false,
            ) { onClick ->
                AppCoverField(
                    cover = cover,
                    placeholder = stringResource(Res.string.codex_cover_form_placeholder),
                    hint = stringResource(Res.string.codex_cover_form_hint),
                    onClick = { onClick?.invoke() },
                )
            }
            if (cover != null) {
                PhotoFieldButtons(
                    removeText = stringResource(Res.string.codex_cover_remove),
                    onCrop = { eventSink(CodexEvent.EntryPhotoRecropRequested(PhotoSlot.COVER)) },
                    onRemove = { eventSink(CodexEvent.EntryPhotoRemoved(PhotoSlot.COVER)) },
                )
            }
        }
    }
}

@Composable
private fun PhotoFieldButtons(
    removeText: String,
    onCrop: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HeaderActionButton(
            text = stringResource(Res.string.photo_crop_again),
            onClick = onCrop,
            modifier = Modifier.height(36.dp),
            foreground = AppPalette.GoldBright,
            background = Color.Transparent,
            border = AppPalette.Gold.copy(alpha = 0.45f),
        )
        HeaderActionButton(
            text = removeText,
            onClick = onRemove,
            modifier = Modifier.height(36.dp),
            foreground = AppPalette.MaroonBright,
            background = Color.Transparent,
            border = AppPalette.Maroon.copy(alpha = 0.6f),
        )
    }
}

/** the crop window of the photo picked in this form - drawn over the sheet until "Готово" or "Отмена" */
@Composable
internal fun FormPhotoCropWindow(
    form: CodexEntryFormState,
    eventSink: (CodexEntryFormEvent) -> Unit,
) {
    val draft = form.photoCrop ?: return
    AppPhotoCropDraftWindow(
        draft = draft,
        onConfirm = { eventSink(CodexEvent.EntryPhotoCropConfirmed(it)) },
        onCancel = { eventSink(CodexEvent.EntryPhotoCropCancelled) },
    )
}
