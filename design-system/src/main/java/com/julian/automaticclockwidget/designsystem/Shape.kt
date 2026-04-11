package com.julian.automaticclockwidget.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Roundedness = maximum: pill shapes for interactive elements, generously rounded for containers
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(50.dp),   // pill: chips, badges
    small = RoundedCornerShape(50.dp),         // pill: buttons, text fields
    medium = RoundedCornerShape(24.dp),        // cards
    large = RoundedCornerShape(28.dp),         // FABs, larger containers
    extraLarge = RoundedCornerShape(32.dp),    // bottom sheets, dialogs
)
