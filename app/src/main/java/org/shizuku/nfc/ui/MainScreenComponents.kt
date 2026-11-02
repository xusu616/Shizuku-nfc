package org.shizuku.nfc.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect
import org.shizuku.nfc.R
import java.text.SimpleDateFormat
import java.util.*

fun <T> telegramSpringSpec() = spring<T>(
    dampingRatio = 0.5f,
    stiffness = Spring.StiffnessMediumLow
)

@Composable
fun MenuButton(
    iconRes: Int,
    rotation: Float = 0f,
    onClick: () -> Unit,
    colorBag: ColorBag = ColorBag(),
    useM3Color: Boolean = colorBag.useM3Color,
    contentDescription: String? = null
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (useM3Color) MaterialTheme.colorScheme.secondaryContainer else colorBag.vibrant)
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(28.dp)
                .rotate(rotation),
            colorFilter = ColorFilter.tint(
                if (useM3Color) MaterialTheme.colorScheme.onSecondaryContainer else colorBag.darkMuted
            )
        )
    }
}

/**
 * 侧栏列表点击行（复刻 shizuku-ftp/RowClick）
 */
@Composable
fun RowClick(
    icon: Any,
    text: String,
    onClick: () -> Unit,
    colorBag: ColorBag = ColorBag(),
    selected: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) {
                    if (colorBag.useM3Color) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    else colorBag.vibrant.copy(alpha = 0.20f)
                } else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (colorBag.useM3Color) MaterialTheme.colorScheme.primary else colorBag.vibrant),
            contentAlignment = Alignment.Center
        ) {
            when (icon) {
                is ImageVector -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (colorBag.useM3Color) MaterialTheme.colorScheme.onPrimary else colorBag.darkMuted,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                is Int -> {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = if (colorBag.useM3Color) MaterialTheme.colorScheme.onPrimary else colorBag.darkMuted,
                        modifier = Modifier.padding(9.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * 经典高光椭圆胶囊控制按钮（复刻 shizuku-ftp/ServerControlButton）
 */
@Composable
fun ServerControlButton(
    isExecuting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isExecuting) Color(0xFF29B6F6) else Color(0xFF81C784),
        label = "serverButtonColor",
        animationSpec = telegramSpringSpec()
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(width = 210.dp, height = 48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, buttonColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = buttonColor
        )
    ) {
        Text(
            text = if (isExecuting) "派发中..." else "碰一下 (TOUCH)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun VerticalTimeText(timeText: String) {
    val parts = timeText.split(":")
    val hour = parts.getOrElse(0) { "00" }
    val minute = parts.getOrElse(1) { "00" }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = hour,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = 4.dp)
        )
        Text(
            text = ":",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W900,
            fontSize = 28.sp,
            modifier = Modifier.rotate(90f).offset(y = (-2).dp)
        )
        Text(
            text = minute,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = (-6).dp)
        )
    }
}

@Composable
fun BatteryIcon(
    progress: Float,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    batteryColor: Color = Color.White,
    chargingColor: Color = Color.Green
) {
    Canvas(modifier = modifier.size(width = 48.dp, height = 16.dp).offset(x = 4.dp)) {
        val strokeWidth = 2.dp.toPx()
        val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

        // 电池头部凸起
        val headWidth = 5.dp.toPx()
        val headHeight = 6.dp.toPx()
        val headLeft = size.width - headWidth
        val headTop = (size.height - headHeight) / 2f
        drawRoundRect(
            color = batteryColor,
            topLeft = Offset(headLeft - 4, headTop),
            size = Size(headWidth, headHeight),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        // 电池外框
        val bodyWidth = size.width - headWidth - 4.dp.toPx()
        val bodyHeight = size.height
        drawRoundRect(
            color = batteryColor,
            topLeft = Offset(0f, 0f),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )

        // 内部填充
        val padding = 4.dp.toPx()
        val innerMaxWidth = bodyWidth - padding * 2
        val innerMaxHeight = bodyHeight - padding * 2
        val currentFillWidth = innerMaxWidth * progress.coerceIn(0f, 1f)

        val fillColor = if (isCharging) chargingColor else if (progress > 0.2f) batteryColor else Color(0xFFFF9800)
        if (currentFillWidth > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(padding, padding),
                size = Size(currentFillWidth, innerMaxHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}

@Composable
fun GlassBackgroundToggleButton(
    enabled: Boolean,
    onClick: () -> Unit,
    rotation: Float,
    colorBag: ColorBag = ColorBag(),
    useOriginColor: Boolean = colorBag.useM3Color
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (useOriginColor) MaterialTheme.colorScheme.secondaryContainer else colorBag.vibrant,
            contentColor = if (useOriginColor) MaterialTheme.colorScheme.onSecondaryContainer else colorBag.darkMuted
        )
    ) {
        Image(
            painter = painterResource(id = if (enabled) R.drawable.boldincenter else R.drawable.blodcantsee),
            contentDescription = if (enabled) "折叠侧边栏" else "展开侧边栏",
            modifier = Modifier.size(24.dp).rotate(rotation),
            colorFilter = ColorFilter.tint(
                if (useOriginColor) MaterialTheme.colorScheme.onSecondaryContainer else colorBag.darkMuted
            )
        )
    }
}

@Composable
fun ShowCardButton(
    onClick: () -> Unit,
    colorBag: ColorBag = ColorBag(),
    useM3Color: Boolean = colorBag.useM3Color
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        border = BorderStroke(1.dp, if (useM3Color) MaterialTheme.colorScheme.outline else colorBag.darkMuted)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.outline_data_alert_24),
            contentDescription = "查看权限与特权状态",
            modifier = Modifier.size(32.dp),
            tint = if (useM3Color) MaterialTheme.colorScheme.onSecondaryContainer else colorBag.darkMuted
        )
    }
}

