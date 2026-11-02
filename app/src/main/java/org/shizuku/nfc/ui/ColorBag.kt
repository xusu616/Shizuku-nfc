package org.shizuku.nfc.ui

import androidx.compose.ui.graphics.Color

data class ColorBag(
    val vibrant: Color = Color(0xFF81C784),
    val darkMuted: Color = Color(0xFF2E7D32),
    val lightMuted: Color = Color(0xFFA5D6A7),
    val muted: Color = Color(0xFF4CAF50),
    val useM3Color: Boolean = true
)
