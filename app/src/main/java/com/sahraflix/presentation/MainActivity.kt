package com.sahraflix.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahraflix.presentation.navigation.AppNavigation
import com.sahraflix.presentation.player.PlayerSurface
import com.sahraflix.presentation.player.PlayerViewModel
import com.sahraflix.presentation.profile.ProfileGateScreen
import com.sahraflix.presentation.profile.ProfileViewModel
import com.sahraflix.presentation.settings.SettingsViewModel
import com.sahraflix.presentation.settings.UiMode
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import com.sahraflix.presentation.theme.SahraflixTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val playerViewModel: PlayerViewModel = hiltViewModel()
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isHomeVisible by playerViewModel.isHomeVisible.collectAsState()
            val isProfileUnlocked by profileViewModel.isUnlocked.collectAsState()
            val forcedMode by settingsViewModel.uiMode.collectAsState()
            val widthClass = calculateWindowSizeClass(this@MainActivity).widthSizeClass
            val isTvMode = when (forcedMode) {
                UiMode.TV -> true
                UiMode.MOBILE -> false
                UiMode.AUTO -> widthClass == WindowWidthSizeClass.Expanded
            }

            SahraflixTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isProfileUnlocked) {
                        PlayerSurface(
                            viewModel = playerViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isHomeVisible) {
                            AppNavigation(isTvMode = isTvMode)
                        }
                    } else {
                        ProfileGateScreen(viewModel = profileViewModel)
                    }
                }
                BackHandler(enabled = !isHomeVisible) {
                    playerViewModel.showHome()
                }
            }
        }
    }
}
