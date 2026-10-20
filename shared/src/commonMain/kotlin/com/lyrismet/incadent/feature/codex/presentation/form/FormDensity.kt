package com.lyrismet.incadent.feature.codex.presentation.form

/** how much of the entry form is on view - the screen that opens the form picks it */
sealed interface FormDensity {
    /** every field in line, the calm creation and editing in the codex */
    data object Full : FormDensity

    /** the rarely filled fields folded under "more", the quick creation inside a session */
    data object Compact : FormDensity
}
