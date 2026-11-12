package app.hydronudge.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import app.hydronudge.ui.components.*
import app.hydronudge.ui.dashboard.Dashboard
import app.hydronudge.ui.drinks.DrinkEditor
import app.hydronudge.ui.history.HistoryScreen
import app.hydronudge.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HydroApp(state: HydroState, model: HydroViewModel) {
    val density = LocalDensity.current
    val windowWidthDp = LocalWindowInfo.current.containerSize.width / density.density
    val compactNavigation = density.fontScale > 1.5f && windowWidthDp < 360f
    val useNavigationRail = windowWidthDp >= 700f
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "today"
    val snackbar = remember { SnackbarHostState() }
    var pendingQuickAmount by rememberSaveable { mutableStateOf<Int?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = state.allEntries.firstOrNull { it.id == editingId } ?: state.entries.firstOrNull { it.id == editingId }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(model) {
        model.feedback.collectLatest { event ->
            if (state.settings.hapticsEnabled && event.message.contains("added")) {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            }
            val hasUndo = event.undoEntry != null || event.undoBatch.isNotEmpty()
            if (snackbar.showSnackbar(
                    event.message,
                    if (hasUndo) "Undo" else null,
                    duration = SnackbarDuration.Short,
                    withDismissAction = true
                ) == SnackbarResult.ActionPerformed
            ) {
                model.undo(event)
            }
        }
    }

    val navItems = listOf("today" to "Today", "history" to "History", "settings" to "Settings")
    val animEnter = if (state.settings.reducedMotion) 0 else 220
    val animExit = if (state.settings.reducedMotion) 0 else 140

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!useNavigationRail) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                ) {
                    navItems.forEach { (route, label) ->
                        NavigationBarItem(
                            modifier = Modifier.testTag("nav:$route"),
                            selected = current == route,
                            onClick = {
                                nav.navigate(route) {
                                    popUpTo("today") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    when (route) {
                                        "today" -> HydroIcons.Drop
                                        "history" -> HydroIcons.Chart
                                        else -> HydroIcons.Settings
                                    },
                                    if (compactNavigation) label else null
                                )
                            },
                            label = if (compactNavigation) null else ({ Text(label) })
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (!state.ready) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            OceanBackground(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.fillMaxSize()) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(Modifier.height(16.dp))
                            navItems.forEach { (route, label) ->
                                NavigationRailItem(
                                    modifier = Modifier.testTag("nav:$route"),
                                    selected = current == route,
                                    onClick = {
                                        nav.navigate(route) {
                                            popUpTo("today") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            when (route) {
                                                "today" -> HydroIcons.Drop
                                                "history" -> HydroIcons.Chart
                                                else -> HydroIcons.Settings
                                            },
                                            null
                                        )
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            nav,
                            startDestination = "today",
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .widthIn(max = 760.dp)
                                .fillMaxSize(),
                            enterTransition = { fadeIn(tween(animEnter)) },
                            exitTransition = { fadeOut(tween(animExit)) },
                            popEnterTransition = { fadeIn(tween(animEnter)) },
                            popExitTransition = { fadeOut(tween(animExit)) }
                        ) {
                            composable("today") {
                                Dashboard(
                                    state = state,
                                    quickAdd = { ml -> if (ml > 1000) pendingQuickAmount = ml else model.quickAdd(ml) },
                                    addDrink = { adding = true },
                                    edit = { editingId = it.id },
                                    delete = model::delete,
                                    settings = { nav.navigate("settings") },
                                    history = { nav.navigate("history") { launchSingleTop = true } },
                                    onLogFavorite = model::logFavorite,
                                    onDeleteFavorite = model::deleteFavorite,
                                    onClearToday = model::clearToday,
                                    onSaveSettings = model::saveSettings
                                )
                            }
                            composable("history") {
                                HistoryScreen(
                                    state = state,
                                    edit = { editingId = it.id },
                                    delete = model::delete
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    settings = state.settings,
                                    save = model::saveSettings,
                                    clear = model::clear,
                                    export = model::export,
                                    import = model::import,
                                    exportCsv = model::exportCsv,
                                    inspectBackup = model::inspectBackup
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    pendingQuickAmount?.let { amount ->
        AlertDialog(
            onDismissRequest = { pendingQuickAmount = null },
            title = { Text("Check the amount") },
            text = { Text("Log ${state.settings.unit.format(amount.toLong())} as one entry?") },
            confirmButton = { TextButton(onClick = { pendingQuickAmount = null; model.quickAdd(amount) }) { Text("Log entry") } },
            dismissButton = { TextButton(onClick = { pendingQuickAmount = null }) { Text("Cancel") } }
        )
    }
    if (state.ready && (adding || editing != null)) {
        DrinkEditor(
            settings = state.settings,
            entry = editing,
            onDismiss = {
                adding = false
                editingId = null
            },
            onSave = { entry ->
                model.save(entry, editing != null)
                adding = false
                editingId = null
            },
            onSaveAsFavorite = model::saveFavorite
        )
    }
}
