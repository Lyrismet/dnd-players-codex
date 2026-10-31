package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.TextUnit
import com.lyrismet.incadent.core.designsystem.AppPalette

private val URL_PATTERN = Regex("""https?://[^\s]+""")
private const val TRAILING_PUNCTUATION = ".,;:!?)"

/** text whose web addresses open in the browser on a tap and which can be selected and copied */
@Composable
fun AppLinkedText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    linkColor: Color = AppPalette.GoldBright,
) {
    val annotated = remember(text, linkColor) { linkedString(text, linkColor) }
    SelectionContainer(modifier = modifier) {
        Text(text = annotated, fontSize = fontSize, color = color)
    }
}

internal fun linkedString(
    text: String,
    linkColor: Color,
): AnnotatedString =
    buildAnnotatedString {
        var cursor = 0
        val style = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
        for (match in URL_PATTERN.findAll(text)) {
            val url = match.value.trimEnd { it in TRAILING_PUNCTUATION }
            append(text.substring(cursor, match.range.first))
            withLink(LinkAnnotation.Url(url, style)) { append(url) }
            cursor = match.range.first + url.length
        }
        append(text.substring(cursor))
    }
