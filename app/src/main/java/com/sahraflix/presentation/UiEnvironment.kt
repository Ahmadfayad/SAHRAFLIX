package com.sahraflix.presentation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.compositionLocalOf

data class UiEnvironment(
    val isTv: Boolean,
    val windowSizeClass: WindowSizeClass
)

val LocalUiEnvironment = compositionLocalOf<UiEnvironment> {
    error("No UiEnvironment provided")
}
