package com.lyrismet.incadent.core.tags

import androidx.compose.runtime.Composable
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_tag_builtin_combat
import dndplayerscodex.shared.generated.resources.session_tag_builtin_intrigue
import dndplayerscodex.shared.generated.resources.session_tag_builtin_investigation
import dndplayerscodex.shared.generated.resources.session_tag_builtin_plot
import dndplayerscodex.shared.generated.resources.session_tag_builtin_rest
import dndplayerscodex.shared.generated.resources.session_tag_builtin_trade
import dndplayerscodex.shared.generated.resources.session_tag_builtin_travel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// same order as BUILT_IN_SESSION_TAGS, whose russian names are the stored keys
internal val BUILT_IN_SESSION_TAG_LABELS: List<StringResource> =
    listOf(
        Res.string.session_tag_builtin_combat,
        Res.string.session_tag_builtin_plot,
        Res.string.session_tag_builtin_investigation,
        Res.string.session_tag_builtin_intrigue,
        Res.string.session_tag_builtin_trade,
        Res.string.session_tag_builtin_travel,
        Res.string.session_tag_builtin_rest,
    )

/** the tag name in the app language - built-in tags are stored in russian, custom tags show as typed */
@Composable
fun sessionTagLabel(name: String): String {
    val index = BUILT_IN_SESSION_TAGS.indexOfFirst { it.equals(name, ignoreCase = true) }
    return if (index >= 0) stringResource(BUILT_IN_SESSION_TAG_LABELS[index]) else name
}
