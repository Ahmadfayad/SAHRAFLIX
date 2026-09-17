package com.sahraflix.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sahraflix.presentation.epg.EpgGuideScreen
import com.sahraflix.presentation.home.HomeScreen
import com.sahraflix.presentation.search.SearchScreen
import com.sahraflix.presentation.settings.SettingsScreen
import com.sahraflix.presentation.catalog.CatalogDetailScreen
import com.sahraflix.presentation.player.SubtitleSettingsScreen
import com.sahraflix.presentation.pairing.PairingScreen
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState

object AppRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CATALOG_DETAIL = "catalog_detail/{entryId}"
    const val EPG_GUIDE = "epg_guide"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val SUBTITLES = "subtitles"
    const val PAIRING = "pairing"
}

@Composable
fun AppNavigation(isTvMode: Boolean = true) {
    val navController = rememberNavController()
    Crossfade(targetState = isTvMode, label = "ui-mode-transition") {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val destinations = listOf(
        AppRoutes.HOME to "Home",
        AppRoutes.SEARCH to "Search",
        AppRoutes.EPG_GUIDE to "Guide",
        AppRoutes.SETTINGS to "Settings",
        AppRoutes.PAIRING to "Pair"
    )
    Column(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.weight(1f)) {
    if (isTvMode) {
        NavigationRail {
            destinations.forEach { (route, label) ->
                NavigationRailItem(
                    selected = currentRoute == route,
                    onClick = { navController.navigate(route) { launchSingleTop = true } },
                    icon = { Text(label.take(1)) }
                )
            }
        }
    }
    NavHost(modifier = Modifier.weight(1f), navController = navController, startDestination = AppRoutes.HOME) {
        composable(AppRoutes.HOME) { HomeScreen() }
        composable(AppRoutes.SEARCH) { SearchScreen(onOpenDetail = { id -> navController.navigate("catalog_detail/$id") }) }
        composable(AppRoutes.EPG_GUIDE) { EpgGuideScreen() }
        composable(AppRoutes.SETTINGS) { SettingsScreen() }
        composable(AppRoutes.SUBTITLES) { SubtitleSettingsScreen() }
        composable(AppRoutes.PAIRING) { PairingScreen() }
        composable(AppRoutes.CATALOG_DETAIL) { entry ->
            CatalogDetailScreen()
        }
        composable(AppRoutes.PLAYER) { HomeScreen() }
    }
    }
    if (!isTvMode) {
        NavigationBar {
            destinations.forEach { (route, label) ->
                NavigationBarItem(
                    selected = currentRoute == route,
                    onClick = { navController.navigate(route) { launchSingleTop = true } },
                    icon = { Text(label.take(1)) },
                    label = { Text(label) }
                )
            }
        }
    }
    }
    }
}
