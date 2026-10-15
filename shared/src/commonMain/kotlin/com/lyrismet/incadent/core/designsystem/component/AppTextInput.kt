package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.GoldCursorBrush
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions

private val DefaultInputShape = AppShapes.Field
private val DefaultInputPadding = PaddingValues(horizontal = 14.dp)

/** the card an input is drawn in */
class AppInputChrome(
    val shape: Shape = DefaultInputShape,
    val background: Color = AppPalette.Background,
    val border: Color? = AppPalette.Border,
)

/** how the input looks, apart from what it edits - one value so the two overloads share their parameters */
class AppTextInputStyle(
    val textStyle: TextStyle? = null,
    // Color.Unspecified keeps the color the text style already carries
    val textColor: Color = AppPalette.TextPrimary,
    val placeholderColor: Color = AppPalette.TextTertiary,
    // null draws no card at all, for an input that sits inside a frame of its own
    val chrome: AppInputChrome? = AppInputChrome(),
    // an exact height, null lets a multi-line input grow with its lines
    val height: Dp? = null,
    val contentPadding: PaddingValues = DefaultInputPadding,
)

/** a counter pill inside the input, "12/32" - at the end of a line, at the bottom end of an area */
class AppTextInputCounter(
    val max: Int,
    // what the pill counts when that is not the raw length, e.g. a note whose mention tokens show shorter
    val count: Int? = null,
)

/**
 * the project's one text field: a [BasicTextField] with the design's card, a stacked placeholder (so the caret sits at
 * its start), the gold caret and sentence capitalization. [maxLength] caps what is typed, [counter] shows the pill,
 * [leading] and [trailing] sit before and after the text. Material3's TextField is not used - its 56dp minimum
 * height and 16dp inset don't match the design.
 */
@Composable
fun AppTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    style: AppTextInputStyle = AppTextInputStyle(),
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int? = null,
    counter: AppTextInputCounter? = null,
    keyboardOptions: KeyboardOptions = SentenceKeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusRequester: FocusRequester? = null,
    autoFocus: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val requester = inputFocusRequester(focusRequester, autoFocus)
    val resolvedStyle = style.resolvedTextStyle()
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(maxLength?.let(it::take) ?: it) },
        singleLine = singleLine,
        minLines = minLines,
        textStyle = resolvedStyle,
        cursorBrush = GoldCursorBrush,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        modifier = modifier.inputChrome(style, requester),
        decorationBox = { inner ->
            InputDecoration(
                isEmpty = value.isEmpty(),
                placeholder = placeholder,
                style = style,
                textStyle = resolvedStyle,
                singleLine = singleLine,
                counter = counter?.let { (it.count ?: value.length) to it.max },
                leading = leading,
                trailing = trailing,
                inner = inner,
            )
        },
    )
}

/** the same input for a caller that keeps the selection itself, so a cursor placed in code survives recomposition */
@Composable
fun AppTextInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    style: AppTextInputStyle = AppTextInputStyle(),
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int? = null,
    counter: AppTextInputCounter? = null,
    keyboardOptions: KeyboardOptions = SentenceKeyboardOptions,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusRequester: FocusRequester? = null,
    autoFocus: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val requester = inputFocusRequester(focusRequester, autoFocus)
    val resolvedStyle = style.resolvedTextStyle()
    BasicTextField(
        value = value,
        onValueChange = { typed -> onValueChange(maxLength?.let { typed.copy(text = typed.text.take(it)) } ?: typed) },
        singleLine = singleLine,
        minLines = minLines,
        textStyle = resolvedStyle,
        cursorBrush = GoldCursorBrush,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        modifier = modifier.inputChrome(style, requester),
        decorationBox = { inner ->
            InputDecoration(
                isEmpty = value.text.isEmpty(),
                placeholder = placeholder,
                style = style,
                textStyle = resolvedStyle,
                singleLine = singleLine,
                counter = counter?.let { (it.count ?: value.text.length) to it.max },
                leading = leading,
                trailing = trailing,
                inner = inner,
            )
        },
    )
}

@Composable
private fun inputFocusRequester(
    given: FocusRequester?,
    autoFocus: Boolean,
): FocusRequester {
    val requester = given ?: remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { requester.requestFocus() }
    return requester
}

@Composable
private fun AppTextInputStyle.resolvedTextStyle(): TextStyle =
    (textStyle ?: MaterialTheme.typography.bodyLarge).let {
        if (textColor ==
            Color.Unspecified
        ) {
            it
        } else {
            it.copy(color = textColor)
        }
    }

private fun Modifier.inputChrome(
    style: AppTextInputStyle,
    requester: FocusRequester,
): Modifier =
    fillMaxWidthIfCard(style)
        .then(if (style.height != null) Modifier.height(style.height) else Modifier)
        .focusRequester(requester)
        .then(
            style.chrome?.let { Modifier.appCard(it.shape, it.background, it.border) } ?: Modifier,
        )

private fun Modifier.fillMaxWidthIfCard(style: AppTextInputStyle): Modifier =
    if (style.chrome != null) fillMaxWidth() else this

@Composable
private fun InputDecoration(
    isEmpty: Boolean,
    placeholder: String?,
    style: AppTextInputStyle,
    textStyle: TextStyle,
    singleLine: Boolean,
    counter: Pair<Int, Int>?,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    inner: @Composable () -> Unit,
) {
    val text: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.CenterStart) {
            // stacked, not side by side, so the caret sits at the start of the placeholder
            if (isEmpty && placeholder != null) {
                Text(placeholder, style = textStyle, color = style.placeholderColor)
            }
            inner()
        }
    }
    if (singleLine) {
        Row(
            modifier = Modifier.padding(style.contentPadding).fillMaxWidth().then(fillHeight(style)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            leading?.invoke()
            Box(modifier = Modifier.weight(1f)) { text() }
            counter?.let { (count, max) -> AppCharCountPill(count = count, max = max) }
            trailing?.invoke()
        }
    } else {
        Column(modifier = Modifier.padding(style.contentPadding).fillMaxWidth()) {
            text()
            counter?.let { (count, max) ->
                AppCharCountPill(count = count, max = max, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

// a fixed-height field centers its row in that height, a growing one hugs its content
private fun fillHeight(style: AppTextInputStyle): Modifier =
    if (style.height != null) Modifier.fillMaxHeight() else Modifier
