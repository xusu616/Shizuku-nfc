package org.shizuku.nfc.ui.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.scale
import androidx.palette.graphics.Palette
import kotlin.math.min

/**
 * 壁纸取色工具，使用 Palette 从 ImageBitmap 中安全提取主题色
 */
object ImagePrefHandler {

    fun extractSync(bitmap: ImageBitmap, fallback: Color): Color {
        val android = bitmap.asAndroidBitmap()
        if (android.isRecycled) return fallback
        val sampled = scaleDown(android, maxSize = 192)
        val p = Palette.from(sampled).maximumColorCount(16).generate()
        val dominant = p.swatches.maxByOrNull { it.population }?.rgb
        val argb = p.getVibrantColor(dominant ?: fallback.toArgb())
        return Color(argb)
    }

    fun extractAsync(
        bitmap: ImageBitmap,
        fallback: Color,
        onResult: (Color) -> Unit
    ) {
        val android = bitmap.asAndroidBitmap()
        if (android.isRecycled) {
            onResult(fallback)
            return
        }
        val sampled = scaleDown(android, maxSize = 192)
        Palette.from(sampled)
            .maximumColorCount(16)
            .generate { palette ->
                val dominant = palette?.swatches?.maxByOrNull { it.population }?.rgb
                val argb = palette?.getVibrantColor(dominant ?: fallback.toArgb())
                    ?: dominant
                    ?: fallback.toArgb()
                onResult(Color(argb))
            }
    }

    fun extractDarkMutedColor(bitmap: ImageBitmap, fallback: Color): Color {
        val android = bitmap.asAndroidBitmap()
        if (android.isRecycled) return fallback
        val sampled = scaleDown(android, maxSize = 192)
        val p = Palette.from(sampled).maximumColorCount(16).generate()
        val dominant = p.swatches.maxByOrNull { it.population }?.rgb
        val argb = p.getDarkMutedColor(dominant ?: fallback.toArgb())
        return Color(argb)
    }

    fun extractDarkMutedColorAsync(
        bitmap: ImageBitmap,
        fallback: Color,
        onResult: (Color) -> Unit
    ) {
        val android = bitmap.asAndroidBitmap()
        if (android.isRecycled) {
            onResult(fallback)
            return
        }
        val sampled = scaleDown(android, maxSize = 192)
        Palette.from(sampled)
            .maximumColorCount(16)
            .generate { palette ->
                val dominant = palette?.swatches?.maxByOrNull { it.population }?.rgb
                val argb = palette?.getDarkMutedColor(dominant ?: fallback.toArgb())
                    ?: dominant
                    ?: fallback.toArgb()
                onResult(Color(argb))
            }
    }

    fun extractLightVibrantColor(bitmap: ImageBitmap, fallback: Color): Color {
        val android = bitmap.asAndroidBitmap()
        if (android.isRecycled) return fallback
        val sampled = scaleDown(android, maxSize = 192)
        val p = Palette.from(sampled).maximumColorCount(16).generate()
        val dominant = p.swatches.maxByOrNull { it.population }?.rgb
        val argb = p.getLightVibrantColor(dominant ?: fallback.toArgb())
        return Color(argb)
    }

    fun extractLightMutedColorAsync(
        bitmap: ImageBitmap,
        fallback: Color,
        onResult: (Color) -> Unit
    ) {
        val android = bitmap.asAndroidBitmap()
        if (android.isRecycled) {
            onResult(fallback)
            return
        }
        val sampled = scaleDown(android, maxSize = 192)
        Palette.from(sampled)
            .maximumColorCount(16)
            .generate { palette ->
                val dominant = palette?.swatches?.maxByOrNull { it.population }?.rgb
                val argb = palette?.getLightMutedColor(dominant ?: fallback.toArgb())
                    ?: dominant
                    ?: fallback.toArgb()
                onResult(Color(argb))
            }
    }

    fun extractMutedColorAsync(
        bitmap: ImageBitmap,
        fallback: Color,
        onResult: (Color) -> Unit
    ) {
        val android = bitmap.asAndroidBitmap()
        if (android.isRecycled) {
            onResult(fallback)
            return
        }
        val sampled = scaleDown(android, maxSize = 192)
        Palette.from(sampled)
            .maximumColorCount(16)
            .generate { palette ->
                val dominant = palette?.swatches?.maxByOrNull { it.population }?.rgb
                val argb = palette?.getMutedColor(dominant ?: fallback.toArgb())
                    ?: dominant
                    ?: fallback.toArgb()
                onResult(Color(argb))
            }
    }

    private fun scaleDown(src: Bitmap, maxSize: Int): Bitmap {
        val ratio = min(maxSize.toFloat() / src.width, maxSize.toFloat() / src.height)
        if (ratio >= 1f) return src
        return src.scale(
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1)
        )
    }
}
