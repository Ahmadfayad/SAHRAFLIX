package com.sahraflix.presentation.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.sahraflix.data.local.userPreferences
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
    private val refreshKey = booleanPreferencesKey("background_refresh_enabled")
    val refreshEnabled: StateFlow<Boolean> = context.userPreferences.data
        .map { it[refreshKey] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setRefreshEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.userPreferences.edit { it[refreshKey] = enabled }
        }
    }
}
