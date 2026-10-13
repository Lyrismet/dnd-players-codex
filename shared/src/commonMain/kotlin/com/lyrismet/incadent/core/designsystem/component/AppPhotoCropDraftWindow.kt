package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.portrait.CropRect
import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.decodeImageBitmap
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.photo_kind_cover
import dndplayerscodex.shared.generated.resources.photo_kind_portrait
import org.jetbrains.compose.resources.stringResource

/** the crop window of a picked photo, titled by its slot - the host cuts and saves what "Готово" reports */
@Composable
fun AppPhotoCropDraftWindow(
    draft: PhotoCropDraft,
    onConfirm: (CropRect) -> Unit,
    onCancel: () -> Unit,
) {
    val source = remember(draft) { decodeImageBitmap(draft.source) }
    val titleRes = if (draft.slot == PhotoSlot.COVER) Res.string.photo_kind_cover else Res.string.photo_kind_portrait
    AppPhotoCropper(
        source = source,
        aspect = draft.slot.aspect,
        title = stringResource(titleRes),
        onConfirm = onConfirm,
        onCancel = onCancel,
    )
}
