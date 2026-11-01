package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.debounce.rememberDebounced
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.designsystem.component.icons.entity.AppEntityIcon
import com.lyrismet.incadent.core.selectlist.SelectOption
import com.lyrismet.incadent.core.selectlist.SelectRow
import com.lyrismet.incadent.core.selectlist.canCreateFromQuery
import com.lyrismet.incadent.core.selectlist.pickedIds
import com.lyrismet.incadent.core.selectlist.selectRows

private val ListMaxHeight = 380.dp
private val SearchHeight = 46.dp
private val RowMinHeight = 58.dp
private val RowContentGap = 16.dp
private val EditButtonSize = 44.dp
private val ListShape = AppShapes.Card
private const val INACTIVE_ALPHA = 0.45f

// the muted gold of a note saying why an option is not pickable
private val NoteColor = Color(0xFFB5A887)

/** the words a host gives [AppSelectSheet] - everything the sheet shows besides the options themselves */
@Suppress("LongParameterList")
class AppSelectTexts(
    val title: String,
    val searchPlaceholder: String,
    val emptyText: String,
    /** shown instead of [emptyText] while the list holds nothing and nothing is typed, says a name creates one */
    val emptyHint: String? = null,
    /** the quiet "can pick several" note next to the title, multi mode */
    val multiHint: String,
    /** single mode only - the row that clears the value, null hides it */
    val clearLabel: String?,
    val doneLabel: @Composable (count: Int) -> String,
    /** the grey note of an option carried along by a checked one, given that one's name - null when nothing is */
    val carriedLabel: (@Composable (name: String) -> String)? = null,
    /** null hides the "create «query»" row */
    val createLabel: (@Composable (query: String) -> String)?,
)

/**
 * a search field over a list to pick from, in a bottom sheet - one value (a tap answers) or several (checks and a
 * "done" button). Generic on purpose: any list of [SelectOption]s fits, the host owns what the ids mean.
 * [onConfirm] gets the final ids, empty when the value is cleared. [onCreate] gets the typed query and the ids
 * checked so far, and is expected to close the sheet.
 */
@Composable
fun AppSelectSheet(
    texts: AppSelectTexts,
    options: List<SelectOption>,
    selectedIds: Set<Long>,
    multi: Boolean,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    recentIds: List<Long> = emptyList(),
    excludedIds: Set<Long> = emptySet(),
    leadingIcon: ImageVector? = null,
    onCreate: ((query: String, checked: Set<Long>) -> Unit)? = null,
    onEdit: ((Long) -> Unit)? = null,
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        AppSelectSheetContent(
            texts = texts,
            options = options,
            selectedIds = selectedIds,
            multi = multi,
            onConfirm = onConfirm,
            modifier = modifier,
            recentIds = recentIds,
            excludedIds = excludedIds,
            leadingIcon = leadingIcon,
            onCreate = onCreate,
            onEdit = onEdit,
        )
    }
}

/** the body of [AppSelectSheet] without its own sheet, for a host that swaps it into a sheet it already shows */
@Composable
@Suppress("LongParameterList")
fun AppSelectSheetContent(
    texts: AppSelectTexts,
    options: List<SelectOption>,
    selectedIds: Set<Long>,
    multi: Boolean,
    onConfirm: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier,
    recentIds: List<Long> = emptyList(),
    excludedIds: Set<Long> = emptySet(),
    leadingIcon: ImageVector? = null,
    onCreate: ((query: String, checked: Set<Long>) -> Unit)? = null,
    onEdit: ((Long) -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    var checked by remember { mutableStateOf(selectedIds) }
    val searchQuery = rememberDebounced(query) { it.isEmpty() }
    val rows =
        remember(options, searchQuery, checked, recentIds, excludedIds, multi) {
            selectRows(options, searchQuery, checked, recentIds, excludedIds, floatSelected = multi)
        }
    val picked = remember(options, checked) { pickedIds(options, checked) }
    val canCreate = onCreate != null && texts.createLabel != null && canCreateFromQuery(options, query)
    val canClear = !multi && texts.clearLabel != null && selectedIds.isNotEmpty() && query.isBlank()
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelectHeader(texts, multi)
            AppSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = texts.searchPlaceholder,
                height = SearchHeight,
                textSize = AppFontSize.Large,
                background = AppPalette.Background,
                clearEndInset = 6.dp,
            )
        }
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (canClear && texts.clearLabel != null) SelectOutlinedButton(texts.clearLabel) { onConfirm(emptySet()) }
            if (rows.isEmpty()) {
                SelectEmptyText(texts, isListEmpty = options.isEmpty() && query.isBlank(), canCreate = onCreate != null)
            } else {
                SelectRows(
                    rows = rows,
                    multi = multi,
                    leadingIcon = leadingIcon,
                    carriedLabel = texts.carriedLabel,
                    onEdit = onEdit,
                    onRowClick = { id ->
                        if (multi) checked = if (id in checked) checked - id else checked + id else onConfirm(setOf(id))
                    },
                )
            }
            if (canCreate) {
                SelectCreateRow(texts.createLabel.invoke(query.trim())) { onCreate.invoke(query.trim(), checked) }
            }
        }
        SelectFooter(multi, texts.doneLabel(picked.size)) { onConfirm(picked) }
    }
}

