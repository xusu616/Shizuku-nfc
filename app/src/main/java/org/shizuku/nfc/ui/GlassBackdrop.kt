package org.shizuku.nfc.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import dev.chrisbanes.haze.HazeState

/**
 * 主界面持有固定尺寸的玻璃背景，侧栏只绘制与自身位置对应的区域。
 * 参考 shizuku-ftp 提交 50a87c25 ("fix the blink of glassMenu in the table (2)")
 * 彻底消除抽屉展开时的重绘闪烁与着色器重载。
 */
@Stable
class GlassBackdrop(val layer: GraphicsLayer) {
    var positionInRoot by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

@Composable
fun CaptureGlassBackdrop(
    backdrop: GlassBackdrop,
    hazeState: HazeState,
    blurIntensity: Float? = 25f,
    experimentalHaze: Boolean = false,
) {
    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { backdrop.positionInRoot = it.positionInRoot() }
            .drawWithContent {
                // 始终在主界面坐标中生成，不随侧栏滑动而裁短、重建模糊纹理。
                // 这里只记录；侧栏在窄栏控件和遮罩上方绘制这份背景。
                backdrop.layer.record { this@drawWithContent.drawContent() }
            }
            .glassHaze(
                hazeState = hazeState,
                blurIntensity = blurIntensity,
                weatherEnableExperimentalHazeOnLowVer = experimentalHaze
            )
    )
}

@Composable
fun Modifier.glassBackdrop(backdrop: GlassBackdrop): Modifier {
    var positionInRoot by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { positionInRoot = it.positionInRoot() }
        .drawBehind {
            val offset = backdrop.positionInRoot - positionInRoot
            translate(offset.x, offset.y) { drawLayer(backdrop.layer) }
        }
}
