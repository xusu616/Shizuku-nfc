package org.shizuku.nfc

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import org.shizuku.nfc.ui.MainScreen
import org.shizuku.nfc.ui.ShizukuNfcTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }

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

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)

        checkShizukuState()

        setContent {
            ShizukuNfcTheme {
                MainScreen(
                    isShizukuRunning = isShizukuRunning,
                    isShizukuPermissionGranted = isShizukuPermissionGranted,
                    shizukuVersion = shizukuVersion,
                    onRequestPermission = {
                        requestShizukuPermission()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkShizukuState()
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
