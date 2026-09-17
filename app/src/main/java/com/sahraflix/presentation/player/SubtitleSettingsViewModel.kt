package com.sahraflix.presentation.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahraflix.data.local.secureUserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SubtitleSettings(
    val fontSize: String = "Normal",
    val textColor: Int = android.graphics.Color.WHITE,
    val edgeColor: Int = android.graphics.Color.BLACK,
    val backgroundOpacity: Int = 0
)

@HiltViewModel
class SubtitleSettingsViewModel @Inject constructor(
    @ApplicationContext context: Context
) : ViewModel() {
    private val preferences = context.secureUserPreferences()
    val settings: StateFlow<SubtitleSettings> = preferences
        .observeString("subtitle_settings", "Normal|WHITE|BLACK|0")
        .map { value ->
            val parts = value.split('|')
            SubtitleSettings(
                fontSize = parts.getOrNull(0) ?: "Normal",
                textColor = parts.getOrNull(1)?.toColor(android.graphics.Color.WHITE) ?: android.graphics.Color.WHITE,
                edgeColor = parts.getOrNull(2)?.toColor(android.graphics.Color.BLACK) ?: android.graphics.Color.BLACK,
                backgroundOpacity = parts.getOrNull(3)?.toIntOrNull() ?: 0
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubtitleSettings())

    fun setFontSize(value: String) = save(value, settings.value.textColor, settings.value.edgeColor, settings.value.backgroundOpacity)
    fun setBackgroundOpacity(value: Int) = save(settings.value.fontSize, settings.value.textColor, settings.value.edgeColor, value)
    fun setTextColor(value: Int) = save(settings.value.fontSize, value, settings.value.edgeColor, settings.value.backgroundOpacity)
    fun setEdgeColor(value: Int) = save(settings.value.fontSize, settings.value.textColor, value, settings.value.backgroundOpacity)

    private fun save(fontSize: String, textColor: Int, edgeColor: Int, opacity: Int) {
        preferences.putString("subtitle_settings", "$fontSize|${textColor.toHex()}|${edgeColor.toHex()}|$opacity")
    }

    private fun String.toColor(default: Int): Int = runCatching { android.graphics.Color.parseColor(this) }.getOrDefault(default)
    private fun Int.toHex(): String = String.format("#%08X", this)
}
