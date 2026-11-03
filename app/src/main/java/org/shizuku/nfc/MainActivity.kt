package org.shizuku.nfc

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.shizuku.nfc.ui.MainScreen
import org.shizuku.nfc.ui.ShizukuNfcTheme
import org.shizuku.nfc.ui.UiPreferencesViewModel
import org.shizuku.nfc.ui.WallpaperControlsTheme
import org.shizuku.nfc.ui.data.ColorBag
import org.shizuku.nfc.ui.data.WallpaperColorEnum
import org.shizuku.nfc.ui.util.WallpaperPalette
import org.shizuku.nfc.ui.util.rememberWallpaperAccentColor
import org.shizuku.nfc.ui.viewmodel.WallpaperViewModel
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }

    private val uiPreferencesViewModel: UiPreferencesViewModel by viewModels()
    private val wallpaperViewModel: WallpaperViewModel by viewModels()

    private var isShizukuRunning by mutableStateOf(false)
    private var isShizukuPermissionGranted by mutableStateOf(false)
    private var shizukuVersion by mutableIntStateOf(-1)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isShizukuRunning = false
        isShizukuPermissionGranted = false
        shizukuVersion = -1
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                isShizukuPermissionGranted = (grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions("L")
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
        enableEdgeToEdge()
        hideSystemStatusBar()

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)

        checkShizukuState()

        setContent {
            ShizukuNfcTheme {
                val wallpaperBitmap: ImageBitmap? = wallpaperViewModel.wallpaper.collectAsState().value
                val usrM3 = uiPreferencesViewModel.usrM3ToPickColors.collectAsState().value
                val colorBag = if (LocalInspectionMode.current) {
                    ColorBag(
                        vibrant = Color(0xFF90CAF9),
                        darkMuted = Color(0xFF1E3A5F),
                        lightMuted = Color(0xFFD1E4FA),
                        muted = Color(0xFF64B5F6),
                        useM3Color = false
                    )
                } else {
                    ColorBag(
                        vibrant = rememberWallpaperAccentColor(WallpaperPalette(bitmap = wallpaperBitmap)),
                        darkMuted = rememberWallpaperAccentColor(
                            WallpaperPalette(bitmap = wallpaperBitmap),
                            type = WallpaperColorEnum.DARK_MUTED
                        ),
                        lightMuted = rememberWallpaperAccentColor(
                            WallpaperPalette(bitmap = wallpaperBitmap),
                            type = WallpaperColorEnum.LIGHT_MUTED
                        ),
                        muted = rememberWallpaperAccentColor(
                            WallpaperPalette(bitmap = wallpaperBitmap),
                            type = WallpaperColorEnum.MUTED
                        ),
                        useM3Color = usrM3
                    )
                }

                WallpaperControlsTheme(colorBag = colorBag) {
                    MainScreen(
                        isShizukuRunning = isShizukuRunning,
                        isShizukuPermissionGranted = isShizukuPermissionGranted,
                        shizukuVersion = shizukuVersion,
                        onRequestPermission = {
                            requestShizukuPermission()
                        },
                        uiPreferencesViewModel = uiPreferencesViewModel,
                        wallpaperViewModel = wallpaperViewModel,
                        providedColorBag = colorBag
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemStatusBar()
        checkShizukuState()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemStatusBar()
        }
    }

    private fun hideSystemStatusBar() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }

    private fun checkShizukuState() {
        val ping = Shizuku.pingBinder()
        isShizukuRunning = ping
        if (ping) {
            shizukuVersion = Shizuku.getVersion()
            isShizukuPermissionGranted = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (_: Throwable) {
                false
            }
        } else {
            isShizukuPermissionGranted = false
            shizukuVersion = -1
        }
    }

    private fun requestShizukuPermission() {
        if (!isShizukuRunning) return
        if (Shizuku.isPreV11()) {
            // 旧版本不支持动态权限
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            isShizukuPermissionGranted = true
        } else {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }
    }
}
