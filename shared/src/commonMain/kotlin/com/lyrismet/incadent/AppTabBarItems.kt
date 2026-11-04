package com.lyrismet.incadent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.BottomTabBarItem
import com.lyrismet.incadent.core.designsystem.component.icons.CodexTabIcon
import com.lyrismet.incadent.core.designsystem.component.icons.CombatTabIcon
import com.lyrismet.incadent.core.designsystem.component.icons.SessionsTabIcon
import com.lyrismet.incadent.core.designsystem.component.icons.SettingsTabIcon
import com.lyrismet.incadent.core.designsystem.component.icons.entity.AppEntityIcon
import com.lyrismet.incadent.core.designsystem.component.icons.entity.EntityIcons
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.bottom_tab_codex
import dndplayerscodex.shared.generated.resources.bottom_tab_combat
import dndplayerscodex.shared.generated.resources.bottom_tab_me
import dndplayerscodex.shared.generated.resources.bottom_tab_me_description
import dndplayerscodex.shared.generated.resources.bottom_tab_sessions
import dndplayerscodex.shared.generated.resources.bottom_tab_settings
import org.jetbrains.compose.resources.stringResource

private val MeTabIconSize = 24.dp

@Composable
internal fun appTabBarItems(combatRound: Int?): List<BottomTabBarItem> {
    val sessions = stringResource(Res.string.bottom_tab_sessions)
    val codex = stringResource(Res.string.bottom_tab_codex)
    val me = stringResource(Res.string.bottom_tab_me)
    val meDescription = stringResource(Res.string.bottom_tab_me_description)
    val combat = stringResource(Res.string.bottom_tab_combat)
    val settings = stringResource(Res.string.bottom_tab_settings)
    return remember(sessions, codex, me, meDescription, combat, settings, combatRound) {
        // the order is the AppTab order
        AppTab.entries.map { tab ->
            when (tab) {
                AppTab.SESSIONS -> BottomTabBarItem(sessions) { tint -> SessionsTabIcon(tint) }
                AppTab.CODEX -> BottomTabBarItem(codex) { tint -> CodexTabIcon(tint) }
                AppTab.ME ->
                    BottomTabBarItem(me, raised = true, contentDescription = meDescription) { tint ->
                        AppEntityIcon(EntityIcons.Me, tint, size = MeTabIconSize)
                    }
                AppTab.COMBAT -> BottomTabBarItem(combat, combatRound?.toString()) { tint -> CombatTabIcon(tint) }
                AppTab.SETTINGS -> BottomTabBarItem(settings) { tint -> SettingsTabIcon(tint) }
            }
        }
    }
}
