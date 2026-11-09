package org.shizuku.nfc.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.shizuku.nfc.R
import org.shizuku.nfc.ui.data.ColorBag
import java.net.HttpURLConnection
import java.net.URL

private const val GITHUB_REPO = "rD227/Shizuku-nfc"
private const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
private const val GITLAB_RELEASES_API = "https://gitlab.com/api/v4/projects/xusu616%2FShizuku-nfc/releases?per_page=1"
private const val LATEST_RELEASE_WEB = "https://github.com/$GITHUB_REPO/releases/latest"
private const val PREFS_STARTUP = "startup_experience"
private const val PREF_KEY_IGNORED_VERSION = "ignored_update_version"

/**
 * 自动更新检查 ViewModel（完全仿照 shizuku-ftp/UpdatePromptViewModel）
 */
class UpdatePromptViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences(PREFS_STARTUP, Context.MODE_PRIVATE)

    var updateVersion by mutableStateOf<String?>(null)
        private set

    var isChecking by mutableStateOf(false)
        private set

    private var checkedOnce = false

    /**
     * 应用启动时自动触发一次检查，如果版本已被用户忽略或已是最新版则静默
     */
    fun checkOnce() {
        if (checkedOnce) return
        checkedOnce = true
        viewModelScope.launch {
            val latest = withContext(Dispatchers.IO) { fetchLatestVersionFromGithub() }
            val current = getAppVersionName(getApplication())
            val ignored = prefs.getString(PREF_KEY_IGNORED_VERSION, null)

            if (latest != null && latest != ignored && compareVersions(current, latest) < 0) {
                updateVersion = latest
            }
        }
    }

    /**
     * 用户在 UI 手动点击“检查更新”时调用，带状态回调
     * [onComplete] 回调参数：(latestVersion: 远程最新版本号，若请求失败则为 null, isNewAvailable: 是否有更高版本)
     */
    fun checkManually(onComplete: (latest: String?, isNewAvailable: Boolean) -> Unit) {
        if (isChecking) return
        isChecking = true
        viewModelScope.launch {
            val latest = withContext(Dispatchers.IO) { fetchLatestVersionFromGithub() }
            val current = getAppVersionName(getApplication())
            isChecking = false

            val isNew = latest != null && compareVersions(current, latest) < 0
            if (isNew) {
                updateVersion = latest
            }
            onComplete(latest, isNew)
        }
    }

    fun dismissUpdate() {
        updateVersion = null
    }

    fun ignoreCurrentVersion(version: String) {
        prefs.edit { putString(PREF_KEY_IGNORED_VERSION, version) }
        updateVersion = null
    }
}

/**
 * 启动时自动弹窗入口组件
 */
@Composable
fun StartupUpdatePrompt(
    ready: Boolean = true,
    viewModel: UpdatePromptViewModel,
    colorBag: ColorBag = ColorBag()
) {
    LaunchedEffect(ready) {
        if (ready) {
            viewModel.checkOnce()
        }
    }

    val updateVersion = viewModel.updateVersion
    if (ready && updateVersion != null) {
        UpdateAvailableDialog(
            version = updateVersion,
            colorBag = colorBag,
            onDismiss = { viewModel.dismissUpdate() },
            onIgnore = { viewModel.ignoreCurrentVersion(updateVersion) }
        )
    }
}

/**
 * 发现新版本更新弹窗（完全仿照 shizuku-ftp/UpdateAvailableDialog）
 */
@Composable
fun UpdateAvailableDialog(
    version: String,
    colorBag: ColorBag = ColorBag(),
    onDismiss: () -> Unit,
    onIgnore: () -> Unit = {
        // 默认存入 ignored_update_version 并关闭
    }
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS_STARTUP, Context.MODE_PRIVATE) }
    val later = onDismiss

    WallpaperControlsTheme(colorBag) {
        CompactSettingsDialog(
            onDismissRequest = later,
            title = {
                Text(stringResource(R.string.update_available_title, version))
            },
            text = {
                Text(stringResource(R.string.update_available_body))
            },
            dismissButton = {
                TextButton(onClick = {
                    prefs.edit { putString(PREF_KEY_IGNORED_VERSION, version) }
                    onIgnore()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.update_ignore_version))
                }
                TextButton(onClick = later) {
                    Text(stringResource(R.string.update_later))
                }
            },
            confirmButton = {
                val tag = if (version.startsWith("v", ignoreCase = true)) version else "v$version"
                val webUrl = "https://github.com/$GITHUB_REPO/releases/tag/$tag"
                val directDownloadUrl = "https://github.com/$GITHUB_REPO/releases/download/$tag/shizuku-nfc-release.apk"

                TextButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUrl.toUri()))
                            onDismiss()
                        } catch (_: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, R.string.update_no_browser, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.update_browser))
                }
                TextButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, directDownloadUrl.toUri()))
                            onDismiss()
                        } catch (_: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, R.string.update_no_browser, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.update_direct_download))
                }
            }
        )
    }
}

// ─── 辅助工具函数 ───

internal fun getAppVersionName(context: Context): String {
    return try {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }
}

internal fun fetchLatestVersionFromGithub(): String? {
    val githubVersion = try {
        val connection = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Shizuku-nfc/update-check")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body).optString("tag_name").takeIf { it.isNotBlank() }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    if (githubVersion != null) {
        return githubVersion
    }

    return fetchLatestVersionFromGitlab()
}

internal fun fetchLatestVersionFromGitlab(): String? {
    return try {
        val connection = URL(GITLAB_RELEASES_API).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Shizuku-nfc/update-check")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val first = array.optJSONObject(0)
                    first?.optString("tag_name")?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * 版本号对比函数（复刻自 shizuku-ftp/AboutScreen.compareVersions）
 * 例如 "v1.2.0" 与 "1.2.1"
 * a < b 返回负数；a == b 返回 0；a > b 返回正数
 */
internal fun compareVersions(a: String, b: String): Int {
    fun numericParts(version: String): List<Int> = version
        .trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore('-')
        .split('.')
        .map { it.toIntOrNull() ?: 0 }

    val aParts = numericParts(a)
    val bParts = numericParts(b)
    for (i in 0 until maxOf(aParts.size, bParts.size)) {
        val aPart = aParts.getOrElse(i) { 0 }
        val bPart = bParts.getOrElse(i) { 0 }
        if (aPart != bPart) return aPart.compareTo(bPart)
    }
    return 0
}

// ─── 完整保留 @Preview ───

@Preview(name = "Update Dialog Light", showBackground = true, locale = "zh")
@Composable
fun UpdateAvailableDialogPreviewLight() {
    ShizukuNfcTheme(darkTheme = false) {
        UpdateAvailableDialog(
            version = "v1.1.0",
            onDismiss = {},
            onIgnore = {}
        )
    }
}

@Preview(name = "Update Dialog Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "zh")
@Composable
fun UpdateAvailableDialogPreviewDark() {
    ShizukuNfcTheme(darkTheme = true) {
        UpdateAvailableDialog(
            version = "v1.2.0-beta",
            onDismiss = {},
            onIgnore = {}
        )
    }
}
