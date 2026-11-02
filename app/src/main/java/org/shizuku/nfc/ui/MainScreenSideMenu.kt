package org.shizuku.nfc.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.shizuku.nfc.R

/**
 * 链接与工具侧抽屉（完全对齐 shizuku-ftp/LinkSideMenu）
 * 包含独立的日志控制台、NDEF 负载配置、引导与更新
 */
@Composable
fun LinkSideMenu(
    visible: Boolean,
    onMenuClick: (String) -> Unit,
    backdrop: GlassBackdrop,
    onClick: () -> Unit,
    colorBag: ColorBag = ColorBag(),
    selectedRoute: String? = null,
) {
    Box {
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300),
            ) + fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            GlassSidebarBox(
                backdrop = backdrop,
                showWallpaper = true,
                onDismiss = onClick,
                content = {
                    Text(
                        text = "功能与工具",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(
                        onClick = onClick,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colorBag.useM3Color) MaterialTheme.colorScheme.surfaceVariant else colorBag.vibrant,
                            contentColor = if (colorBag.useM3Color) MaterialTheme.colorScheme.onSurfaceVariant else colorBag.darkMuted
                        )
                    ) {
                        Text("关闭")
                    }

                    // 1. 独立日志控制台入口
                    RowClick(
                        icon = R.drawable.outline_dialogs_24,
                        text = "系统注入控制台",
                        selected = selectedRoute == "logs",
                        colorBag = colorBag,
                        onClick = { onMenuClick("logs") }
                    )

                    // 2. 独立 NDEF 负载配置入口
                    RowClick(
                        icon = R.drawable.connectsetting,
                        text = "NDEF 目标负载",
                        selected = selectedRoute == "payload",
                        colorBag = colorBag,
                        onClick = { onMenuClick("payload") }
                    )

                    // 3. 快速入门引导
                    RowClick(
                        icon = R.drawable.refresh,
                        text = "快速入门引导",
                        selected = selectedRoute == "guide",
                        colorBag = colorBag,
                        onClick = { onMenuClick("guide") }
                    )

                    // 4. 检查版本更新
                    RowClick(
                        icon = R.drawable.outline_cloud_download_24,
                        text = "检查版本更新",
                        selected = selectedRoute == "update",
                        colorBag = colorBag,
                        onClick = { onMenuClick("update") }
                    )
                }
            )
        }
    }
}

/**
 * 设置与系统侧抽屉（完全对齐 shizuku-ftp/GearSideMenu）
 */
@Composable
fun GearSideMenu(
    visible: Boolean,
    onMenuClick: (String) -> Unit,
    backdrop: GlassBackdrop,
    onClick: () -> Unit,
    colorBag: ColorBag = ColorBag(),
    selectedRoute: String? = null,
) {
    Box {
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300),
            ) + fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            GlassSidebarBox(
                backdrop = backdrop,
                showWallpaper = true,
                onDismiss = onClick,
                content = {
                    Text(
                        text = "设置与状态",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Button(
                        onClick = onClick,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (colorBag.useM3Color) MaterialTheme.colorScheme.surfaceVariant else colorBag.vibrant,
                            contentColor = if (colorBag.useM3Color) MaterialTheme.colorScheme.onSurfaceVariant else colorBag.darkMuted
                        )
                    ) {
                        Text("关闭")
                    }

                    // 1. Shizuku 特权状态
                    RowClick(
                        icon = R.drawable.outline_fingerprint_24,
                        text = "Shizuku 特权状态",
                        selected = selectedRoute == "shizuku",
                        colorBag = colorBag,
                        onClick = { onMenuClick("shizuku") }
                    )

                    // 2. 自定义桌面壁纸
                    RowClick(
                        icon = R.drawable.cleaner,
                        text = "更换主屏壁纸",
                        selected = selectedRoute == "wallpaper",
                        colorBag = colorBag,
                        onClick = { onMenuClick("wallpaper") }
                    )

                    // 3. 关于与致谢
                    RowClick(
                        icon = R.drawable.outline_info_24,
                        text = "关于 Shizuku-nfc",
                        selected = selectedRoute == "about",
                        colorBag = colorBag,
                        onClick = { onMenuClick("about") }
                    )
                }
            )
        }
    }
}
