package org.shizuku.nfc.ui

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import org.shizuku.nfc.R
import org.shizuku.nfc.ui.data.ColorBag

private const val PREFS_STARTUP = "startup_experience"
private const val PREF_KEY_GUIDE_DONE = "home_guide_v1_done"

/**
 * 启动引导状态管理（完全仿照 shizuku-ftp/StartupViewModel）
 */
class StartupViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS_STARTUP, Context.MODE_PRIVATE)

    var guideVisible by mutableStateOf(!prefs.getBoolean(PREF_KEY_GUIDE_DONE, false))
        private set

    var page by mutableIntStateOf(0)

    fun replay() {
        page = 0
        guideVisible = true
    }

    fun finishGuide() {
        prefs.edit { putBoolean(PREF_KEY_GUIDE_DONE, true) }
        guideVisible = false
    }
}

@Composable
fun StartupExperience(
    state: StartupViewModel,
    colorBag: ColorBag = ColorBag(),
    onReady: () -> Unit = {}
) {
    WallpaperControlsTheme(colorBag) {
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = state.guideVisible

    LaunchedEffect(state.guideVisible, visibility.isIdle) {
        if (!state.guideVisible && visibility.isIdle) onReady()
    }

    if (visibility.currentState || visibility.targetState) {
        Dialog(onDismissRequest = state::finishGuide) {
            AnimatedVisibility(
                visibleState = visibility,
                enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.94f),
                exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.96f),
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.guide_welcome),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            AnimatedContent(
                                targetState = state.page,
                                transitionSpec = {
                                    val direction = if (targetState > initialState) 1 else -1
                                    (slideInHorizontally(tween(320)) { direction * it / 3 } + fadeIn()) togetherWith
                                            (slideOutHorizontally(tween(320)) { -direction * it / 3 } + fadeOut())
                                },
                                label = "guidePageAnimation",
                            ) { page ->
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    GuideIllustration(page)
                                    Text(
                                        text = stringResource(guideTitles[page]),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stringResource(guideDescriptions[page]),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // 引导页圆点指示器
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(4) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(if (index == state.page) 9.dp else 6.dp)
                                        .background(
                                            if (index == state.page) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                )
                            }
                        }

                        // 底部操作按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = state::finishGuide) {
                                Text(stringResource(R.string.guide_skip))
                            }
                            Row {
                                if (state.page > 0) {
                                    TextButton(onClick = { state.page-- }) {
                                        Text(stringResource(R.string.guide_back))
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (state.page == 3) state.finishGuide() else state.page++
                                    }
                                ) {
                                    Text(
                                        stringResource(
                                            if (state.page == 3) R.string.guide_done else R.string.guide_next
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            BackHandler {
                if (state.page > 0) state.page-- else state.finishGuide()
            }
        }
    }
}
}

private val guideTitles = listOf(
    R.string.guide_wallpaper_title,
    R.string.guide_trigger_title,
    R.string.guide_payload_title,
    R.string.guide_permissions_title
)

private val guideDescriptions = listOf(
    R.string.guide_wallpaper_body,
    R.string.guide_trigger_body,
    R.string.guide_payload_body,
    R.string.guide_permissions_body
)

/**
 * 引导卡片内部动态微缩模型插画（仿照 shizuku-ftp 动态呼吸脉冲设计）
 */
@Composable
private fun GuideIllustration(page: Int) {
    val colors = MaterialTheme.colorScheme
    val pulse by rememberInfiniteTransition(label = "guidePulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "targetPulse",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        // 左侧微缩侧边栏模拟
        Column(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val icons = listOf(Icons.Default.PlayArrow, Icons.Default.Send, Icons.Default.Info)
            icons.forEachIndexed { index, icon ->
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .size(36.dp)
                        .scale(if (page == index + 1) pulse else 1f)
                        .background(
                            if (page == index + 1) colors.primaryContainer else colors.surface,
                            CircleShape
                        )
                        .border(
                            if (page == index + 1) 2.dp else 0.dp,
                            if (page == index + 1) colors.primary else Color.Transparent,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.onSurface
                    )
                }
            }
        }

        // 右侧主面板模拟
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(colors.primaryContainer, colors.tertiaryContainer)
                        )
                    )
            ) {
                if (page == 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(50.dp)
                            .scale(pulse)
                            .border(2.dp, colors.primary, CircleShape)
                            .background(colors.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("碰一下", fontSize = 10.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(30.dp)
                    .border(1.dp, colors.primary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Shizuku NFC · 免Root",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── 完整保留 @Preview ───

@Preview(name = "Startup Guide Light", showBackground = true, locale = "zh")
@Composable
fun StartupGuidePreviewLight() {
    ShizukuNfcTheme(darkTheme = false) {
        GuideIllustration(page = 1)
    }
}

@Preview(name = "Startup Guide Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "zh")
@Composable
fun StartupGuidePreviewDark() {
    ShizukuNfcTheme(darkTheme = true) {
        GuideIllustration(page = 2)
    }
}
