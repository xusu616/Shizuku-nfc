package org.shizuku.nfc.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.shizuku.nfc.ui.data.ColorBag

/**
 * 依据壁纸提取的色彩包为 Material 控件提供自适应配色，完全对齐 shizuku-ftp/WallpaperControlsTheme
 */
@Composable
internal fun WallpaperControlsTheme(colorBag: ColorBag, content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val accent = if (dark) colorBag.vibrant else colorBag.muted
    val onAccent = if (dark) colorBag.darkMuted else colorBag.lightMuted
    val colors = if (colorBag.useM3Color) base else base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = colorBag.vibrant,
        onPrimaryContainer = colorBag.darkMuted,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = colorBag.vibrant,
        onSecondaryContainer = colorBag.darkMuted,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = colorBag.lightMuted,
        onTertiaryContainer = colorBag.darkMuted,
        surfaceTint = accent,
    )
    MaterialTheme(colorScheme = colors, content = content)
}
