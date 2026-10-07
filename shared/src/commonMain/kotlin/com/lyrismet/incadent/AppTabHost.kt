package com.lyrismet.incadent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.lyrismet.incadent.core.designsystem.component.BottomTabBar
import com.lyrismet.incadent.core.designsystem.component.BottomTabBarItem
import com.lyrismet.incadent.core.designsystem.component.ConfirmationDialog
import com.lyrismet.incadent.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.incadent.core.designsystem.component.UndoToast
import com.lyrismet.incadent.core.designsystem.component.dismissKeyboardOnTap
import com.lyrismet.incadent.core.designsystem.component.icons.CodexTabIcon
import com.lyrismet.incadent.core.designsystem.component.icons.CombatTabIcon
import com.lyrismet.incadent.core.designsystem.component.icons.SessionsTabIcon
import com.lyrismet.incadent.core.designsystem.component.icons.SettingsTabIcon
import com.lyrismet.incadent.core.navigation.BackAction
import com.lyrismet.incadent.core.navigation.resolveBackAction
import com.lyrismet.incadent.core.swipehint.SwipeHintReplayController
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.presentation.codex.CodexScreen
import com.lyrismet.incadent.presentation.sessionlist.SessionListScreen
import com.lyrismet.incadent.presentation.settings.SettingsScreen
import com.slack.circuit.backstack.SaveableBackStack
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_cancel
import dndplayerscodex.shared.generated.resources.app_exit_confirm
import dndplayerscodex.shared.generated.resources.app_exit_text
import dndplayerscodex.shared.generated.resources.app_exit_title
import dndplayerscodex.shared.generated.resources.bottom_tab_codex
import dndplayerscodex.shared.generated.resources.bottom_tab_combat
import dndplayerscodex.shared.generated.resources.bottom_tab_sessions
import dndplayerscodex.shared.generated.resources.bottom_tab_settings
import dndplayerscodex.shared.generated.resources.combat_tab_placeholder
import org.jetbrains.compose.resources.stringResource

internal enum class AppTab {
    SESSIONS,
    CODEX,
    COMBAT,
    SETTINGS,
}

/** a tab's back stack, plus a navigator used only for tab-level actions (not for back presses) */
internal class TabStack(
    val backStack: SaveableBackStack,
    val navigator: Navigator,
)

/** selected tab and each tab's stack, hoisted above AppEnvironment so a locale change keeps them */
internal class AppTabsState(
    val selectedTabIndex: MutableState<Int>,
    val sessions: TabStack,
    val codex: TabStack,
    val settings: TabStack,
) {
    val selectedTab: AppTab get() = AppTab.entries[selectedTabIndex.value]

    fun stackOf(tab: AppTab): TabStack? =
        when (tab) {
            AppTab.SESSIONS -> sessions
            AppTab.CODEX -> codex
            AppTab.SETTINGS -> settings
            AppTab.COMBAT -> null
        }
}

@Composable
internal fun rememberAppTabsState(): AppTabsState {
    val selectedTabIndex = rememberSaveable { mutableStateOf(AppTab.SESSIONS.ordinal) }
    val sessions = rememberTabStack(SessionListScreen)
    val codex = rememberTabStack(CodexScreen)
    val settings = rememberTabStack(SettingsScreen)
    return remember(selectedTabIndex, sessions, codex, settings) {
        AppTabsState(selectedTabIndex, sessions, codex, settings)
    }
}

@Composable
private fun rememberTabStack(root: Screen): TabStack {
    val backStack = rememberSaveableBackStack(root = root)
    // no back handler here: back presses are handled inside each tab, and only for the selected one
    val navigator = rememberCircuitNavigator(backStack, onRootPop = {}, enableBackHandler = false)
    return remember(backStack, navigator) { TabStack(backStack, navigator) }
}

