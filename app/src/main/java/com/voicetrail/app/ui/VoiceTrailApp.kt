package com.voicetrail.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicetrail.app.ui.detail.EntryDetailScreen
import com.voicetrail.app.ui.navigation.Screen
import com.voicetrail.app.ui.record.RecordScreen
import com.voicetrail.app.ui.record.RecordViewModel
import com.voicetrail.app.ui.search.SearchScreen
import com.voicetrail.app.ui.search.SearchViewModel
import com.voicetrail.app.ui.timeline.TimelineScreen
import com.voicetrail.app.ui.timeline.TimelineViewModel
import com.voicetrail.app.ui.viewmodel.AppViewModelFactory

private data class TopLevelDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Screen.Record, "Record", Icons.Rounded.Mic),
    TopLevelDestination(Screen.Timeline, "Timeline", Icons.Rounded.CalendarMonth),
    TopLevelDestination(Screen.Search, "Search", Icons.Rounded.Search)
)

@Composable
fun VoiceTrailApp() {
    val container = LocalAppContainer.current
    val viewModelFactory = remember(container) {
        AppViewModelFactory(container)
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == destination.screen.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.screen.route) {
                                popUpTo(Screen.Record.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Record.route
        ) {
            composable(Screen.Record.route) {
                val recordViewModel: RecordViewModel = viewModel(factory = viewModelFactory)
                RecordScreen(
                    innerPadding = innerPadding,
                    viewModel = recordViewModel,
                    onOpenEntry = { entryId ->
                        navController.navigate(Screen.Detail.createRoute(entryId))
                    }
                )
            }

            composable(Screen.Timeline.route) {
                val timelineViewModel: TimelineViewModel = viewModel(factory = viewModelFactory)
                TimelineScreen(
                    innerPadding = innerPadding,
                    viewModel = timelineViewModel,
                    onOpenEntry = { entryId ->
                        navController.navigate(Screen.Detail.createRoute(entryId))
                    }
                )
            }

            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = viewModel(factory = viewModelFactory)
                SearchScreen(
                    innerPadding = innerPadding,
                    viewModel = searchViewModel,
                    onOpenEntry = { entryId ->
                        navController.navigate(Screen.Detail.createRoute(entryId))
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument(Screen.Detail.ARG_ENTRY_ID) {
                        type = NavType.LongType
                    }
                )
            ) { entry ->
                val entryId = entry.arguments?.getLong(Screen.Detail.ARG_ENTRY_ID) ?: return@composable
                EntryDetailScreen(
                    innerPadding = innerPadding,
                    entryId = entryId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

