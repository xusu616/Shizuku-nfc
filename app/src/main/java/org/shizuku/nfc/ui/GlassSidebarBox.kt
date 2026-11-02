package org.shizuku.nfc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

val GlassSideMenuWidth = 320.dp

/**
 * 经典毛玻璃抽屉侧栏（复刻 shizuku-ftp/GlassSidebarBox 架构）
 * 点击蒙层关闭，平滑弹性滑入/滑出。
 */
@Composable
fun GlassSidebarBox(
    visible: Boolean,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 黑色半透明遮罩蒙层
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(260)),
            exit = fadeOut(tween(260))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // 滑动毛玻璃抽屉
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(tween(200)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(250)
            ) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .width(GlassSideMenuWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                    .then(
                        if (hazeState != null) {
                            Modifier.glassHaze(
                                hazeState = hazeState,
                                blurIntensity = 25f,
                                darkTint = Color.Black.copy(alpha = 0.25f),
                                lightTint = Color.White.copy(alpha = 0.25f)
                            )
                        } else {
                            Modifier
                        }
                    )
                    // 阻断抽屉内部点击穿透到外部蒙层
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    content = content
                )
            }
        }
    }
}
