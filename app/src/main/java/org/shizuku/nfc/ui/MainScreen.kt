package org.shizuku.nfc.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.shizuku.nfc.NfcEmulator
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
    isShizukuRunning: Boolean,
    isShizukuPermissionGranted: Boolean,
    shizukuVersion: Int,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

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
            appendLog("已选择文件: $selectedFileName")
        }
    }

    // 解析当前选中的 NDEF 消息
    val currentNdefMessage = remember(selectedMode, customHexInput, selectedFileUri) {
        try {
            when (selectedMode) {
                PayloadMode.DEFAULT_ALIPAY -> {
                    NfcEmulator.createDefaultAlipayNdefMessage()
                }
                PayloadMode.ASSET_FILE -> {
                    NfcEmulator.loadFromAssets(context, "xiaoyuanpao-extracted.ndef")
                }
                PayloadMode.LOCAL_FILE -> {
                    val uri = selectedFileUri
                    if (uri != null) {
                        context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                            NfcEmulator.parseNdefBytes(stream.readBytes())
                        } ?: NfcEmulator.createDefaultAlipayNdefMessage()
                    } else {
                        NfcEmulator.createDefaultAlipayNdefMessage()
                    }
                }
                PayloadMode.CUSTOM_HEX -> {
                    if (customHexInput.isNotBlank()) {
                        NfcEmulator.parseNdefHex(customHexInput)
                    } else {
                        NfcEmulator.createDefaultAlipayNdefMessage()
                    }
                }
            }
        } catch (e: Exception) {
            appendLog("NDEF 解析警告: ${e.message}")
            NfcEmulator.createDefaultAlipayNdefMessage()
        }
    }

    val recordsInfo = remember(currentNdefMessage) {
        NfcEmulator.inspectRecords(currentNdefMessage)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Shizuku NFC",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "免 Root 模拟触发“碰一下” NDEF",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        logs.clear()
                        appendLog("日志已清空")
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "清空日志")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── 1. Shizuku 状态卡片 ──
            item {
                ShizukuStatusCard(
                    isRunning = isShizukuRunning,
                    isGranted = isShizukuPermissionGranted,
                    version = shizukuVersion,
                    onRequestPermission = onRequestPermission
                )
            }

            // ── 2. NFC 核心触发卡片 ──
            item {
                NfcTriggerCard(
                    isReady = isShizukuRunning && isShizukuPermissionGranted,
                    recordCount = recordsInfo.size,
                    onTriggerShizuku = {
                        appendLog(">>> 开始执行 Shizuku 模拟派发...")
                        try {
                            val mockTag = NfcEmulator.createMockTag(currentNdefMessage)
                            appendLog("Mock Tag 创建成功 (Tech: NFC_A, NDEF)")
                            val result = NfcEmulator.dispatchNfcTagViaShizuku(mockTag) { logText ->
                                appendLog(logText)
                            }
                            if (result.isSuccess) {
                                Toast.makeText(context, "碰一下事件派发成功！", Toast.LENGTH_SHORT).show()
                            } else {
                                val err = result.exceptionOrNull()
                                appendLog("派发失败: ${err?.message}")
                                Toast.makeText(context, "派发失败: ${err?.message}", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Throwable) {
                            appendLog("发生异常: ${e.message}")
                            Toast.makeText(context, "异常: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    onTriggerIntentFallback = {
                        appendLog(">>> 开始执行备用 Intent 方案...")
                        try {
                            val mockTag = NfcEmulator.createMockTag(currentNdefMessage)
                            NfcEmulator.dispatchViaIntentDirect(context, mockTag, currentNdefMessage) {
                                appendLog(it)
                            }
                        } catch (e: Throwable) {
                            appendLog("Intent 发送异常: ${e.message}")
                        }
                    }
                )
            }

            // ── 3. 负载配置与解析卡片 ──
            item {
                PayloadConfigCard(
                    selectedMode = selectedMode,
                    onModeChange = { selectedMode = it },
                    customHex = customHexInput,
                    onHexChange = { customHexInput = it },
                    selectedFileName = selectedFileName,
                    onPickFile = { filePickerLauncher.launch("*/*") },
                    records = recordsInfo
                )
            }

            // ── 4. 实时日志卡片 ──
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "执行控制台日志",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = {
                                    val full = logs.joinToString("\n")
                                    clipboardManager.setText(AnnotatedString(full))
                                    Toast.makeText(context, "已复制全部日志", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("复制日志", fontSize = 12.sp)
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        LazyColumn(
                            state = logListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (logs.isEmpty()) {
                                item {
                                    Text(
                                        text = "就绪。点击上方按钮模拟派发 NFC 事件...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                items(logs) { logLine ->
                                    Text(
                                        text = logLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShizukuStatusCard(
    isRunning: Boolean,
    isGranted: Boolean,
    version: Int,
    onRequestPermission: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when {
            !isRunning -> Color(0xFFF5222D)
            !isGranted -> Color(0xFFFAAD14)
            else -> Color(0xFF52C41A)
        },
        label = "statusColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shizuku 服务状态",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when {
                            !isRunning -> "未运行"
                            !isGranted -> "未授权"
                            else -> "已就绪 (UID 2000)"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when {
                    !isRunning -> "请启动 Shizuku 应用（通过无线调试或电脑 ADB 激活）"
                    !isGranted -> "Shizuku 服务已启动，但本应用尚未获得权限，请点击下方按钮授权。"
                    else -> "Shizuku 正常运行 (API v$version)，具备 Binder 调用权限。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isRunning && !isGranted) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("请求 Shizuku 授权")
                }
            }
        }
    }
}

@Composable
fun NfcTriggerCard(
    isReady: Boolean,
    recordCount: Int,
    onTriggerShizuku: () -> Unit,
    onTriggerIntentFallback: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "模拟触发碰一下",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "目标：注入包含 $recordCount 条 Record 的虚拟 NDEF 标签，经由 INfcAdapter 派发至支付宝碰一下引擎。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onTriggerShizuku,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = isReady,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "一键模拟碰一下 (Shizuku dispatch)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onTriggerIntentFallback,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("备选方案：Intent 显式调起", fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadConfigCard(
    selectedMode: PayloadMode,
    onModeChange: (PayloadMode) -> Unit,
    customHex: String,
    onHexChange: (String) -> Unit,
    selectedFileName: String?,
    onPickFile: () -> Unit,
    records: List<NfcEmulator.RecordInfo>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "NDEF 负载配置与解析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 模式选择 Tabs / FilterChips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedMode == PayloadMode.DEFAULT_ALIPAY,
                    onClick = { onModeChange(PayloadMode.DEFAULT_ALIPAY) },
                    label = { Text("内置支付宝", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedMode == PayloadMode.ASSET_FILE,
                    onClick = { onModeChange(PayloadMode.ASSET_FILE) },
                    label = { Text("提取文件", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedMode == PayloadMode.LOCAL_FILE,
                    onClick = { onModeChange(PayloadMode.LOCAL_FILE) },
                    label = { Text("本地选择", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = selectedMode == PayloadMode.CUSTOM_HEX,
                    onClick = { onModeChange(PayloadMode.CUSTOM_HEX) },
                    label = { Text("Hex 输入", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedMode) {
                PayloadMode.DEFAULT_ALIPAY -> {
                    Text(
                        text = "当前使用内置默认值：axdk 统一链接 + 支付宝 AAR",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                PayloadMode.ASSET_FILE -> {
                    Text(
                        text = "当前读取内置 assets/xiaoyuanpao-extracted.ndef (154 字节)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                PayloadMode.LOCAL_FILE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedFileName ?: "未选择文件",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = onPickFile,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("选择 .ndef 文件", fontSize = 12.sp)
                        }
                    }
                }
                PayloadMode.CUSTOM_HEX -> {
                    OutlinedTextField(
                        value = customHex,
                        onValueChange = onHexChange,
                        placeholder = { Text("输入或粘贴 HEX 格式 NDEF 字节串 (如 91 01 69 55...)") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        maxLines = 4
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "解析结果 (包含 ${records.size} 条 Record):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            records.forEach { record ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Record #${record.index}: ${record.type}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${record.length} 字节",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "TNF: ${record.tnfName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = record.payloadPreview,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
