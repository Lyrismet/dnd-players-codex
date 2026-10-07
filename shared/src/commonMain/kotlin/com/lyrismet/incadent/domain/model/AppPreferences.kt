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

/** how a codex card is edited - in place with holds ([QUICK]) or read-only with the form ([FORM]) */
enum class EntityEditMode(
    val tag: String,
) {
    QUICK("quick"),
    FORM("form"),
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
