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

object AppRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val CATALOG_DETAIL = "catalog_detail/{entryId}"
    const val EPG_GUIDE = "epg_guide"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = AppRoutes.HOME) {
        composable(AppRoutes.HOME) { HomeScreen() }
        composable(AppRoutes.SEARCH) { SearchScreen(onOpenDetail = { id -> navController.navigate("catalog_detail/$id") }) }
        composable(AppRoutes.EPG_GUIDE) { EpgGuideScreen() }
        composable(AppRoutes.SETTINGS) { SettingsScreen() }
        composable(AppRoutes.CATALOG_DETAIL) { entry ->
            CatalogDetailScreen()
        }
        composable(AppRoutes.PLAYER) { HomeScreen() }
    }
}
