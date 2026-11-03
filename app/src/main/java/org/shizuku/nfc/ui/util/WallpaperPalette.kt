package org.shizuku.nfc.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import org.shizuku.nfc.ui.data.WallpaperColorEnum

data class WallpaperPalette(
    val bitmap: ImageBitmap? = null,
    val pickImageColor: Boolean = true,
)

@Composable
fun rememberWallpaperAccentColor(
    palette: WallpaperPalette,
    type: WallpaperColorEnum = WallpaperColorEnum.VIBRANT
): Color {
    val fallback = MaterialTheme.colorScheme.secondaryContainer
    when (type) {
        WallpaperColorEnum.VIBRANT -> {
            if (LocalInspectionMode.current) {
                val bmp = palette.bitmap ?: return fallback
                return if (palette.pickImageColor) ImagePrefHandler.extractSync(bmp, fallback) else fallback
            }
            var color by remember(palette.bitmap) { mutableStateOf(fallback) }
            LaunchedEffect(palette.bitmap) {
                val bmp = palette.bitmap ?: return@LaunchedEffect
                ImagePrefHandler.extractAsync(bmp, fallback) { color = it }
            }
            return if (palette.pickImageColor) color else fallback
        }
        WallpaperColorEnum.DARK_MUTED -> {
            if (LocalInspectionMode.current) {
                val bmp = palette.bitmap ?: return fallback
                return if (palette.pickImageColor) ImagePrefHandler.extractDarkMutedColor(bmp, fallback) else fallback
            }
            var color by remember(palette.bitmap) { mutableStateOf(fallback) }
            LaunchedEffect(palette.bitmap) {
                val bmp = palette.bitmap ?: return@LaunchedEffect
                ImagePrefHandler.extractDarkMutedColorAsync(bmp, fallback) { color = it }
            }
            return if (palette.pickImageColor) color else fallback
        }
        WallpaperColorEnum.LIGHT_MUTED -> {
            var color by remember(palette.bitmap) { mutableStateOf(fallback) }
            LaunchedEffect(palette.bitmap) {
                val bmp = palette.bitmap ?: return@LaunchedEffect
                ImagePrefHandler.extractLightMutedColorAsync(bmp, fallback) { color = it }
            }
            return if (palette.pickImageColor) color else fallback
        }
        WallpaperColorEnum.MUTED -> {
            var color by remember(palette.bitmap) { mutableStateOf(fallback) }
            LaunchedEffect(palette.bitmap) {
                val bmp = palette.bitmap ?: return@LaunchedEffect
                ImagePrefHandler.extractMutedColorAsync(bmp, fallback) { color = it }
            }
            return if (palette.pickImageColor) color else fallback
        }
    }
}
