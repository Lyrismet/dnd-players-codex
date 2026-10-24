package com.lyrismet.incadent.feature.combat.presentation

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.sessionNumberLabels
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_target_new
import dndplayerscodex.shared.generated.resources.combat_target_prefix
import dndplayerscodex.shared.generated.resources.combat_target_session
import dndplayerscodex.shared.generated.resources.combat_target_session_titled
import org.jetbrains.compose.resources.stringResource

/** "Итоги попадут в" and the session the results go to, a new one when none is running */
data class CombatTarget(
    val prefix: String,
    val session: String,
) {
    val sentence: String get() = "$prefix $session"
}

/** [withTitle] adds the session's title after its number, the preparation header names it that way */
@Composable
fun combatSessionTarget(
    sessions: List<SessionNote>,
    numbering: SessionNumbering,
    withTitle: Boolean = false,
): CombatTarget {
    val prefix = stringResource(Res.string.combat_target_prefix)
    val live =
        sessions.firstOrNull { it.isLive } ?: return CombatTarget(prefix, stringResource(Res.string.combat_target_new))
    val label = sessions.sessionNumberLabels(numbering).getValue(live.id)
    val session =
        if (withTitle) {
            stringResource(Res.string.combat_target_session_titled, label, live.title)
        } else {
            stringResource(Res.string.combat_target_session, label)
        }
    return CombatTarget(prefix, session)
}
