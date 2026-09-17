package com.sahraflix.presentation.settings

import android.content.Context
import com.sahraflix.data.local.secureUserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val preferences = context.secureUserPreferences()
    val refreshEnabled: StateFlow<Boolean> = preferences
        .observeBoolean("background_refresh_enabled", true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val uiMode: StateFlow<UiMode> = preferences.observeString("ui_mode", UiMode.AUTO.name)
        .map { runCatching { UiMode.valueOf(it) }.getOrDefault(UiMode.AUTO) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiMode.AUTO)
    val hardwareAcceleration: StateFlow<Boolean> = preferences.observeBoolean("hardware_acceleration", true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val defaultPlayer: StateFlow<String> = preferences.observeString("default_player", "Media3")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Media3")
    val updateIntervalHours: StateFlow<Int> = preferences.observeString("update_interval_hours", "12")
        .map { it.toIntOrNull() ?: 12 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 12)

    fun setRefreshEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.putBoolean("background_refresh_enabled", enabled)
        }
    }

    fun setUiMode(mode: UiMode) {
        preferences.putString("ui_mode", mode.name)
    }

    fun setHardwareAcceleration(enabled: Boolean) = preferences.putBoolean("hardware_acceleration", enabled)
    fun cycleDefaultPlayer() = preferences.putString("default_player", if (defaultPlayer.value == "Media3") "External" else "Media3")
    fun cycleUpdateInterval() = preferences.putString("update_interval_hours", if (updateIntervalHours.value == 12) "24" else "12")
    fun clearCache() {
        context.cacheDir.deleteRecursively()
    }
}
