package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.entitysummary.EntityEmblem
import com.lyrismet.incadent.core.entitysummary.QuickEditSheet
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.domain.model.Photo
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
                photo?.display?.let { stored -> it.onEvent(QuickEditUiEvent.PhotoRecropRequested(slot, stored)) }
            },
            onDelete = { it.onEvent(QuickEditUiEvent.PhotoRemoved(slot)) },
        )
    }

/**
 * the header's portrait arch - a tap opens the full-screen viewer of a stored portrait, and in quick mode an empty
 * one opens the camera/gallery chooser. [showEditHint] tells a read-only viewer where the form edit lives.
 */
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
            emblemText = emblem.text,
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
