package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.AppCoverAddStrip
import com.lyrismet.incadent.core.designsystem.component.AppCoverFrame
import com.lyrismet.incadent.core.designsystem.component.AppPhotoSlot
import com.lyrismet.incadent.core.designsystem.component.PhotoSlotActions
import com.lyrismet.incadent.core.designsystem.component.PortraitArch
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityEmblem
import com.lyrismet.incadent.feature.codex.presentation.summary.QuickEditSheet
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_cover_add
import dndplayerscodex.shared.generated.resources.photo_kind_cover
import dndplayerscodex.shared.generated.resources.photo_kind_portrait
import org.jetbrains.compose.resources.stringResource

/** quick mode's photo slot actions - pick, crop again and delete all go to the presenter as events */
@Composable
internal fun quickPhotoActions(
    quick: QuickEditSheet?,
    slot: PhotoSlot,
    photo: Photo?,
): PhotoSlotActions? =
    quick?.let {
        PhotoSlotActions(
            onPicked = { bytes -> it.onEvent(QuickEditUiEvent.PhotoPicked(slot, bytes)) },
            onCrop = {
                // the list thumbnail would be upscaled into the stored photo, so wait for the full picture
                photo?.full?.let { stored -> it.onEvent(QuickEditUiEvent.PhotoRecropRequested(slot, stored)) }
            },
            onDelete = { it.onEvent(QuickEditUiEvent.PhotoRemoved(slot)) },
        )
    }

/** the header's portrait arch, a tap opens the viewer or, in quick mode, the chooser of an empty one */
@Composable
internal fun EntityPortraitSlot(
    emblem: EntityEmblem,
    title: String,
    quick: QuickEditSheet?,
    showEditHint: Boolean,
) {
    AppPhotoSlot(
        photo = emblem.portrait,
        kind = stringResource(Res.string.photo_kind_portrait).uppercase(),
        title = title,
        monochrome = emblem.isMonochrome,
        actions = quickPhotoActions(quick, PhotoSlot.PORTRAIT, emblem.portrait),
        showEditHint = showEditHint,
    ) { onClick ->
        PortraitArch(
            portrait = emblem.portrait,
            mark = emblem.mark,
            color = emblem.color,
            monochrome = emblem.isMonochrome,
            onClick = onClick,
        )
    }
}

/** the place's cover on its card - the viewer on a tap, and in quick mode a dashed "add" strip while it is empty */
@Composable
internal fun EntityCoverSlot(
    cover: Photo?,
    title: String,
    quick: QuickEditSheet?,
    showEditHint: Boolean,
) {
    // a read-only card has nothing to show, and nothing to invite a tap to, while there is no cover
    if (cover == null && quick == null) return
    AppPhotoSlot(
        photo = cover,
        kind = stringResource(Res.string.photo_kind_cover).uppercase(),
        title = title,
        monochrome = false,
        actions = quickPhotoActions(quick, PhotoSlot.COVER, cover),
        showEditHint = showEditHint,
    ) { onClick ->
        if (cover != null) {
            AppCoverFrame(cover = cover, onClick = onClick)
        } else if (onClick != null) {
            AppCoverAddStrip(text = stringResource(Res.string.codex_cover_add), onClick = onClick)
        }
    }
}
