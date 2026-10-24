package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import com.lyrismet.incadent.core.portrait.CropRect
import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.decodeImageBitmap
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.photo_kind_cover
import dndplayerscodex.shared.generated.resources.photo_kind_portrait
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/** the crop window of a picked photo, titled by its slot - the host cuts and saves what "Готово" reports */
@Composable
fun AppPhotoCropDraftWindow(
    draft: PhotoCropDraft,
    onConfirm: (CropRect) -> Unit,
    onCancel: () -> Unit,
) {
    // the picked photo is full size, so it decodes off the main thread and the cropper opens once it is ready
    val source by produceState<ImageBitmap?>(null, draft) {
        value = withContext(Dispatchers.Default) { decodeImageBitmap(draft.source) }
    }
    val titleRes = if (draft.slot == PhotoSlot.COVER) Res.string.photo_kind_cover else Res.string.photo_kind_portrait
    val decoded = source
    if (decoded == null) {
        AppPhotoCropLoading(onCancel)
    } else {
        AppPhotoCropper(
            source = decoded,
            aspect = draft.slot.aspect,
            title = stringResource(titleRes),
            onConfirm = onConfirm,
            onCancel = onCancel,
        )
    }
}
