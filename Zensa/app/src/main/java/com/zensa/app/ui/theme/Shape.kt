package com.zensa.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Zensa shape system.
 *
 * The design uses rounded cards and rounded buttons everywhere.
 * Defining them here means every component automatically matches.
 */
val ZensaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small       = RoundedCornerShape(10.dp),
    medium      = RoundedCornerShape(16.dp),   // cards
    large       = RoundedCornerShape(24.dp),   // large surfaces
    extraLarge  = RoundedCornerShape(32.dp)
)
