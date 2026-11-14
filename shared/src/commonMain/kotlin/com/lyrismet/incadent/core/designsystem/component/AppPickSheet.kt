package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.selectlist.SelectOption

/** a single-pick [AppSelectSheet] over a plain list, the pick comes back as the item itself, null when cleared */
@Composable
@Suppress("LongParameterList")
fun <T> AppPickSheet(
    items: List<T>,
    chosen: T?,
    title: String,
    emptyText: String,
    label: @Composable (T) -> String,
    onPick: (T?) -> Unit,
    onDismiss: () -> Unit,
    searchPlaceholder: String = "",
    clearLabel: String? = null,
    hint: @Composable (T) -> String? = { null },
    showSearch: Boolean = true,
    itemIcon: (@Composable (T) -> Unit)? = null,
) {
    val texts =
        AppSelectTexts(
            title = title,
            searchPlaceholder = searchPlaceholder,
            emptyText = emptyText,
            multiHint = "",
            clearLabel = clearLabel,
            doneLabel = { "" },
            createLabel = null,
        )
    AppSelectSheet(
        texts = texts,
        options = items.mapIndexed { index, item -> SelectOption(index + 1L, label(item), hint(item)) },
        selectedIds = setOfNotNull(items.indexOf(chosen).takeIf { it >= 0 }?.let { it + 1L }),
        multi = false,
        onConfirm = { ids -> onPick(ids.firstOrNull()?.let { items[it.toInt() - 1] }) },
        onDismiss = onDismiss,
        showSearch = showSearch,
        optionIcon = itemIcon?.let { icon -> { option -> icon(items[option.id.toInt() - 1]) } },
    )
}
