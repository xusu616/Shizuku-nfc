package org.shizuku.nfc.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
    previewColorBag: ColorBag? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    // 引导与更新 ViewModel（完全对齐 shizuku-ftp 架构）
    val startupViewModel: StartupViewModel = if (LocalInspectionMode.current) {
        viewModel()
    } else {
        viewModel()
    }
    val updateViewModel: UpdatePromptViewModel = if (LocalInspectionMode.current) {
        viewModel()
    } else {
        viewModel()
    }

    var startupReady by remember { mutableStateOf(false) }

    // 弹窗状态
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

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = uri.lastPathSegment ?: "custom.ndef"
            appendLog("已选择本地 NDEF: $selectedFileName")
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

    // 核心触发操作
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
    val colorBag = previewColorBag ?: ColorBag()

    val railPadding by animateDpAsState(
        targetValue = if (railVisible) 64.dp else 0.dp,
        label = "RailPadding"
    )
    val barVisibleRotation by animateFloatAsState(
        targetValue = if (railVisible) 180f else 0f,
        label = "BarVisibleRotation"
    )
    val cornerRadius = getRadius()

    // ── 外层全屏容器 ──
    Box(modifier = Modifier.fillMaxSize()) {
        // 壁纸采样图层 (Haze Source)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer()
                .hazeSource(state = hazeState)
        ) {
            WallpaperBase(wallpaperBitmap = customWallpaperBitmap)
        }

        // 全局毛玻璃磨砂覆盖层
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

        // ── 左侧窄边栏 (NarrowWallpaperRail) ──
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
                onSettingsClick = { showShizukuDialog = true },
                onReplayGuideClick = {
                    startupViewModel.replay()
                },
                onUpdateClick = {
                    updateViewModel.checkManually { isNewAvailable, version ->
                        if (!isNewAvailable) {
                            Toast.makeText(context, "当前已是最新版本 (v$version)", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onShizukuCardClick = { showShizukuDialog = true },
                modifier = Modifier.fillMaxHeight(),
                colorBag = colorBag,
                useM3Color = colorBag.useM3Color
            )
        }

        // ── 主内容面板容器 (带平滑缩进和圆角剪裁) ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = railPadding)
                .clip(RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius))
                .background(MaterialTheme.colorScheme.background)
        ) {
            MainHeroLayout(
                wallpaperBitmap = customWallpaperBitmap,
                topBarPressedDown = true,
                onWallpaperLongPress = { wallpaperPicker.launch("image/*") }
            ) {
                // 顶部控制行：展开/折叠侧边栏 + 高光碰一下胶囊按钮 + 重新引导快捷入口
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

                    // 重新引导快捷按钮
                    FilledTonalIconButton(
                        onClick = { startupViewModel.replay() },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.refresh),
                            contentDescription = stringResource(R.string.guide_replay),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 卡片垂直流容器
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ── 1. Shizuku 运行与授权状态卡片 ──
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showShizukuDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            if (isShizukuRunning && isShizukuPermissionGranted) Color(0xFF4CAF50)
                                            else MaterialTheme.colorScheme.error,
                                            CircleShape
                                        )
                                )
                                Column {
                                    Text(
                                        text = if (isShizukuRunning) "Shizuku 正常运行 (v$shizukuVersion)" else "Shizuku 未启动",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isShizukuPermissionGranted) "UID 2000 权限已就绪 (可注入 NFC)" else "未获得授权，点击查看详情",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (!isShizukuPermissionGranted && isShizukuRunning) {
                                Button(
                                    onClick = onRequestPermission,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("申请授权", fontSize = 12.sp)
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // ── 2. NDEF 负载配置与切换卡片 ──
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "NDEF 目标负载选择",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(10.dp))

                            // 4 种模式分段选择器
                            val modes = listOf(
                                PayloadMode.DEFAULT_ALIPAY to "支付宝 axdk",
                                PayloadMode.ASSET_FILE to "小元宝提取",
                                PayloadMode.LOCAL_FILE to "本地文件",
                                PayloadMode.CUSTOM_HEX to "HEX 字节"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                modes.forEach { (mode, title) ->
                                    val isSelected = selectedMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedMode = mode }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // 本地文件选择入口
                            if (selectedMode == PayloadMode.LOCAL_FILE) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = selectedFileName ?: "未选择文件",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    OutlinedButton(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("浏览选择 .ndef", fontSize = 12.sp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            // 自定义 HEX 输入框
                            if (selectedMode == PayloadMode.CUSTOM_HEX) {
                                OutlinedTextField(
                                    value = customHexInput,
                                    onValueChange = { customHexInput = it },
                                    label = { Text("输入 NDEF 二进制十六进制字符串 (Hex)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    placeholder = { Text("如: D1012C5402656E...") },
                                    maxLines = 3
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            // 负载 Records 详细解析信息
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "当前解析负载 (包含 ${recordsInfo.size} 条 Record):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                recordsInfo.forEach { recordLine ->
                                    Text(
                                        text = recordLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // ── 3. 实时日志控制台卡片 ──
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "系统注入控制台日志",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row {
                                    TextButton(
                                        onClick = { logs.clear() },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("清空", fontSize = 12.sp)
                                    }
                                    TextButton(
                                        onClick = {
                                            val full = logs.joinToString("\n")
                                            clipboardManager.setText(AnnotatedString(full))
                                            Toast.makeText(context, "已复制全部日志", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("复制", fontSize = 12.sp)
                                    }
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            LazyColumn(
                                state = logListState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (logs.isEmpty()) {
                                    item {
                                        Text(
                                            text = "就绪。点击上方高光胶囊按钮开始模拟注入...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(top = 16.dp)
                                        )
                                    }
                                } else {
                                    items(logs) { log ->
                                        Text(
                                            text = log,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

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
                        Divider()
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
        if (!LocalInspectionMode.current) {
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
