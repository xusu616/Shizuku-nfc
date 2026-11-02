package org.shizuku.nfc.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.view.RoundedCorner
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BatteryState(
    val percent: Int? = null,
    val isCharging: Boolean = false
)

@Composable
fun rememberBatteryState(): BatteryState {
    val context = LocalContext.current
    if (LocalInspectionMode.current) return BatteryState(percent = 85, isCharging = true)

    var state by remember { mutableStateOf(BatteryState()) }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val pct = if (level >= 0 && scale > 0) (level * 100) / scale else null
                    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    state = BatteryState(percent = pct, isCharging = charging)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }
    return state
}

@Composable
fun getCarmaHeight(): Dp {
    return WindowInsets.displayCutout
        .asPaddingValues()
        .calculateTopPadding()
}

@Composable
fun getRadius(): Dp {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val windowInsets = LocalView.current.rootWindowInsets
        val roundedCorner = windowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
        roundedCorner?.radius?.let { with(LocalDensity.current) { it.toDp() } } ?: 32.dp
    } else {
        32.dp
    }
}
