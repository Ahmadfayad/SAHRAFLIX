package com.sahraflix.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween

object SahraMotion {
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0f, 1f)
    val StandardEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)
    const val FocusExpandMs = 220
    const val HeroCrossfadeMs = 600
    const val RailSlideMs = 280
    const val SkeletonPulseMs = 1200
    const val SheetExpandMs = 320
    fun <T> focusExpand() = tween<T>(durationMillis = FocusExpandMs, easing = EmphasizedEasing)
    fun <T> heroCrossfade() = tween<T>(durationMillis = HeroCrossfadeMs)
    fun <T> railSlide() = tween<T>(durationMillis = RailSlideMs, easing = EmphasizedEasing)
}
