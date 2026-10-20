package com.lyrismet.incadent.feature.codex.presentation.quickedit

/** how an in-place input behaves - a title is one large line, a number takes a digit keypad, a description wraps */
enum class InlineInputKind { TITLE, TEXT, LIST, NUMBER, MULTILINE }

/** the input kind a text or number field opens with */
fun QuickEditField.inputKind(): InlineInputKind =
    when (kind) {
        is QuickEditKind.Number -> InlineInputKind.NUMBER
        QuickEditKind.Text ->
            when (this) {
                QuickEditField.NAME -> InlineInputKind.TITLE
                QuickEditField.DESCRIPTION -> InlineInputKind.MULTILINE
                QuickEditField.NICKNAMES -> InlineInputKind.LIST
                else -> InlineInputKind.TEXT
            }
        QuickEditKind.Link,
        QuickEditKind.Places,
        QuickEditKind.Choice,
        QuickEditKind.Photo,
        QuickEditKind.Relations,
        -> InlineInputKind.TEXT
    }