// the done button of a multi pick sits under a rule, a single pick just leaves some air below the list
@Composable
private fun SelectFooter(
    multi: Boolean,
    doneLabel: String,
    onDone: () -> Unit,
) {
    if (multi) {
        AppDivider(color = AppPalette.BorderSubtle)
        Box(Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 28.dp)) {
            FormPrimaryButton(text = doneLabel, onClick = onDone, height = 50.dp)
        }
    } else {
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SelectHeader(
    texts: AppSelectTexts,
    multi: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            texts.title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = AppFontSize.Section),
            color = AppPalette.TextHeading,
            modifier = Modifier.weight(1f),
        )
        if (multi) Text(texts.multiHint, fontSize = AppFontSize.Small, color = AppPalette.TextSecondary)
    }
}

@Composable
private fun SelectRows(
    rows: List<SelectRow>,
    multi: Boolean,
    leadingIcon: ImageVector?,
    carriedLabel: (@Composable (name: String) -> String)?,
    onEdit: ((Long) -> Unit)?,
    onRowClick: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = ListMaxHeight)
                .clip(ListShape)
                .background(AppPalette.Background)
                .border(1.dp, AppPalette.BorderSubtle, ListShape)
                .scrollEdgeFade(listState),
        contentPadding = PaddingValues(horizontal = RowContentGap),
    ) {
        itemsIndexed(rows, key = { _, row -> row.option.id }) { index, row ->
            val group = row.option.group
            if (group != null &&
                group != rows.getOrNull(index - 1)?.option?.group
            ) {
                SelectGroupHeader(group, isFirst = index == 0)
            } else if (index > 0) {
                AppDivider(color = AppPalette.SurfacePopover)
            }
            SelectListRow(row, multi, leadingIcon, carriedLabel, onEdit, onClick = { onRowClick(row.option.id) })
        }
    }
}

@Composable
private fun SelectListRow(
    row: SelectRow,
    multi: Boolean,
    leadingIcon: ImageVector?,
    carriedLabel: (@Composable (name: String) -> String)?,
    onEdit: ((Long) -> Unit)?,
    onClick: () -> Unit,
) {
    val note = row.option.unavailableNote ?: row.carriedBy?.let { name -> carriedLabel?.invoke(name) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = !row.isInactive, onClick = onClick)
                .alpha(if (row.isInactive) INACTIVE_ALPHA else 1f)
                .heightIn(min = RowMinHeight)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RowContentGap),
    ) {
        if (multi) AppCheckBox(row.selected, carried = row.carriedBy != null)
        if (!multi &&
            leadingIcon != null
        ) {
            AppEntityIcon(leadingIcon, AppPalette.Parchment, size = 14.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            // explicit line heights - without them the theme's tall default spreads the title and the path apart
            Text(
                highlighted(row),
                fontSize = AppFontSize.Large,
                lineHeight = 20.sp,
                color = AppPalette.TextPrimary,
            )
            row.option.subtitle?.let { path ->
                Text(
                    path,
                    fontSize = AppFontSize.CaptionPlus,
                    lineHeight = 16.sp,
                    color = AppPalette.TextSecondary,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            note?.let { reason ->
                Text(
                    reason,
                    fontSize = AppFontSize.CaptionPlus,
                    lineHeight = 16.sp,
                    color = NoteColor,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        if (!multi && row.selected) Text("✓", color = AppPalette.GoldBright, fontWeight = FontWeight.Bold)
        if (onEdit != null && row.option.editable) SelectEditButton { onEdit(row.option.id) }
    }
}

@Composable
private fun SelectEditButton(onClick: () -> Unit) {
    IconBadge(size = EditButtonSize, shape = AppShapes.Control, background = Color.Transparent, onClick = onClick) {
        Icon(AppIcons.Edit, contentDescription = null, tint = AppPalette.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

private fun highlighted(row: SelectRow) =
    buildAnnotatedString {
        val title = row.option.title
        val match = row.match
        if (match == null) {
            append(title)
        } else {
            append(title.substring(0, match.first))
            withStyle(SpanStyle(color = AppPalette.GoldBright, fontWeight = FontWeight.SemiBold)) {
                append(title.substring(match.first, match.last + 1))
            }
            append(title.substring(match.last + 1))
        }
    }

@Composable
private fun SelectOutlinedButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .appCard(
                    shape = AppShapes.Field,
                    background = Color.Transparent,
                    border = AppPalette.Border,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = AppFontSize.BodyPlus, color = AppPalette.TextMuted)
    }
}

@Composable
internal fun SelectCreateRow(
    text: String,
    onClick: () -> Unit,
) {
    AppDashedTile(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = AppPalette.Gold.copy(alpha = 0.6f),
        cornerRadius = 14.dp,
        minHeight = 50.dp,
    ) {
        Text(text, fontSize = AppFontSize.Medium, fontWeight = FontWeight.SemiBold, color = AppPalette.GoldBright)
    }
}
