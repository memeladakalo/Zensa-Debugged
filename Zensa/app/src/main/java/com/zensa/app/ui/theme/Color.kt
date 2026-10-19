package com.zensa.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Zensa colour palette.
 *
 * Derived from the Zensa Planning & Design document:
 *   Primary    : Deep purple      — academic identity
 *   Secondary  : Soft lavender    — calm
 *   Accent     : Baby / soft pink — warm accent
 *   Background : White / light lavender
 *
 * Every screen must use these names. Never hard-code a hex value
 * inside a composable — always reference these constants.
 */

// ---------- Primary (Deep Purple) ----------
val ZensaPurple        = Color(0xFF6A4BAF)   // Main brand purple
val ZensaPurpleDark    = Color(0xFF4A2F85)   // Pressed / dark variant
val ZensaPurpleLight   = Color(0xFF9C7BD9)   // Highlight / secondary use

// ---------- Secondary (Soft Lavender) ----------
val ZensaLavender      = Color(0xFFD9CBF0)
val ZensaLavenderLight = Color(0xFFEFE8F9)   // Very light background lavender

// ---------- Accent (Baby / Soft Pink) ----------
val ZensaPink          = Color(0xFFF8C8DC)
val ZensaPinkDark      = Color(0xFFE8A6C0)

// ---------- Neutrals ----------
val ZensaWhite         = Color(0xFFFFFFFF)
val ZensaOffWhite      = Color(0xFFFAF8FD)   // Slightly purple-tinted white
val ZensaGrey          = Color(0xFF6E6A75)
val ZensaGreyLight     = Color(0xFFE0DEE3)
val ZensaBlack         = Color(0xFF1A1A1A)

// ---------- Semantic ----------
val ZensaSuccess       = Color(0xFF4CAF50)
val ZensaWarning       = Color(0xFFFFA726)
val ZensaError         = Color(0xFFD9534F)

// ---------- Priority colours (used by assignments) ----------
val PriorityLow        = Color(0xFF8BC34A)
val PriorityMedium     = Color(0xFFFFC107)
val PriorityHigh       = Color(0xFFFF9800)
val PriorityUrgent     = Color(0xFFE53935)