@Composable
fun Modifier.glassHaze(
    hazeState: HazeState,
    blurIntensity: Float? = 25f,
    weatherEnableExperimentalHazeOnLowVer: Boolean = false,
    darkTint: Color = Color.Black.copy(alpha = 0.22f),
    lightTint: Color = Color.White.copy(alpha = 0.22f),
    darkFallback: Color = Color.Black.copy(alpha = 0.62f),
    lightFallback: Color = Color.White.copy(alpha = 0.82f),
): Modifier {
    val isDark = isSystemInDarkTheme()
    return this.hazeEffect(state = hazeState) {
        inputScale = HazeInputScale.Fixed(0.5f)
        blurEffect {
            blurEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || weatherEnableExperimentalHazeOnLowVer
            blurIntensity?.let { blurRadius = it.dp }
            noiseFactor = 0.06f
            colorEffects = listOf(HazeColorEffect.tint(if (isDark) darkTint else lightTint))
            fallbackTint = HazeColorEffect.tint(if (isDark) darkFallback else lightFallback)
        }
    }
}

@Composable
fun WallpaperBase(
    wallpaperBitmap: ImageBitmap? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.my_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // 轻微暗化保证文字和卡片可读性
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f))
        )
    }
}

@Composable
fun MainHeroLayout(
    wallpaperBitmap: ImageBitmap?,
    topBarPressedDown: Boolean = true,
    heroHeight: androidx.compose.ui.unit.Dp = 380.dp,
    onWallpaperLongPress: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useBackgroundHero = maxWidth > maxHeight && maxHeight < 1600.dp
        if (useBackgroundHero) {
            MainHeroImage(
                wallpaperBitmap = wallpaperBitmap,
                extendedFade = true,
                modifier = Modifier.fillMaxSize().padding(top = 8.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            val cutoutHeight = if (LocalInspectionMode.current) 16.dp else getCarmaHeight()
            if (topBarPressedDown) Spacer(Modifier.height(cutoutHeight.coerceAtLeast(8.dp) - 8.dp))

            val heroModifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onWallpaperLongPress() })
                }
            if (useBackgroundHero) {
                Spacer(heroModifier)
            } else {
                MainHeroImage(wallpaperBitmap = wallpaperBitmap, modifier = heroModifier)
            }

            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun MainHeroImage(
    wallpaperBitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    extendedFade: Boolean = false,
) {
    val cornerRadius = getRadius()
    val imageModifier = Modifier.fillMaxSize()
        .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
    val background = MaterialTheme.colorScheme.background

    Box(modifier.padding(horizontal = 8.dp)) {
        if (wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
        } else {
            Image(
                painter = painterResource(R.drawable.my_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
        }

        val fade = if (extendedFade) {
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.2f to background.copy(alpha = 0.15f),
                0.5f to background.copy(alpha = 0.75f),
                0.8f to background.copy(alpha = 0.95f),
                1f to background,
            )
        } else {
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.65f to Color.Transparent,
                1f to background,
            )
        }
        Box(Modifier.fillMaxSize().background(fade))
    }
}

/**
 * 左侧 YumeBox 风格半透明窄边栏（严格对齐 shizuku-ftp：时钟、电量、设置、工具、权限 3 大按钮）
 */
@Composable
fun NarrowWallpaperRail(
    currentTime: Long,
    batteryState: BatteryState,
    gearRotation: Float = 0f,
    onGearClick: () -> Unit,
    onLinkClick: () -> Unit,
    onShowCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorBag: ColorBag = ColorBag(),
    useM3Color: Boolean = colorBag.useM3Color
) {
    val timeText = remember(currentTime) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTime))
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(64.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            VerticalTimeText(timeText)
            Spacer(modifier = Modifier.height(24.dp))

            BatteryIcon(
                progress = batteryState.percent?.toFloat()?.div(100f) ?: 0.85f,
                isCharging = batteryState.isCharging
            )
            Text(
                text = batteryState.percent?.let { "$it%" } ?: "--%",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                softWrap = false,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))

            // 1. 设置侧栏 (Gear)
            MenuButton(
                iconRes = R.drawable.gear,
                rotation = gearRotation,
                onClick = onGearClick,
                colorBag = colorBag,
                useM3Color = useM3Color,
                contentDescription = "系统与环境设置"
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 2. 工具与日志侧栏 (Link)
            MenuButton(
                iconRes = R.drawable.link,
                rotation = gearRotation,
                onClick = onLinkClick,
                colorBag = colorBag,
                useM3Color = useM3Color,
                contentDescription = "工具与日志"
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 3. 权限与状态卡片入口 (ShowCardButton)
            ShowCardButton(
                onClick = onShowCardClick,
                colorBag = colorBag,
                useM3Color = useM3Color
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
