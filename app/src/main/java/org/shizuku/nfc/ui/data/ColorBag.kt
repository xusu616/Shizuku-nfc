package org.shizuku.nfc.ui.data

import androidx.compose.ui.graphics.Color

data class ColorBag(
    val vibrant: Color = Color(0xFF90CAF9),
    val darkMuted: Color = Color(0xFF1E3A5F),
    val lightMuted: Color = Color(0xFFD1E4FA),
    val muted: Color = Color(0xFF64B5F6),
    val useM3Color: Boolean = false,
)
