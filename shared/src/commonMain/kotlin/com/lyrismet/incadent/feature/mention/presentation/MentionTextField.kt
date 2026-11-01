package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppCharCountPill
import com.lyrismet.incadent.core.designsystem.component.AppPopoverPanel
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputCounter
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.TextLimits
import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.insertMentionAt
import com.lyrismet.incadent.feature.mention.domain.mentionToken
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** a notes input where "@" offers the records of [source] under it, the record written in ([self]) left out */
@Composable
fun MentionTextField(
    text: String,
    onTextChange: (String) -> Unit,
    source: MentionSource,
    self: MentionRef?,
    modifier: Modifier = Modifier,
    state: MentionFieldState = rememberMentionFieldState(text),
    // the input takes the height left over, the dropdown stays under it
    fillHeight: Boolean = false,
    placeholder: String? = null,
    style: AppTextInputStyle = AppTextInputStyle(),
    minLines: Int = 1,
    maxLength: Int = TextLimits.NOTE,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
    autoFocus: Boolean = false,
    inputModifier: Modifier = Modifier,
) {
    val value = state.value
    val creating = remember { mutableStateOf<MentionCreateSheetState?>(null) }
    val scope = rememberCoroutineScope()
    val offered = remember(source.entities, self) { source.entities.filter { it.ref != self } }
    val index = remember(offered) { MentionIndex(offered) }
    val rows =
        rememberMentionDropdown(
            offered,
            index,
            source.lookups.nicknames,
            source.lookups.details,
            MentionSessionBodies(emptyList(), emptyList()),
            value.text,
            value.selection.end,
        )

    fun change(next: TextFieldValue) {
        state.value = next
        onTextChange(next.text)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MentionInput(
            value = value,
            onChange = { typed ->
                change(cleanedEdit(takeMentionDisplay(typed.text, maxLength), typed.selection, typed.composition))
            },
            fillHeight = fillHeight,
            placeholder = placeholder,
            style = style,
            minLines = minLines,
            maxLength = maxLength,
            keyboardActions = keyboardActions,
            focusRequester = focusRequester,
            autoFocus = autoFocus,
            modifier = inputModifier.then(if (fillHeight) Modifier.weight(1f) else Modifier),
        )
        if (rows.isNotEmpty()) {
            MentionDropdownPanel(
                rows,
                onPick = { token ->
                    val inserted = insertMentionAt(value.text, token, value.selection.end)
                    change(TextFieldValue(inserted.text, TextRange(inserted.caret)))
                },
                onCreate = {
                    creating.value =
                        MentionCreateSheetState(it.trim(), EntityKind.NPC, MentionCreateKinds, null)
                },
            )
        }
        // an input that fills the screen counts under the dropdown, a short one inside its own box
        if (fillHeight) {
            AppCharCountPill(mentionDisplayLength(value.text), maxLength, Modifier.align(Alignment.End))
        }
    }
    CreateSheetHost(creating) { sheet ->
        // the record is made even if the screen is left meanwhile, so it never ends up without its token
        scope.launch { withContext(NonCancellable) { insertCreated(source, sheet) { change(it(state.value)) } } }
    }
}

@Composable
private fun CreateSheetHost(
    creating: MutableState<MentionCreateSheetState?>,
    onConfirm: (MentionCreateSheetState) -> Unit,
) {
    val sheet = creating.value ?: return
    MentionCreateSheet(
        sheet,
        MentionCreateActions(
            onNameChanged = { creating.value = creating.value?.copy(name = it) },
            onTypeChanged = { creating.value = creating.value?.copy(type = it) },
            onConfirm = {
                creating.value = null
                onConfirm(sheet)
            },
            onDismiss = { creating.value = null },
        ),
    )
}

// makes the record the sheet describes and hands back the text with its token put in at the caret
private suspend fun insertCreated(
    source: MentionSource,
    sheet: MentionCreateSheetState,
    apply: ((TextFieldValue) -> TextFieldValue) -> Unit,
) {
    val name = sheet.name.trim().capitalizeFirst()
    val ref = source.create(sheet.type, name)
    apply { current ->
        val inserted = insertMentionAt(current.text, mentionToken(ref, name), current.selection.end)
        TextFieldValue(inserted.text, TextRange(inserted.caret))
    }
}

// the dropdown rows scroll and the create row stays in view under them
@Composable
internal fun MentionDropdownPanel(
    rows: List<MentionDropdownItem>,
    onPick: (String) -> Unit,
    onCreate: (String) -> Unit,
) {
    val (list, pinned) = rows.withPinnedCreate()
    AppPopoverPanel(
        modifier = Modifier.fillMaxWidth(),
        footer = pinned?.let { { MentionDropdownContent(listOf(it), onPick, onCreate) } },
    ) {
        MentionDropdownContent(list, onPick, onCreate)
    }
}

// a short input grows with its text, one that fills the screen scrolls so the caret stays above the dropdown
@Composable
private fun MentionInput(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    fillHeight: Boolean,
    placeholder: String?,
    style: AppTextInputStyle,
    minLines: Int,
    maxLength: Int,
    keyboardActions: KeyboardActions,
    focusRequester: FocusRequester?,
    autoFocus: Boolean,
    modifier: Modifier,
) {
    val transformation = remember { MentionVisualTransformation(SpanStyle(color = AppPalette.GoldBright)) }
    val scroll = rememberScrollState()
    var viewportPx by remember { mutableIntStateOf(0) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    if (fillHeight) KeepCaretInView(value, layout, transformation, scroll, viewportPx)
    Box(
        modifier =
            if (fillHeight) {
                modifier.fillMaxWidth().onSizeChanged { viewportPx = it.height }.verticalScroll(scroll)
            } else {
                modifier.fillMaxWidth()
            },
    ) {
        AppTextInput(
            value = value,
            onValueChange = onChange,
            placeholder = placeholder,
            style = style,
            singleLine = false,
            minLines = minLines,
            counter =
                if (fillHeight) {
                    null
                } else {
                    AppTextInputCounter(
                        maxLength,
                        count = mentionDisplayLength(value.text),
                    )
                },
            keyboardActions = keyboardActions,
            visualTransformation = transformation,
            focusRequester = focusRequester,
            autoFocus = autoFocus,
            onTextLayout = { layout = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (fillHeight) {
                            Modifier.heightIn(
                                min = with(LocalDensity.current) { viewportPx.toDp() },
                            )
                        } else {
                            Modifier
                        },
                    ),
        )
    }
}

// scrolls the input so the caret is not under the dropdown that took the space below it
@Composable
private fun KeepCaretInView(
    value: TextFieldValue,
    layout: TextLayoutResult?,
    transformation: MentionVisualTransformation,
    scroll: ScrollState,
    viewportPx: Int,
) {
    LaunchedEffect(layout, value.selection.end, viewportPx) {
        val shown = layout ?: return@LaunchedEffect
        val mapping = transformation.filter(AnnotatedString(value.text)).offsetMapping
        val caret = mapping.originalToTransformed(value.selection.end)
        val rect = shown.getCursorRect(caret.coerceIn(0, shown.layoutInput.text.length))
        when {
            rect.bottom > scroll.value + viewportPx -> scroll.scrollTo((rect.bottom - viewportPx).roundToInt())
            rect.top < scroll.value -> scroll.scrollTo(rect.top.roundToInt())
        }
    }
}
