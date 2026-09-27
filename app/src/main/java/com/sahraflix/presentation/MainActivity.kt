package com.sahraflix.presentation

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Text
import com.sahraflix.player.PlaybackActionReceiver
import com.sahraflix.presentation.components.sahraCinematicGradient
import com.sahraflix.presentation.navigation.AppNavigation
import com.sahraflix.presentation.player.LocalPlayerViewModel
import com.sahraflix.presentation.player.PlayerScreen
import com.sahraflix.presentation.player.PlayerViewModel
import com.sahraflix.presentation.profile.ProfileGateScreen
import com.sahraflix.presentation.profile.ProfileViewModel
import com.sahraflix.presentation.settings.SettingsViewModel
import com.sahraflix.presentation.settings.UiMode
import com.sahraflix.presentation.theme.SahraGold
import com.sahraflix.presentation.theme.SahraflixTheme
import com.sahraflix.presentation.onboarding.SplashModeScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    /** Activity-scoped and shared with every screen via LocalPlayerViewModel. */
    private val playerViewModel: PlayerViewModel by viewModels()
    private val inPip = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val profileViewModel: ProfileViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val playerVisible by playerViewModel.isPlayerVisible.collectAsState()
            val resolving by playerViewModel.isResolving.collectAsState()
            val message by playerViewModel.message.collectAsState()
            val unlocked by profileViewModel.isUnlocked.collectAsState()
            val uiMode by settingsViewModel.uiMode.collectAsState()
            val modeChosen by settingsViewModel.modeChosen.collectAsState()
            val widthClass = calculateWindowSizeClass(this)
            val isTv = when (uiMode) {
                UiMode.TV -> true
                UiMode.MOBILE -> false
                UiMode.AUTO -> widthClass.widthSizeClass == WindowWidthSizeClass.Expanded ||
                    packageManager.hasSystemFeature("android.software.leanback")
            }
            
            LaunchedEffect(isTv) {
                if (isTv) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }

            CompositionLocalProvider(LocalUiEnvironment provides UiEnvironment(isTv, widthClass)) {
                SahraflixTheme {
                    CompositionLocalProvider(LocalPlayerViewModel provides playerViewModel) {
                    Box(Modifier.fillMaxSize().sahraCinematicGradient()) {
                        when {
                            !modeChosen -> {
                                SplashModeScreen { mode -> 
                                    settingsViewModel.setUiMode(mode) 
                                }
                            }
                            !unlocked -> {
                                ProfileGateScreen(viewModel = profileViewModel)
                            }
                            else -> {
                                // Navigation stays composed underneath so screen state survives the player.
                                if (!inPip.value) AppNavigation()
                                if (playerVisible) {
                                    PlayerScreen(playerViewModel, isInPip = inPip.value)
                                    BackHandler { playerViewModel.closePlayer() }
                                }
                            }
                        }
                        
                        if (resolving && !playerVisible) {
                            CircularProgressIndicator(color = SahraGold, modifier = Modifier.align(Alignment.Center))
                        }
                        message?.let { text ->
                            Text(
                                text,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                                    .background(Color(0xE6202530), androidx.tv.material3.MaterialTheme.shapes.small)
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                            LaunchedEffect(text) { delay(4_000); playerViewModel.clearMessage() }
                        }
                    }
                }
            }
        }
    }
}

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // PiP APIs are API 26+; minSdk is 23 (the old code called them unconditionally).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            playerViewModel.isPlayerVisible.value && playerViewModel.player.player.isPlaying &&
            packageManager.hasSystemFeature("android.software.picture_in_picture")
        ) {
            runCatching { enterPictureInPictureMode(pipParams()) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun pipParams(): PictureInPictureParams {
        fun action(code: Int, act: String, icon: Int, title: String) = RemoteAction(
            Icon.createWithResource(this, icon), title, title,
            PendingIntent.getBroadcast(this, code, Intent(this, PlaybackActionReceiver::class.java).setAction(act),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(listOf(
                action(1, PlaybackActionReceiver.ACTION_PLAY_PAUSE, android.R.drawable.ic_media_pause, "Play/Pause"),
                action(2, PlaybackActionReceiver.ACTION_CLOSE, android.R.drawable.ic_menu_close_clear_cancel, "Close")
            ))
            .build()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip.value = isInPictureInPictureMode
        // Closing the PiP window stops playback instead of leaving audio running invisibly.
        if (!isInPictureInPictureMode && !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            playerViewModel.closePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        // Not in PiP and leaving the app: pause (live keeps its session; VOD progress is saved).
        if (!inPip.value) playerViewModel.player.player.pause()
    }
}
