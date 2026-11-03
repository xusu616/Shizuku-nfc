package org.shizuku.nfc.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1677FF), // Alipay Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6F4FF),
    onPrimaryContainer = Color(0xFF003A8C),
    secondary = Color(0xFF52C41A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6FFED),
    onSecondaryContainer = Color(0xFF135200),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF1F1F1F),
    background = Color(0xFFF0F2F5),
    onBackground = Color(0xFF1F1F1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4096FF),
    onPrimary = Color(0xFF002766),
    primaryContainer = Color(0xFF112A45),
    onPrimaryContainer = Color(0xFF91CAFF),
    secondary = Color(0xFF73D13D),
    onSecondary = Color(0xFF0D3800),
    secondaryContainer = Color(0xFF1E381E),
    onSecondaryContainer = Color(0xFFB7EB8F),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF0F0F0),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ShizukuNfcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes
    ) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
