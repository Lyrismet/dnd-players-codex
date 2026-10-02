package com.lyrismet.dndcodex

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.component.BottomTabBar
import com.lyrismet.dndcodex.core.designsystem.component.BottomTabBarItem
import com.lyrismet.dndcodex.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.dndcodex.core.designsystem.component.UndoToast
import com.lyrismet.dndcodex.core.designsystem.component.dismissKeyboardOnTap
import com.lyrismet.dndcodex.core.designsystem.component.icons.CodexTabIcon
import com.lyrismet.dndcodex.core.designsystem.component.icons.CombatTabIcon
import com.lyrismet.dndcodex.core.designsystem.component.icons.SessionsTabIcon
import com.lyrismet.dndcodex.core.designsystem.component.icons.SettingsTabIcon
import com.lyrismet.dndcodex.core.undo.UndoController
import com.lyrismet.dndcodex.presentation.codex.CodexScreen
import com.lyrismet.dndcodex.presentation.sessionlist.SessionListScreen
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.screen.Screen
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.bottom_tab_codex
import dndplayerscodex.shared.generated.resources.bottom_tab_combat
import dndplayerscodex.shared.generated.resources.bottom_tab_sessions
import dndplayerscodex.shared.generated.resources.bottom_tab_settings
import dndplayerscodex.shared.generated.resources.combat_tab_placeholder
import dndplayerscodex.shared.generated.resources.settings_tab_placeholder
import org.jetbrains.compose.resources.stringResource

private enum class AppTab {
    SESSIONS,
    CODEX,
    COMBAT,
    SETTINGS,
}

/** owns the selected bottom tab and hosts each tab's own navigation - relies on an ambient Circuit */
@Composable
internal fun AppTabHost(undoController: UndoController) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(AppTab.SESSIONS.ordinal) }
    val selectedTab = AppTab.entries[selectedTabIndex]
    val pendingUndo by undoController.current.collectAsState()

    Column(modifier = Modifier.fillMaxSize().dismissKeyboardOnTap()) {
        // tabs stay composed and zero-sized when hidden so their nav stack and ui state survive switching away
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(modifier = tabContentModifier(selectedTab == AppTab.SESSIONS)) {
                TabBackstackHost(root = SessionListScreen)
            }
            Box(modifier = tabContentModifier(selectedTab == AppTab.CODEX)) {
                TabBackstackHost(root = CodexScreen)
            }
            Box(modifier = tabContentModifier(selectedTab == AppTab.COMBAT)) {
                CombatPlaceholder()
            }
            Box(modifier = tabContentModifier(selectedTab == AppTab.SETTINGS)) {
                SettingsPlaceholder()
            }
            UndoToast(
                action = pendingUndo,
                onUndo = undoController::undo,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
        BottomTabBar(
            tabs = appTabBarItems(),
            selectedIndex = selectedTabIndex,
            onTabSelected = { selectedTabIndex = it },
        )
    }
}

private fun tabContentModifier(visible: Boolean): Modifier =
    if (visible) Modifier.fillMaxSize() else Modifier.size(0.dp)

@Composable
private fun TabBackstackHost(root: Screen) {
    val backStack = rememberSaveableBackStack(root = root)
    val navigator = rememberCircuitNavigator(backStack, onRootPop = {})
    NavigableCircuitContent(navigator = navigator, backStack = backStack)
}

@Composable
private fun CombatPlaceholder() {
    Scaffold { contentPadding ->
        EmptyStatePlaceholder(
            text = stringResource(Res.string.combat_tab_placeholder),
            modifier = Modifier.padding(contentPadding),
        )
    }
}

@Composable
private fun SettingsPlaceholder() {
    Scaffold { contentPadding ->
        EmptyStatePlaceholder(
            text = stringResource(Res.string.settings_tab_placeholder),
            modifier = Modifier.padding(contentPadding),
        )
    }
}

@Composable
private fun appTabBarItems(): List<BottomTabBarItem> =
    listOf(
        BottomTabBarItem(stringResource(Res.string.bottom_tab_sessions)) { tint -> SessionsTabIcon(tint) },
        BottomTabBarItem(stringResource(Res.string.bottom_tab_codex)) { tint -> CodexTabIcon(tint) },
        BottomTabBarItem(stringResource(Res.string.bottom_tab_combat)) { tint -> CombatTabIcon(tint) },
        BottomTabBarItem(stringResource(Res.string.bottom_tab_settings)) { tint -> SettingsTabIcon(tint) },
    )
