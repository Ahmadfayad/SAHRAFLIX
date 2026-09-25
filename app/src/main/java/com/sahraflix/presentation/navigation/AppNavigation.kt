package com.sahraflix.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.presentation.detail.DetailScreen
import com.sahraflix.presentation.epg.EpgGuideScreen
import com.sahraflix.presentation.home.HomeScreen
import com.sahraflix.presentation.pairing.PairingScreen
import com.sahraflix.presentation.player.SubtitleSettingsScreen
import com.sahraflix.presentation.search.SearchScreen
import com.sahraflix.presentation.settings.SettingsScreen
import java.net.URLEncoder

object AppRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val GUIDE = "guide"
    const val SETTINGS = "settings"
    const val PAIR = "pair"
    const val SUBTITLES = "subtitles"
    const val DETAIL = "detail/{id}"
    fun detail(entry: CatalogEntry) = "detail/" + URLEncoder.encode(entry.id, "UTF-8")
}

private data class Dest(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val destinations = listOf(
    Dest(AppRoutes.HOME, "Home", Icons.Default.Home),
    Dest(AppRoutes.SEARCH, "Search", Icons.Default.Search),
    Dest(AppRoutes.GUIDE, "Guide", Icons.Default.LiveTv),
    Dest(AppRoutes.SETTINGS, "Settings", Icons.Default.Settings)
)

@Composable
fun AppNavigation(isTvMode: Boolean) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val go: (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f)) {
            if (isTvMode) NavigationRail {
                destinations.forEach { d ->
                    NavigationRailItem(selected = current == d.route, onClick = { go(d.route) },
                        icon = { Icon(d.icon, d.label) }, label = { Text(d.label) })
                }
            }
            Graph(nav, Modifier.weight(1f))
        }
        if (!isTvMode) NavigationBar {
            destinations.forEach { d ->
                NavigationBarItem(selected = current == d.route, onClick = { go(d.route) },
                    icon = { Icon(d.icon, d.label) }, label = { Text(d.label) })
            }
        }
    }
}

@Composable
private fun Graph(nav: NavHostController, modifier: Modifier) {
    val openDetail: (CatalogEntry) -> Unit = { nav.navigate(AppRoutes.detail(it)) }
    NavHost(nav, startDestination = AppRoutes.HOME, modifier = modifier) {
        composable(AppRoutes.HOME) { HomeScreen(onOpenDetail = openDetail, onAddPlaylist = { nav.navigate(AppRoutes.SETTINGS) }) }
        composable(AppRoutes.SEARCH) { SearchScreen(onOpenDetail = openDetail) }
        composable(AppRoutes.GUIDE) { EpgGuideScreen() }
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(onPair = { nav.navigate(AppRoutes.PAIR) }, onSubtitles = { nav.navigate(AppRoutes.SUBTITLES) })
        }
        composable(AppRoutes.PAIR) { PairingScreen(onDone = { nav.popBackStack(AppRoutes.HOME, inclusive = false) }) }
        composable(AppRoutes.SUBTITLES) { SubtitleSettingsScreen() }
        composable(AppRoutes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { DetailScreen() }
    }
}
