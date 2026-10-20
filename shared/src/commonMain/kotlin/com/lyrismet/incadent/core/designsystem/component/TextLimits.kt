package com.lyrismet.incadent.core.designsystem.component

/** character caps for every text input, so a pasted wall of text can't break the layout */
object TextLimits {
    /** names, titles and short single-line facts - race, class, player, reward, campaign */
    const val LINE = 32
    const val SESSION_TITLE = 32
    const val TAG = 24
    const val SEARCH = 64
    const val PHRASE = 200

    /** the comma separated nicknames of one record */
    const val NICKNAMES = 96

    /** descriptions, party notes and session notes */
    const val NOTE = 2000
}
