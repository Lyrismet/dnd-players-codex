package com.lyrismet.incadent.core.mention

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

private val DISPLAY_TOKEN_REGEX = Regex("@\\[([^\\[\\]|]*)\\|[npql]:\\d+]")

/** one token inside a draft - where it sits in the stored text and in the text the field shows */
internal class TokenSpan(
    val rawStart: Int,
    val rawEnd: Int,
    val displayStart: Int,
    val displayEnd: Int,
)

/** the draft as the field shows it - "@Name" for every token - with the cursor mapped both ways */
internal class MentionDisplay(
    raw: String,
) {
    val spans: List<TokenSpan>
    val text: String

    init {
        val found = mutableListOf<TokenSpan>()
        val out = StringBuilder()
        var cursor = 0
        DISPLAY_TOKEN_REGEX.findAll(raw).forEach { token ->
            out.append(raw, cursor, token.range.first)
            val displayStart = out.length
            out.append('@').append(token.groupValues[1].replace('_', ' '))
            found += TokenSpan(token.range.first, token.range.last + 1, displayStart, out.length)
            cursor = token.range.last + 1
        }
        out.append(raw, cursor, raw.length)
        spans = found
        text = out.toString()
    }

    /** a cursor inside a tag lands after it, a tag is never entered */
    fun toDisplay(rawOffset: Int): Int {
        val inside = spans.firstOrNull { it.rawStart < rawOffset && rawOffset < it.rawEnd }
        val shift = spans.lastOrNull { it.rawEnd <= rawOffset }?.let { it.displayEnd - it.rawEnd } ?: 0
        return inside?.displayEnd ?: (rawOffset + shift)
    }

    /** a cursor inside the shown "@Name" snaps to the nearer end of its token */
    fun toRaw(displayOffset: Int): Int {
        val inside = spans.firstOrNull { it.displayStart < displayOffset && displayOffset < it.displayEnd }
        val shift = spans.lastOrNull { it.displayEnd <= displayOffset }?.let { it.displayEnd - it.rawEnd } ?: 0
        return when {
            inside == null -> displayOffset - shift
            displayOffset - inside.displayStart <= (inside.displayEnd - inside.displayStart) / 2 -> inside.rawStart
            else -> inside.rawEnd
        }
    }
}

/** how many characters the field shows for [raw] - a token counts as its "@Name", not its stored form */
fun mentionDisplayLength(raw: String): Int = MentionDisplay(raw).text.length

/** [raw] cut to [limit] shown characters, a token that would straddle the limit is left out whole */
fun takeMentionDisplay(
    raw: String,
    limit: Int,
): String {
    val display = MentionDisplay(raw)
    if (display.text.length <= limit) return raw
    val straddling = display.spans.firstOrNull { it.displayStart < limit && limit < it.displayEnd }
    return raw.substring(0, straddling?.rawStart ?: display.toRaw(limit))
}

/** shows every mention token of a draft as its "@Name" in [tokenStyle], the stored text keeps the token */
class MentionVisualTransformation(
    private val tokenStyle: SpanStyle,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val display = MentionDisplay(text.text)
        val styled =
            AnnotatedString(
                display.text,
                display.spans.map { AnnotatedString.Range(tokenStyle, it.displayStart, it.displayEnd) },
            )
        val mapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = display.toDisplay(offset)

                override fun transformedToOriginal(offset: Int) = display.toRaw(offset)
            }
        return TransformedText(styled, mapping)
    }
}
