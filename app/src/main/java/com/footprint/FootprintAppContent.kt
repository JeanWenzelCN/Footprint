package com.footprint

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.footprint.ui.components.AddFootprintDialog
import com.footprint.ui.components.AddGoalDialog
import com.footprint.ui.components.LiquidGlassCard
import com.footprint.ui.effects.LiquidNavItem
import com.footprint.ui.effects.LiquidNavigationBar
import com.footprint.ui.effects.bouncyClick
import com.footprint.ui.screens.*
import com.footprint.ui.theme.FootprintTheme
import com.footprint.ui.theme.LocalHazeState

@Composable
fun FootprintApp() {
    val navController = rememberNavController()
    val viewModel: FootprintViewModel = viewModel(factory = FootprintViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val performHaptic = {
        if (uiState.hapticFeedbackEnabled) {
            haptic.performHapticFeedback(
                    HapticFeedbackType.LongPress
            ) // Using LongPress for a distinct "tick" feel
        }
    }

    var showEntryDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<com.footprint.data.model.FootprintEntry?>(null) }
    var detailEntry by remember { mutableStateOf<com.footprint.data.model.FootprintEntry?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<com.footprint.data.model.TravelGoal?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val isBlurActive =
            showEntryDialog || editingEntry != null || showGoalDialog || editingGoal != null
    val isDark =
            uiState.themeMode == com.footprint.ui.theme.ThemeMode.DARK ||
                    (uiState.themeMode == com.footprint.ui.theme.ThemeMode.SYSTEM &&
                            isSystemInDarkTheme())

    val tabOrder = listOf("dashboard", "map", "timeline", "planner")

    FootprintTheme(
            themeMode = uiState.themeMode,
            style = uiState.themeStyle,
            dominantMood = uiState.summary.monthly.dominantMood
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                    modifier =
                            Modifier.then(
                                    if (isBlurActive) {
                                        Modifier.blur(uiState.blurStrength.dp).drawWithContent {
                                            drawContent()
                                            drawRect(
                                                    if (isDark) Color.Black.copy(alpha = 0.3f)
                                                    else Color.White.copy(alpha = 0.3f)
                                            )
                                        }
                                    } else Modifier
                            ),
                    floatingActionButton = {
                        if (currentDestination == "dashboard" ||
                                        currentDestination == "timeline" ||
                                        currentDestination == "planner"
                        ) {
                            LiquidGlassCard(
                                    shape = CircleShape,
                                    modifier =
                                            Modifier.padding(bottom = 80.dp)
                                                    .bouncyClick()
                                                    .clickable {
                                                        performHaptic()
                                                        showEntryDialog = true
                                                    }
                            ) {
                                Icon(
                                        Icons.Outlined.Add,
                                        contentDescription = "添加足迹",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier =
                                                Modifier.size(56.dp) // Standard FAB size
                                                        .padding(16.dp) // Inner padding for icon
                                )
                            }
                        }
                    },

                    // ...

                    bottomBar = {
                        val hideBottomBar =
                                currentDestination == "settings" ||
                                        currentDestination == "generative_art" ||
                                        currentDestination == "art_studio" ||
                                        currentDestination?.startsWith("export_trace") == true
                        if (!hideBottomBar) {
                            val items = remember {
                                FootprintTab.entries.map {
                                    LiquidNavItem(it.route, it.label, it.icon)
                                }
                            }

                            val selectedIndex =
                                    FootprintTab.entries
                                            .indexOfFirst { it.route == currentDestination }
                                            .takeIf { it != -1 }
                                            ?: 0

                            LiquidNavigationBar(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .padding(horizontal = 24.dp, vertical = 24.dp)
                                                    .height(
                                                            72.dp
                                                    ), // Slightly taller for the liquid blob
                                    hazeState = LocalHazeState.current,
                                    items = items,
                                    selectedIndex = selectedIndex,
                                    onItemSelected = { index ->
                                        val tab = FootprintTab.entries[index]
                                        performHaptic()
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                            )
                        }
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.fillMaxSize(),
                        enterTransition = {
                            val fromRoute = initialState.destination.route
                            val toRoute = targetState.destination.route
                            val fromIndex = tabOrder.indexOf(fromRoute)
                            val toIndex = tabOrder.indexOf(toRoute)

                            // iOS-style Silky Smooth Transition
                            // Slower duration (500ms) with a sophisticated easing curve
                            // (FastOutSlowIn)
                            if (fromIndex != -1 && toIndex != -1) {
                                if (toIndex > fromIndex) {
                                    slideIntoContainer(
                                            AnimatedContentTransitionScope.SlideDirection.Start,
                                            animationSpec = tween(500, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(500))
                                } else {
                                    slideIntoContainer(
                                            AnimatedContentTransitionScope.SlideDirection.End,
                                            animationSpec = tween(500, easing = FastOutSlowInEasing)
                                    ) + fadeIn(animationSpec = tween(500))
                                }
                            } else {
                                fadeIn(animationSpec = tween(500))
                            }
                        },
                        exitTransition = {
                            val fromRoute = initialState.destination.route
                            val toRoute = targetState.destination.route
                            val fromIndex = tabOrder.indexOf(fromRoute)
                            val toIndex = tabOrder.indexOf(toRoute)

                            // Parallax-like exit (slower fade, scale down slightly if possible, but
                            // slide works best here)
                            if (fromIndex != -1 && toIndex != -1) {
                                if (toIndex > fromIndex) {
                                    slideOutOfContainer(
                                            AnimatedContentTransitionScope.SlideDirection.Start,
                                            animationSpec = tween(500, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(500))
                                } else {
                                    slideOutOfContainer(
                                            AnimatedContentTransitionScope.SlideDirection.End,
                                            animationSpec = tween(500, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(500))
                                }
                            } else {
                                fadeOut(animationSpec = tween(500))
                            }
                        }
                ) {
                    composable("dashboard") {
                        DashboardScreen(
                                state = uiState,
                                onSearch = viewModel::updateSearch,
                                onYearShift = viewModel::shiftYear,
                                onMoodSelected = viewModel::toggleMoodFilter,
                                onCreateGoal = { showGoalDialog = true },
                                onExportTrace = { year ->
                                    if (year != null) navController.navigate("export_trace/$year")
                                    else navController.navigate("export_trace")
                                },
                                onSettings = { navController.navigate("settings") },
                                onEditEntry = { editingEntry = it },
                                onDeleteEntry = viewModel::deleteFootprint,
                                onDetailClick = { detailEntry = it },
                                onEditGoal = { editingGoal = it },
                                onDeleteGoal = viewModel::deleteGoal,
                                onMemoryLaneClick = {
                                    navController.navigate("timeline") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                        )
                    }
                    composable("settings") {
                        val context = LocalContext.current
                        SettingsScreen(
                                currentThemeMode = uiState.themeMode,
                                currentThemeStyle = uiState.themeStyle,
                                currentNickname = uiState.userNickname,
                                currentAvatarId = uiState.userAvatarId,
                                currentBlurStrength = uiState.blurStrength,
                                currentHapticFeedback = uiState.hapticFeedbackEnabled,
                                onThemeModeChange = viewModel::setThemeMode,
                                onThemeStyleChange = viewModel::setThemeStyle,
                                onBlurStrengthChange = viewModel::setBlurStrength,
                                onHapticFeedbackChange = viewModel::setHapticFeedback,
                                onUpdateProfile = viewModel::updateProfile,
                                onUpdateAvatar = viewModel::updateAvatar,
                                onExportData = { uri ->
                                    viewModel.exportData(
                                            uri = uri,
                                            onSuccess = {
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "数据导出成功",
                                                                android.widget.Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            },
                                            onError = { error ->
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "导出错误: $error",
                                                                android.widget.Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                            }
                                    )
                                },
                                onImportData = { uri ->
                                    viewModel.importData(
                                            uri = uri,
                                            onSuccess = {
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "数据恢复完成",
                                                                android.widget.Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                            },
                                            onError = { error ->
                                                android.widget.Toast.makeText(
                                                                context,
                                                                "导入错误: $error",
                                                                android.widget.Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                            }
                                    )
                                },
                                onGenerativeArt = { navController.navigate("art_studio") },
                                onBack = { navController.popBackStack() }
                        )
                    }
                    composable("export_trace") {
                        ExportTraceScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                        )
                    }
                    composable("export_trace/{year}") { backStackEntry ->
                        val year = backStackEntry.arguments?.getString("year")?.toIntOrNull()
                        ExportTraceScreen(
                                viewModel = viewModel,
                                initialYear = year,
                                onBack = { navController.popBackStack() }
                        )
                    }
                    composable("art_studio") {
                        com.footprint.ui.screens.art.FootprintArtStudioScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                        )
                    }
                    composable("generative_art") {
                        GenerativeArtScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                        )
                    }
                    composable("map") {
                        MapScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { id ->
                                    editingEntry = uiState.entries.find { it.id == id }
                                },
                                entries = uiState.visibleEntries,
                                contentPadding = innerPadding
                        )
                    }
                    composable("timeline") {
                        TimelineScreen(
                                entries =
                                        if (uiState.filterState.searchQuery.isBlank())
                                                uiState.entries
                                        else uiState.visibleEntries,
                                filterState = uiState.filterState,
                                hapticFeedbackEnabled = uiState.hapticFeedbackEnabled,
                                onMoodFilterChange = viewModel::toggleMoodFilter,
                                onSearch = viewModel::updateSearch,
                                onEditEntry = { editingEntry = it },
                                onDeleteEntry = viewModel::deleteFootprint,
                                onDetailClick = { detailEntry = it }
                        )
                    }
                    composable("planner") {
                        GoalPlannerScreen(
                                goals = uiState.goals,
                                summary = uiState.summary,
                                hapticFeedbackEnabled = uiState.hapticFeedbackEnabled,
                                onToggleGoal = viewModel::toggleGoal,
                                onAddGoal = { showGoalDialog = true },
                                onEditGoal = { editingGoal = it },
                                onDeleteGoal = viewModel::deleteGoal
                        )
                    }
                }
            }
        }

        // 处理添加/编辑对话框
        if (showEntryDialog || editingEntry != null) {
            AddFootprintDialog(
                    initialEntry = editingEntry,
                    onDismiss = {
                        showEntryDialog = false
                        editingEntry = null
                    },
                    onSave = { payload ->
                        if (editingEntry != null) {
                            viewModel.updateFootprint(
                                    editingEntry!!.copy(
                                            title = payload.title,
                                            location = payload.location,
                                            detail = payload.detail,
                                            mood = payload.mood,
                                            tags = payload.tags,
                                            distanceKm = payload.distance,
                                            energyLevel = payload.energy,
                                            happenedOn = payload.date,
                                            latitude = payload.latitude,
                                            longitude = payload.longitude,
                                            icon = payload.icon,
                                            photos = payload.photos
                                    )
                            )
                        } else {
                            viewModel.addFootprint(
                                    title = payload.title,
                                    location = payload.location,
                                    detail = payload.detail,
                                    mood = payload.mood,
                                    tags = payload.tags,
                                    distanceKm = payload.distance,
                                    photos = payload.photos,
                                    energyLevel = payload.energy,
                                    date = payload.date,
                                    latitude = payload.latitude,
                                    longitude = payload.longitude,
                                    icon = payload.icon
                            )
                        }
                        showEntryDialog = false
                        editingEntry = null
                    }
            )
        }

        if (showGoalDialog || editingGoal != null) {
            AddGoalDialog(
                    initialGoal = editingGoal,
                    onDismiss = {
                        showGoalDialog = false
                        editingGoal = null
                    },
                    onSave = { goal ->
                        if (editingGoal != null) {
                            viewModel.updateGoal(
                                    editingGoal!!.copy(
                                            title = goal.title,
                                            targetLocation = goal.location,
                                            targetDate = goal.date,
                                            notes = goal.notes,
                                            icon = goal.icon
                                    )
                            )
                        } else {
                            viewModel.addGoal(
                                    goal.title,
                                    goal.location,
                                    goal.date,
                                    goal.notes,
                                    goal.icon
                            )
                        }
                        showGoalDialog = false
                        editingGoal = null
                    }
            )
        }

        if (detailEntry != null) {
            val trackPoints by
                    if (detailEntry != null) {
                        val start =
                                detailEntry!!
                                        .happenedOn
                                        .atStartOfDay(java.time.ZoneOffset.UTC)
                                        .toInstant()
                                        .toEpochMilli()
                        val end = start + 86400000L
                        viewModel.getTrackPoints(start, end).collectAsState(initial = emptyList())
                    } else {
                        remember {
                            mutableStateOf(emptyList<com.footprint.data.local.TrackPointEntity>())
                        }
                    }

            FootprintDetailScreen(
                    entry = detailEntry!!,
                    trackPoints = trackPoints,
                    onBack = { detailEntry = null },
                    onEdit = {
                        editingEntry = it
                        detailEntry = null
                    },
                    onUpdateEntry = viewModel::updateFootprint
            )
        }
    }
}

enum class FootprintTab(
        val route: String,
        val label: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Dashboard("dashboard", "概览", Icons.Outlined.Dashboard),
    Map("map", "地图", Icons.Outlined.Map),
    Timeline("timeline", "足迹簿", Icons.Outlined.CalendarMonth),
    Planner("planner", "目标", Icons.Outlined.CheckCircle);

    companion object {
        val entries = values().toList()
    }
}
