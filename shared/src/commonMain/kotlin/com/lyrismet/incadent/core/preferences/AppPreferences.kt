package com.lyrismet.incadent.core.preferences

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

/** whether the "hold a field to edit it" hint still has to be shown in a codex card */
enum class HoldHintState(
    val tag: String,
) {
    PENDING("pending"),
    SEEN("seen"),
}

/** whether a list's one-time swipe-gesture peek still has to be shown - sessions and codex each track their own */
enum class SwipeHintState(
    val tag: String,
) {
    PENDING("pending"),
    SEEN("seen"),
}
