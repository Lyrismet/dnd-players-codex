package com.lyrismet.incadent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.BottomTabBar
import com.lyrismet.incadent.core.designsystem.component.BottomTabBarItem
import com.lyrismet.incadent.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.incadent.core.designsystem.component.UndoToast
import com.lyrismet.incadent.core.designsystem.component.dismissKeyboardOnTap
import com.lyrismet.incadent.core.designsystem.component.icons.CodexTabIcon
import com.lyrismet.incadent.core.designsystem.component.icons.CombatTabIcon
import com.lyrismet.incadent.core.designsystem.component.icons.SessionsTabIcon
import com.lyrismet.incadent.core.designsystem.component.icons.SettingsTabIcon
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.presentation.codex.CodexScreen
import com.lyrismet.incadent.presentation.sessionlist.SessionListScreen
import com.lyrismet.incadent.presentation.settings.SettingsScreen
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.bottom_tab_codex
import dndplayerscodex.shared.generated.resources.bottom_tab_combat
import dndplayerscodex.shared.generated.resources.bottom_tab_sessions
import dndplayerscodex.shared.generated.resources.bottom_tab_settings
import dndplayerscodex.shared.generated.resources.combat_tab_placeholder
import org.jetbrains.compose.resources.stringResource

private enum class AppTab {
    SESSIONS,
    CODEX,
    COMBAT,
    SETTINGS,
}

/** selected tab and each tab's back stack, hoisted above AppEnvironment so a locale change keeps them */
internal class AppTabsState(
    val selectedTabIndex: MutableState<Int>,
    val sessionsBackStack: SaveableBackStack,
    val codexBackStack: SaveableBackStack,
    val settingsBackStack: SaveableBackStack,
)

@Composable
internal fun rememberAppTabsState(): AppTabsState {
    val selectedTabIndex = rememberSaveable { mutableStateOf(AppTab.SESSIONS.ordinal) }
    val sessionsBackStack = rememberSaveableBackStack(root = SessionListScreen)
    val codexBackStack = rememberSaveableBackStack(root = CodexScreen)
    val settingsBackStack = rememberSaveableBackStack(root = SettingsScreen)
    return remember(selectedTabIndex, sessionsBackStack, codexBackStack, settingsBackStack) {
        AppTabsState(selectedTabIndex, sessionsBackStack, codexBackStack, settingsBackStack)
    }
}

/** renders the bottom tab bar and the tab selected in [tabs] - relies on an ambient Circuit */
@Composable
internal fun AppTabHost(
    undoController: UndoController,
    tabs: AppTabsState,
) {
    val pendingUndo by undoController.current.collectAsState()
    val selectedTab = AppTab.entries[tabs.selectedTabIndex.value]

    Column(modifier = Modifier.fillMaxSize().dismissKeyboardOnTap()) {
        // tabs stay composed and zero-sized when hidden so their nav stack and ui state survive switching away
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(modifier = tabContentModifier(selectedTab == AppTab.SESSIONS)) {
                TabBackstackHost(tabs.sessionsBackStack)
            }
            Box(modifier = tabContentModifier(selectedTab == AppTab.CODEX)) {
                TabBackstackHost(tabs.codexBackStack)
            }
            Box(modifier = tabContentModifier(selectedTab == AppTab.COMBAT)) {
                CombatPlaceholder()
            }
            Box(modifier = tabContentModifier(selectedTab == AppTab.SETTINGS)) {
                TabBackstackHost(tabs.settingsBackStack)
            }
            UndoToast(
                action = pendingUndo,
                onUndo = undoController::undo,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
        BottomTabBar(
            tabs = appTabBarItems(),
            selectedIndex = tabs.selectedTabIndex.value,
            onTabSelected = { tabs.selectedTabIndex.value = it },
        )
    }
}

private fun tabContentModifier(visible: Boolean): Modifier =
    if (visible) Modifier.fillMaxSize() else Modifier.size(0.dp)

@Composable
private fun TabBackstackHost(backStack: SaveableBackStack) {
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
private fun appTabBarItems(): List<BottomTabBarItem> =
    listOf(
        BottomTabBarItem(stringResource(Res.string.bottom_tab_sessions)) { tint -> SessionsTabIcon(tint) },
        BottomTabBarItem(stringResource(Res.string.bottom_tab_codex)) { tint -> CodexTabIcon(tint) },
        BottomTabBarItem(stringResource(Res.string.bottom_tab_combat)) { tint -> CombatTabIcon(tint) },
        BottomTabBarItem(stringResource(Res.string.bottom_tab_settings)) { tint -> SettingsTabIcon(tint) },
    )
