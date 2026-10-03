package com.lyrismet.incadent.domain.model

enum class SessionNumbering(
    val tag: String,
) {
    ROMAN("roman"),
    ARABIC("arabic"),
}

enum class MentionStyle(
    val tag: String,
) {
    FILLED("filled"),
    UNDERLINE("underline"),
}
