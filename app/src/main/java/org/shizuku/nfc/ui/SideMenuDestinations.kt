package org.shizuku.nfc.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.shizuku.nfc.R

/**
 * 独立的系统注入控制台日志页面（拆为独立页面，完全解决右屏杂乱问题）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsDestinationScreen(
    logs: List<String>,
    logListState: LazyListState,
    onClearLogs: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统注入控制台", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onClearLogs) {
                        Text("清空")
                    }
                    TextButton(onClick = {
                        val full = logs.joinToString("\n")
                        clipboardManager.setText(AnnotatedString(full))
                        Toast.makeText(context, "已复制全部日志", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("复制全部")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                LazyColumn(
                    state = logListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = "等待触发 NFC 碰一下事件...",
                                style = MaterialTheme.typography.bodyMedium,
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
    }
}

/**
 * 独立的 NDEF 目标负载配置页面（拆为独立页面）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadDestinationScreen(
    selectedMode: PayloadMode,
    onModeChange: (PayloadMode) -> Unit,
    customHex: String,
    onHexChange: (String) -> Unit,
    selectedFileName: String?,
    onSelectFileUri: (Uri, String) -> Unit,
    recordsInfo: List<String>,
    onBack: () -> Unit
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment ?: "custom.ndef"
            onSelectFileUri(uri, name)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NDEF 目标负载配置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 模式选择分段卡片
            Text(
                text = "目标载荷来源",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val modes = listOf(
                PayloadMode.DEFAULT_ALIPAY to "支付宝碰一下 (axdk 官方)",
                PayloadMode.ASSET_FILE to "小元宝提取包 (xiaoyuanpao.ndef)",
                PayloadMode.LOCAL_FILE to "本地自定义 .ndef 文件",
                PayloadMode.CUSTOM_HEX to "自定义 HEX 16 进制字节串"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { (mode, title) ->
                    val isSelected = selectedMode == mode
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeChange(mode) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onModeChange(mode) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // 文件选择输入
            if (selectedMode == PayloadMode.LOCAL_FILE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedFileName ?: "未选择文件",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Button(onClick = { filePickerLauncher.launch("*/*") }) {
                            Text("浏览选择")
                        }
                    }
                }
            }

            // HEX 输入框
            if (selectedMode == PayloadMode.CUSTOM_HEX) {
                OutlinedTextField(
                    value = customHex,
                    onValueChange = onHexChange,
                    label = { Text("输入 16 进制字符串") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    placeholder = { Text("如: D1012C5402656E...") },
                    maxLines = 4
                )
            }

            // Records Breakdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "解析结果 (${recordsInfo.size} 条 Record):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    recordsInfo.forEach { recordLine ->
                        Text(
                            text = recordLine,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 独立的 Shizuku 权限与状态页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuDestinationScreen(
    isShizukuRunning: Boolean,
    isShizukuPermissionGranted: Boolean,
    shizukuVersion: Int,
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shizuku 免 Root 特权", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    if (isShizukuRunning && isShizukuPermissionGranted) Color(0xFF4CAF50) else Color(0xFFE57373),
                                    CircleShape
                                )
                        )
                        Text(
                            text = if (isShizukuRunning) "服务正在运行 (v$shizukuVersion)" else "服务未启动",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isShizukuPermissionGranted) "已获得 UID 2000 (Shell) 权限，允许向系统 NFC 适配器派发虚拟 Tag" else "尚未授权 UID 2000 权限",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• 隐藏 API: 已通过 HiddenApiBypass 全局免反射限制",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isShizukuPermissionGranted && isShizukuRunning) {
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("立即申请 Shizuku 授权")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 独立的关于页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutDestinationScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于 Shizuku NFC", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.my_background),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp)),
                tint = Color.Unspecified
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Shizuku NFC",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "版本 1.0.0 (免 Root 碰一下模拟器)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "基于 Shizuku (UID 2000) 机制调用系统 INfcAdapter 派发硬件 Tag 事件，完美触发支付宝碰一下与小程序引擎。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 独立的界面显示与动效个性化设置页面（完全对齐 shizuku-ftp/SettingScreenUi）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiSettingsDestinationScreen(
    uiPreferencesViewModel: UiPreferencesViewModel?,
    colorBag: ColorBag = ColorBag(),
    onBack: () -> Unit
) {
    val prefs = LocalContext.current.getSharedPreferences("ui_state", android.content.Context.MODE_PRIVATE)

    var blurIntensity by remember {
        mutableFloatStateOf(uiPreferencesViewModel?.getBlurIntensity() ?: UiPreferences.getBlurIntensity(prefs))
    }
    var stateBarPressDown by remember {
        mutableStateOf(uiPreferencesViewModel?.getTopComponentPressedDown() ?: UiPreferences.getTopComponentPressedDown(prefs))
    }
    var usrM3ToPickColors by remember {
        mutableStateOf(uiPreferencesViewModel?.getUsrM3ToPickColors() ?: UiPreferences.getUsrM3ToPickColors(prefs))
    }
    var glassSideMenuWallpaper by remember {
        mutableStateOf(uiPreferencesViewModel?.getGlassSideMenuWallpaper() ?: UiPreferences.getGlassSideMenuWallpaper(prefs))
    }
    var sideMenuSpringAnimation by remember {
        mutableStateOf(uiPreferencesViewModel?.getSideMenuSpringAnimation() ?: UiPreferences.getSideMenuSpringAnimation(prefs))
    }
    var experimentalHaze by remember {
        mutableStateOf(uiPreferencesViewModel?.getExperimentalHaze() ?: UiPreferences.getExperimentalHaze(prefs))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("界面与个性化", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "毛玻璃与视觉效果",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SliderRow(
                sliderStep = 7,
                minSlideValue = 5f,
                maxSlideValue = 40f,
                sliderTitle = "背景模糊强度",
                sliderDescription = "调整全局毛玻璃底衬及侧栏磨砂采样的高斯模糊半径",
                rememberedSliderPosition = blurIntensity,
                onSliderValueChange = { newValue ->
                    blurIntensity = newValue
                    uiPreferencesViewModel?.setBlurIntensity(newValue)
                },
                colorBag = colorBag
            )

            SwitchPrefRow(
                title = "侧栏毛玻璃背景投影",
                description = "在滑动的侧栏抽屉上渲染半透明毛玻璃背景；关闭则显示纯色面板",
                checked = glassSideMenuWallpaper,
                colorBag = colorBag,
                onCheckedChange = {
                    glassSideMenuWallpaper = it
                    uiPreferencesViewModel?.setGlassSideMenuWallpaper(it)
                }
            )

            SwitchPrefRow(
                title = "低版本实验性模糊兼容",
                description = "在非 Android 12+ 原生 RenderEffect 环境尝试强制开启模糊着色器",
                checked = experimentalHaze,
                colorBag = colorBag,
                onCheckedChange = {
                    experimentalHaze = it
                    uiPreferencesViewModel?.setExperimentalHaze(it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

            Text(
                text = "动效与布局适配",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SwitchPrefRow(
                title = "侧栏弹性弹簧阻尼动画",
                description = "抽屉展开与收起时启用物理弹性回弹阻尼动效（关闭可降低部分设备负载）",
                checked = sideMenuSpringAnimation,
                colorBag = colorBag,
                onCheckedChange = {
                    sideMenuSpringAnimation = it
                    uiPreferencesViewModel?.setSideMenuSpringAnimation(it)
                }
            )

            SwitchPrefRow(
                title = "顶部组件下压适配",
                description = "留出手机前置挖孔与屏下视距，保持主屏幕视觉开阔与舒适下移",
                checked = stateBarPressDown,
                colorBag = colorBag,
                onCheckedChange = {
                    stateBarPressDown = it
                    uiPreferencesViewModel?.setTopComponentPressedDown(it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

            Text(
                text = "主题色彩提取",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SwitchPrefRow(
                title = "使用 Material 3 动态取色",
                description = "开启优先使用系统 Material You 色盘；关闭时智能匹配当前自定义壁纸基色",
                checked = usrM3ToPickColors,
                colorBag = colorBag,
                onCheckedChange = {
                    usrM3ToPickColors = it
                    uiPreferencesViewModel?.setUsrM3ToPickColors(it)
                }
            )
        }
    }
}