@Composable
internal fun AppTabHost(
    undoController: UndoController,
    swipeHintReplayController: SwipeHintReplayController,
    tabs: AppTabsState,
    onExit: () -> Unit,
) {
    val pendingUndo by undoController.current.collectAsState()
    val selectedTab = tabs.selectedTab
    HandleSwipeHintReplay(swipeHintReplayController, tabs)
    var isExitConfirmVisible by remember { mutableStateOf(false) }
    // the tab bar stays put under the keyboard, so the content only lifts by the keyboard height beyond it
    val density = LocalDensity.current
    var tabBarHeightPx by remember { mutableIntStateOf(0) }
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val contentLiftPx = (imeBottomPx - tabBarHeightPx).coerceAtLeast(0)

    // reached only when the selected tab's own back handler passes, i.e. at a tab root
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = { handleTabRootBack(tabs) { isExitConfirmVisible = true } },
    )

    Column(modifier = Modifier.fillMaxSize().dismissKeyboardOnTap()) {
        // tabs stay composed and zero-sized when hidden so their nav stack and ui state survive switching away
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = with(density) { contentLiftPx.toDp() }),
        ) {
            Box(modifier = tabContentModifier(selectedTab == AppTab.SESSIONS)) {
                TabBackstackHost(tabs.sessions, isSelected = selectedTab == AppTab.SESSIONS)
            }
            Box(modifier = tabContentModifier(selectedTab == AppTab.CODEX)) {
                TabBackstackHost(tabs.codex, isSelected = selectedTab == AppTab.CODEX)
            }
            Box(modifier = tabContentModifier(selectedTab == AppTab.COMBAT)) {
                CombatPlaceholder()
            }
            Box(modifier = tabContentModifier(selectedTab == AppTab.SETTINGS)) {
                TabBackstackHost(tabs.settings, isSelected = selectedTab == AppTab.SETTINGS)
            }
            UndoToast(
                action = pendingUndo,
                onUndo = undoController::undo,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
        BottomTabBar(
            modifier = Modifier.onSizeChanged { tabBarHeightPx = it.height },
            tabs = appTabBarItems(),
            selectedIndex = tabs.selectedTabIndex.value,
            onTabSelected = { index -> onTabTapped(tabs, index) },
        )
    }

    if (isExitConfirmVisible) {
        ConfirmationDialog(
            title = stringResource(Res.string.app_exit_title),
            text = stringResource(Res.string.app_exit_text),
            onConfirm = onExit,
            onDismiss = { isExitConfirmVisible = false },
            confirmLabel = stringResource(Res.string.app_exit_confirm),
            dismissLabel = stringResource(Res.string.action_cancel),
        )
    }
}

// settings asked to replay the sessions swipe hint - jump there and pop its stack to root, like onTabTapped does
@Composable
private fun HandleSwipeHintReplay(
    controller: SwipeHintReplayController,
    tabs: AppTabsState,
) {
    val sessionsReplayRequested by controller.sessionsReplayRequested.collectAsState()
    LaunchedEffect(sessionsReplayRequested) {
        if (sessionsReplayRequested) {
            tabs.selectedTabIndex.value = AppTab.SESSIONS.ordinal
            popToRoot(tabs.sessions)
            controller.onSessionsReplayHandled()
        }
    }
}

private fun handleTabRootBack(
    tabs: AppTabsState,
    onExitRequested: () -> Unit,
) {
    val tab = tabs.selectedTab
    val stack = tabs.stackOf(tab)
    val depth = stack?.backStack?.size ?: 1
    when (resolveBackAction(isOnSessionsTab = tab == AppTab.SESSIONS, activeTabDepth = depth)) {
        BackAction.Pop -> stack?.navigator?.pop()
        BackAction.SwitchToSessions -> tabs.selectedTabIndex.value = AppTab.SESSIONS.ordinal
        BackAction.ConfirmExit -> onExitRequested()
    }
}

// tapping the active tab returns it to its root, tapping another tab just switches to it
private fun onTabTapped(
    tabs: AppTabsState,
    index: Int,
) {
    if (index != tabs.selectedTabIndex.value) {
        tabs.selectedTabIndex.value = index
        return
    }
    val stack = tabs.stackOf(AppTab.entries[index]) ?: return
    popToRoot(stack)
}

private fun popToRoot(stack: TabStack) {
    repeat(stack.backStack.size - 1) { stack.navigator.pop() }
}

private fun tabContentModifier(visible: Boolean): Modifier =
    if (visible) Modifier.fillMaxSize() else Modifier.size(0.dp)

// a hidden tab stays fully composed (zero-sized) so its nav stack and ui state survive switching away - this
// tells its presenters whether their tab is actually on screen right now, e.g. so a one-time hint doesn't
// burn itself in the background while the user is looking at a different tab
internal val LocalTabSelected = compositionLocalOf { true }

// each tab gets its own dispatcher, enabled only while selected, so hidden tabs never swallow a back press
@Composable
private fun TabBackstackHost(
    stack: TabStack,
    isSelected: Boolean,
) {
    val owner = rememberNavigationEventDispatcherOwner(enabled = isSelected)
    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides owner,
        LocalTabSelected provides isSelected,
    ) {
        val navigator = rememberCircuitNavigator(stack.backStack, onRootPop = {})
        NavigableCircuitContent(navigator = navigator, backStack = stack.backStack)
    }
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
