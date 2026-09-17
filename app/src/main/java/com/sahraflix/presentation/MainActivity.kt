package com.sahraflix.presentation

import android.os.Bundle
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Rational
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
import com.sahraflix.presentation.player.PreviewSurface
import com.sahraflix.presentation.profile.ProfileGateScreen
import com.sahraflix.presentation.profile.ProfileViewModel
import com.sahraflix.presentation.settings.SettingsViewModel
import com.sahraflix.presentation.settings.UiMode
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import com.sahraflix.presentation.theme.SahraflixTheme
import com.sahraflix.presentation.components.sahraCinematicGradient
import dagger.hilt.android.AndroidEntryPoint
import com.sahraflix.player.PlaybackActionReceiver
import com.sahraflix.domain.repository.IptvVideoPlayer
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    @Inject lateinit var iptvPlayer: IptvVideoPlayer
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val playerViewModel: PlayerViewModel = hiltViewModel()
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isHomeVisible by playerViewModel.isHomeVisible.collectAsState()
            val isPreviewing by playerViewModel.isPreviewing.collectAsState()
            val isProfileUnlocked by profileViewModel.isUnlocked.collectAsState()
            val forcedMode by settingsViewModel.uiMode.collectAsState()
            val widthClass = calculateWindowSizeClass(this@MainActivity).widthSizeClass
            val isTvMode = when (forcedMode) {
                UiMode.TV -> true
                UiMode.MOBILE -> false
                UiMode.AUTO -> widthClass == WindowWidthSizeClass.Expanded
            }

            SahraflixTheme {
                Box(modifier = Modifier.fillMaxSize().sahraCinematicGradient()) {
                    if (isProfileUnlocked) {
                        if (isPreviewing) {
                            PreviewSurface(playerViewModel.player.player, Modifier.align(androidx.compose.ui.Alignment.TopEnd))
                        } else {
                            PlayerSurface(viewModel = playerViewModel, modifier = Modifier.fillMaxSize())
                        }
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isFinishing) return
        if (iptvPlayer.player.isPlaying) {
            val playPause = PendingIntent.getBroadcast(
                this, 1, Intent(this, PlaybackActionReceiver::class.java).setAction(PlaybackActionReceiver.ACTION_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val close = PendingIntent.getBroadcast(
                this, 2, Intent(this, PlaybackActionReceiver::class.java).setAction(PlaybackActionReceiver.ACTION_CLOSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(listOf(
                    RemoteAction(Icon.createWithResource(this, android.R.drawable.ic_media_pause), "Pause", "Pause", playPause),
                    RemoteAction(Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel), "Close", "Close", close)
                ))
                .build()
            enterPictureInPictureMode(params)
        }
    }
}
