package org.shizuku.nfc.ui

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.shizuku.nfc.NfcEmulator
import org.shizuku.nfc.R
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

enum class PayloadMode {
    DEFAULT_ALIPAY,
    ASSET_FILE,
    LOCAL_FILE,
    CUSTOM_HEX
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isShizukuRunning: Boolean = true,
    isShizukuPermissionGranted: Boolean = true,
    shizukuVersion: Int = 13,
    onRequestPermission: () -> Unit = {},
    previewColorBag: ColorBag? = null,
    startupViewModel: StartupViewModel? = if (LocalInspectionMode.current) null else viewModel(),
    updateViewModel: UpdatePromptViewModel? = if (LocalInspectionMode.current) null else viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    var startupReady by remember { mutableStateOf(false) }

    // 抽屉与路由状态（完全对齐 shizuku-ftp 侧栏路由架构）
    val sideMenuState = rememberSideMenuState()

    var showShizukuDialog by remember { mutableStateOf(false) }
    var railVisible by remember { mutableStateOf(true) }
    var isDispatching by remember { mutableStateOf(false) }

    // 时间与电池状态
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val batteryState = rememberBatteryState()

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(30_000)
        }
    }

    // 自定义壁纸状态
    var customWallpaperBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val wallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        customWallpaperBitmap = bmp.asImageBitmap()
                        Toast.makeText(context, "已设置自定义专属壁纸", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "壁纸加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 负载模式与配置
    var selectedMode by remember { mutableStateOf(PayloadMode.DEFAULT_ALIPAY) }
    var customHexInput by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val logs = remember { mutableStateListOf<String>() }
    val logListState = rememberLazyListState()

    fun appendLog(msg: String) {
        val entry = "[${timeFormat.format(Date())}] $msg"
        logs.add(entry)
        coroutineScope.launch {
            if (logs.isNotEmpty()) {
                logListState.animateScrollToItem(logs.size - 1)
            }
        }
    }

    // 解析当前选中的 NDEF 消息
    val currentNdefMessage: android.nfc.NdefMessage = remember(selectedMode, customHexInput, selectedFileUri) {
        try {
            when (selectedMode) {
                PayloadMode.DEFAULT_ALIPAY -> {
                    NfcEmulator.createDefaultAlipayNdefMessage()
                }
                PayloadMode.ASSET_FILE -> {
                    try {
                        NfcEmulator.loadFromAssets(context, "xiaoyuanpao-extracted.ndef")
                    } catch (e: Exception) {
                        appendLog("未找到 assets 资产包，降级使用默认 axdk 消息")
                        NfcEmulator.createDefaultAlipayNdefMessage()
                    }
                }
                PayloadMode.LOCAL_FILE -> {
                    if (selectedFileUri != null) {
                        val stream = context.contentResolver.openInputStream(selectedFileUri!!)
                        val bytes = stream?.readBytes() ?: ByteArray(0)
                        stream?.close()
                        NfcEmulator.parseNdefBytes(bytes)
                    } else {
                        NfcEmulator.createDefaultAlipayNdefMessage()
                    }
                }
                PayloadMode.CUSTOM_HEX -> {
                    val cleaned = customHexInput.replace("\\s+".toRegex(), "")
                    if (cleaned.isNotEmpty()) {
                        NfcEmulator.parseNdefHex(cleaned)
                    } else {
                        NfcEmulator.createDefaultAlipayNdefMessage()
                    }
                }
            }
        } catch (e: Exception) {
            appendLog("NDEF 解析异常: ${e.message}，使用默认消息")
            NfcEmulator.createDefaultAlipayNdefMessage()
        }
    }

    val recordsInfo: List<String> = remember(currentNdefMessage) {
        currentNdefMessage.records.mapIndexed { index: Int, record: android.nfc.NdefRecord ->
            val payloadText = try {
                if (record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                    record.type.contentEquals(android.nfc.NdefRecord.RTD_URI)
                ) {
                    val prefix = when (record.payload.firstOrNull()?.toInt()) {
                        0x01 -> "http://www."
                        0x02 -> "https://www."
                        0x03 -> "http://"
                        0x04 -> "https://"
                        else -> ""
                    }
                    if (record.payload.isNotEmpty()) {
                        prefix + String(record.payload, 1, record.payload.size - 1, Charsets.UTF_8)
                    } else {
                        ""
                    }
                } else {
                    String(record.payload, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                "Hex: " + record.payload.joinToString("") { "%02X".format(it) }
            }
            "Record #${index + 1} [TNF=${record.tnf}, Type=${String(record.type)}]: $payloadText"
        }
    }

    val currentModeTitle = remember(selectedMode, selectedFileName) {
        when (selectedMode) {
            PayloadMode.DEFAULT_ALIPAY -> "支付宝碰一下 (axdk 官方)"
            PayloadMode.ASSET_FILE -> "小元宝提取包 (xiaoyuanpao)"
            PayloadMode.LOCAL_FILE -> "本地文件: ${selectedFileName ?: "未选择"}"
            PayloadMode.CUSTOM_HEX -> "自定义 HEX 16 进制字节流"
        }
    }

    // 核心碰一下模拟派发
    fun doTriggerTouch() {
        if (!isShizukuRunning) {
            Toast.makeText(context, "Shizuku 未运行，请先在 Shizuku App 中启动服务！", Toast.LENGTH_LONG).show()
            appendLog("错误: Shizuku 服务未运行")
            showShizukuDialog = true
            return
        }
        if (!isShizukuPermissionGranted) {
            Toast.makeText(context, "未获得 Shizuku 授权，请点击侧栏状态盾牌完成授权！", Toast.LENGTH_LONG).show()
            appendLog("错误: Shizuku 未授权，正在拉起授权申请...")
            onRequestPermission()
            return
        }

        coroutineScope.launch {
            isDispatching = true
            appendLog(">>> 开始执行免 Root 碰一下事件模拟注入...")
            try {
                val mockTag = NfcEmulator.createMockTag(currentNdefMessage)
                appendLog("Mock Tag 构造成功 (Tech: NFC_A 0x01, NDEF 0x06)")
                val result = NfcEmulator.dispatchNfcTagViaShizuku(mockTag) { logText ->
                    appendLog(logText)
                }
                if (result.isSuccess) {
                    Toast.makeText(context, "碰一下事件派发成功！已唤醒支付宝", Toast.LENGTH_SHORT).show()
                } else {
                    val err = result.exceptionOrNull()
                    appendLog("派发失败: ${err?.message}")
                    Toast.makeText(context, "派发失败: ${err?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Throwable) {
                appendLog("发生异常: ${e.message}")
                Toast.makeText(context, "异常: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                delay(600)
                isDispatching = false
            }
        }
    }

    // 毛玻璃与动画参数
    val hazeState = rememberHazeState()
    val sidebarBackdrop = rememberGlassBackdrop()
    val colorBag = previewColorBag ?: ColorBag()

    val railPadding by animateDpAsState(
        targetValue = if (railVisible) 64.dp else 0.dp,
        label = "RailPadding"
    )
    val barVisibleRotation by animateFloatAsState(
        targetValue = if (railVisible) 180f else 0f,
        label = "BarVisibleRotation"
    )
    val gearRotation by animateFloatAsState(
        targetValue = if (sideMenuState.isOpen) 180f else 0f,
        label = "GearRotation"
    )
    val cornerRadius = getRadius()

    // ── 外层全屏容器 ──
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 底层真实壁纸（参考 commit 7ae6ff86：动画挂载时 source layer 可能失效，必须打底保底，永不露黑底）
        WallpaperBase(wallpaperBitmap = customWallpaperBitmap)

        // 2. 独立采样图层（参考 commit 17b9074c：独立 RenderNode 避免兄弟组件重绘时清空 Haze 采样）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer()
                .hazeSource(state = hazeState)
        ) {
            WallpaperBase(wallpaperBitmap = customWallpaperBitmap)
        }

        // 3. 全局毛玻璃磨砂覆盖层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassHaze(
                    hazeState = hazeState,
                    blurIntensity = 25f,
                    darkTint = Color.Black.copy(alpha = 0.18f),
                    lightTint = Color.White.copy(alpha = 0.22f),
                    darkFallback = Color.Black.copy(alpha = 0.50f),
                    lightFallback = Color.White.copy(alpha = 0.65f),
                )
        )

        // 4. 侧栏专用固定尺寸背景捕获（参考 commit 50a87c25：始终在主界面坐标生成，记录到 GraphicsLayer，抽屉展开时零着色器重建，杜绝闪烁）
        CaptureGlassBackdrop(
            backdrop = sidebarBackdrop,
            hazeState = hazeState,
            blurIntensity = 25f,
        )

        // ── 左侧窄边栏 (NarrowWallpaperRail) 严格保持 3 大标准按钮 ──
        AnimatedVisibility(
            visible = railVisible,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300)),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            NarrowWallpaperRail(
                currentTime = currentTime,
                batteryState = batteryState,
                gearRotation = gearRotation,
                onGearClick = { sideMenuState.open(SideMenu.SETTINGS) },
                onLinkClick = { sideMenuState.open(SideMenu.TOOLS) },
                onShowCardClick = { showShizukuDialog = true },
                modifier = Modifier.fillMaxHeight(),
                colorBag = colorBag,
                useM3Color = colorBag.useM3Color
            )
        }

        // ── 侧栏及响应式多端布局框架 (完全对齐 shizuku-ftp/SideMenuLayout) ──
        SideMenuLayout(
            state = sideMenuState,
            railPadding = railPadding,
            backdrop = sidebarBackdrop,
            colorBag = colorBag,
            onNavigate = { route ->
                when (route) {
                    "guide" -> {
                        sideMenuState.close()
                        startupViewModel?.replay()
                    }
                    "update" -> {
                        updateViewModel?.checkManually { isNewAvailable, version ->
                            Toast.makeText(
                                context,
                                if (isNewAvailable) "发现新版本: v$version" else "当前已是最新版 (v$version)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    "wallpaper" -> {
                        sideMenuState.close()
                        wallpaperPicker.launch("image/*")
                    }
                }
            },
            mainContent = { mainStartPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = mainStartPadding)
                        .clip(RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    MainHeroLayout(
                        wallpaperBitmap = customWallpaperBitmap,
                        topBarPressedDown = true,
                        heroHeight = 380.dp,
                        onWallpaperLongPress = { wallpaperPicker.launch("image/*") }
                    ) {
                        // 顶部控制行：折叠侧栏 + 居中高光碰一下胶囊按钮 + 工具快捷按钮
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            GlassBackgroundToggleButton(
                                enabled = railVisible,
                                onClick = { railVisible = !railVisible },
                                rotation = barVisibleRotation,
                                colorBag = colorBag,
                                useOriginColor = colorBag.useM3Color
                            )

                            Spacer(modifier = Modifier.weight(0.1f))

                            // 核心碰一下胶囊按钮 (ServerControlButton 风格)
                            ServerControlButton(
                                isExecuting = isDispatching,
                                onClick = { doTriggerTouch() }
                            )

                            Spacer(modifier = Modifier.weight(0.1f))

                            // 工具快捷按钮 (Link)
                            FilledTonalIconButton(
                                onClick = { sideMenuState.open(SideMenu.TOOLS) },
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.link),
                                    contentDescription = "打开功能与工具",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(gearRotation),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        // ── 简约分散卡片 1：当前活跃 NDEF 负载与 Shizuku 状态胶囊条 ──
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clickable {
                                    sideMenuState.open(SideMenu.TOOLS)
                                    sideMenuState.showDetail("payload")
                                },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                if (isShizukuRunning && isShizukuPermissionGranted) Color(0xFF4CAF50)
                                                else MaterialTheme.colorScheme.error,
                                                CircleShape
                                            )
                                    )
                                    Column {
                                        Text(
                                            text = currentModeTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isShizukuRunning && isShizukuPermissionGranted)
                                                "UID 2000 就绪 · 包含 ${recordsInfo.size} 条 Record"
                                            else
                                                "Shizuku 特权未就绪 · 点击侧栏管理",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "进入独立负载配置页面",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        // ── 简约分散卡片 2：极简执行动态条（点击直达独立全屏控制台页面） ──
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clickable {
                                    sideMenuState.open(SideMenu.TOOLS)
                                    sideMenuState.showDetail("logs")
                                },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "最新执行动态",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "独立控制台 ❯",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    val lastLog = logs.lastOrNull()
                                    Text(
                                        text = lastLog ?: "就绪。点击上方高光胶囊按钮开始模拟注入...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (lastLog != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            },
            detailContent = { route, expanded, onBack ->
                when (route) {
                    "logs" -> LogsDestinationScreen(
                        logs = logs,
                        logListState = logListState,
                        onClearLogs = { logs.clear() },
                        onBack = onBack
                    )
                    "payload" -> PayloadDestinationScreen(
                        selectedMode = selectedMode,
                        onModeChange = { selectedMode = it },
                        customHex = customHexInput,
                        onHexChange = { customHexInput = it },
                        selectedFileName = selectedFileName,
                        onSelectFileUri = { uri, name ->
                            selectedFileUri = uri
                            selectedFileName = name
                            appendLog("已选择本地 NDEF: $name")
                        },
                        recordsInfo = recordsInfo,
                        onBack = onBack
                    )
                    "shizuku" -> ShizukuDestinationScreen(
                        isShizukuRunning = isShizukuRunning,
                        isShizukuPermissionGranted = isShizukuPermissionGranted,
                        shizukuVersion = shizukuVersion,
                        onRequestPermission = onRequestPermission,
                        onBack = onBack
                    )
                    "about" -> AboutDestinationScreen(
                        onBack = onBack
                    )
                }
            }
        )

        // ── 权限与 Shizuku 详情弹窗 ──
        if (showShizukuDialog) {
            CompactSettingsDialog(
                onDismissRequest = { showShizukuDialog = false },
                title = { Text("Shizuku 免 Root 运行环境") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "本程序通过 Shizuku Binder 机制获取系统 android.nfc.INfcAdapter 特权服务，并模拟真实物理卡片（Tech: NFC_A 0x01, NDEF 0x06）的插入派发。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        HorizontalDivider()
                        Text("• 服务状态: " + if (isShizukuRunning) "正在运行 (v$shizukuVersion)" else "未运行")
                        Text("• 授权状态: " + if (isShizukuPermissionGranted) "已获得 UID 2000 授权" else "未授权")
                        Text("• 隐藏 API: 已通过 HiddenApiBypass 全局豁免限制")
                        if (!isShizukuPermissionGranted && isShizukuRunning) {
                            Button(
                                onClick = {
                                    onRequestPermission()
                                    showShizukuDialog = false
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text("立即授予 Shizuku 权限")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showShizukuDialog = false }) {
                        Text("关闭")
                    }
                }
            )
        }

        // ── 欢迎引导与更新弹窗（保持与 shizuku-ftp 一致的级联触发逻辑） ──
        if (!LocalInspectionMode.current && startupViewModel != null && updateViewModel != null) {
            StartupExperience(
                state = startupViewModel,
                onReady = { startupReady = true }
            )
            StartupUpdatePrompt(
                ready = startupReady,
                viewModel = updateViewModel
            )
        }
    }
}

// ─── 预览专用的 Mock Application 与 ViewModel 构造器 ───

/**
 * 预览专用的 Mock Application，包装 Preview 环境下的 BridgeContext，安全支持 SharedPreferences
 */
class PreviewApplication(base: Context) : Application() {
    init {
        attachBaseContext(base)
    }
}

/**
 * 为 Preview 构造专用的 StartupViewModel
 */
@Composable
fun rememberPreviewStartupViewModel(): StartupViewModel {
    val context = LocalContext.current
    return remember { StartupViewModel(PreviewApplication(context)) }
}

/**
 * 为 Preview 构造专用的 UpdatePromptViewModel
 */
@Composable
fun rememberPreviewUpdatePromptViewModel(): UpdatePromptViewModel {
    val context = LocalContext.current
    return remember { UpdatePromptViewModel(PreviewApplication(context)) }
}

// ─── 预览支持 ───

@Preview(name = "Main Screen Light", showBackground = true)
@Composable
fun MainScreenPreviewLight() {
    ShizukuNfcTheme(darkTheme = false) {
        MainScreen(
            isShizukuRunning = true,
            isShizukuPermissionGranted = true,
            shizukuVersion = 13,
            onRequestPermission = {}
        )
    }
}

@Preview(name = "Main Screen Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreenPreviewDark() {
    ShizukuNfcTheme(darkTheme = true) {
        MainScreen(
            isShizukuRunning = true,
            isShizukuPermissionGranted = false,
            shizukuVersion = 13,
            onRequestPermission = {}
        )
    }
}

/**@Preview(name = "Main Screen with Mock ViewModel", showBackground = true)
@Composable
fun MainScreenPreviewWithViewModel() {
    ShizukuNfcTheme(darkTheme = false) {
        MainScreen(
            isShizukuRunning = true,
            isShizukuPermissionGranted = true,
            shizukuVersion = 13,
            onRequestPermission = {},
            startupViewModel = rememberPreviewStartupViewModel(),
            updateViewModel = rememberPreviewUpdatePromptViewModel()
        )
    }
}
**/