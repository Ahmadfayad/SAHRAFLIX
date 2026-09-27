package com.sahraflix.presentation.theme

import androidx.compose.ui.graphics.Color

// ── Core backgrounds ─────────────────────────────────────────────────────────
val VoidBlack     = Color(0xFF07080F)
val AbyssBlue     = Color(0xFF0C1120)
val InkCard       = Color(0xFF141C2E)
val SlateElevated = Color(0xFF1E2640)
val SlateDivider  = Color(0xFF2A3350)

// ── Brand crimson ─────────────────────────────────────────────────────────────
val NebulaCrimson = Color(0xFFE5192E)
val CrimsonLight  = Color(0xFFFF3A50)
val CrimsonDim    = Color(0xFF7A0A16)

// ── Accent ────────────────────────────────────────────────────────────────────
val AuroraViolet  = Color(0xFFA855F7)
val NeonTeal      = Color(0xFF22D3EE)

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFF0F2F8)
val TextSecondary = Color(0xFF8A93A8)
val TextMuted     = Color(0xFF4A5068)

// ── Semantic ─────────────────────────────────────────────────────────────────
val FocusRingColor = NebulaCrimson.copy(alpha = 0.55f)
val OverlayScrim   = VoidBlack.copy(alpha = 0.82f)
val CardGloss      = Color(0x0DFFFFFF)

// ── Legacy aliases — keeps unported files compiling ───────────────────────────
val SahraGold         = NebulaCrimson
val EmbersGlow        = CrimsonLight
val CinematicCharcoal = AbyssBlue
val SurfaceCard       = InkCard
val DeepShadow        = VoidBlack
val CinemaWhite       = TextPrimary
val MutedSilver       = TextSecondary
val ScrimOverlay      = OverlayScrim
val GoldWash          = NebulaCrimson.copy(alpha = 0.10f)
val FocusGlowColor    = CrimsonLight.copy(alpha = 0.40f)
val SahraRed          = NebulaCrimson
val CinematicBlack    = VoidBlack
val DarkSlateBg       = AbyssBlue
