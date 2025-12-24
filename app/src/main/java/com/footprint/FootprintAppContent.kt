package com.footprint

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.footprint.ui.components.AddFootprintDialog
import com.footprint.ui.components.AddGoalDialog
import com.footprint.ui.screens.DashboardScreen
import com.footprint.ui.screens.GoalPlannerScreen
import com.footprint.ui.screens.MapScreen
import com.footprint.ui.screens.TimelineScreen

@Composable
fun FootprintApp(initialDestination: FootprintDestination = FootprintDestination.Dashboard) {
    val viewModel: FootprintViewModel = viewModel(factory = FootprintViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(initialDestination) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (destination != FootprintDestination.Map) {
                ExtendedFloatingActionButton(
                    onClick = { showEntryDialog = true },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text("记录足迹") }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                FootprintDestination.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = destination == tab,
                        onClick = { destination = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (destination) {
            FootprintDestination.Dashboard -> DashboardScreen(
                modifier = Modifier.padding(innerPadding),
                state = uiState,
                onSearch = viewModel::updateSearch,
                onYearShift = viewModel::shiftYear,
                onMoodSelected = viewModel::toggleMoodFilter,
                onCreateGoal = { showGoalDialog = true }
            )
            FootprintDestination.Map -> MapScreen()
            FootprintDestination.Timeline -> TimelineScreen(
                modifier = Modifier.padding(innerPadding),
                entries = uiState.visibleEntries,
                filterState = uiState.filterState,
                onMoodFilterChange = viewModel::toggleMoodFilter,
                onSearch = viewModel::updateSearch
            )
            FootprintDestination.Planner -> GoalPlannerScreen(
                modifier = Modifier.padding(innerPadding),
                goals = uiState.goals,
                summary = uiState.summary,
                onToggleGoal = viewModel::toggleGoal,
                onAddGoal = { showGoalDialog = true }
            )
        }
    }

    if (showEntryDialog) {
        AddFootprintDialog(
            onDismiss = { showEntryDialog = false },
            onSave = { payload ->
                viewModel.addFootprint(
                    title = payload.title,
                    location = payload.location,
                    detail = payload.detail,
                    mood = payload.mood,
                    tags = payload.tags,
                    distanceKm = payload.distance,
                    photos = emptyList(),
                    energyLevel = payload.energy,
                    date = payload.date
                )
                showEntryDialog = false
            }
        )
    }

    if (showGoalDialog) {
        AddGoalDialog(
            onDismiss = { showGoalDialog = false },
            onSave = { goal ->
                viewModel.addGoal(goal.title, goal.location, goal.date, goal.notes)
                showGoalDialog = false
            }
        )
    }
}

enum class FootprintDestination(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Dashboard("概览", Icons.Outlined.Dashboard),
    Map("地图", Icons.Outlined.Map),
    Timeline("足迹簿", Icons.Outlined.CalendarMonth),
    Planner("目标", Icons.Outlined.CheckCircle);

    companion object {
        val entries = values().toList()
    }
}
