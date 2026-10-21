package com.lyrismet.incadent.feature.mention.domain

// what is left of a token after an edit cut into it
private val TOKEN_FRAGMENT_REGEX = Regex("@\\[[^\\[\\]@]*?\\|[npqls]:\\d*(?:\\|\\d*)?]?")

/** the draft without any half deleted token, so an edit inside a tag removes the tag instead of corrupting it */
fun withoutBrokenTokens(text: String): String {
    if ("@[" !in text) return text
    val out = StringBuilder()
    var i = 0
    while (i < text.length) {
        if (!text.startsWith("@[", i)) {
            out.append(text[i])
            i++
            continue
        }
        val whole = TOKEN_REGEX.matchAt(text, i)
        if (whole != null) {
            out.append(whole.value)
            i += whole.value.length
        } else {
            // a cut token goes whole, a stray "@[" alone loses just its two characters
            i += TOKEN_FRAGMENT_REGEX.matchAt(text, i)?.value?.length ?: 2
        }
    }
    return out.toString()
}
