package org.shizuku.nfc.ui

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * UI 偏好设置持久化管理器（完全复刻 shizuku-ftp/UiPreferences 架构）
 */
object UiPreferences {
    const val PREF_KEY_TOP_COMPONENT_PRESSED_DOWN = "topComponentPressedDownPref"
    const val DEFAULT_TOP_COMPONENT_PRESSED_DOWN = true

    fun getTopComponentPressedDown(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_KEY_TOP_COMPONENT_PRESSED_DOWN, DEFAULT_TOP_COMPONENT_PRESSED_DOWN)

    fun setTopComponentPressedDown(prefs: SharedPreferences, pressedDown: Boolean) {
        prefs.edit { putBoolean(PREF_KEY_TOP_COMPONENT_PRESSED_DOWN, pressedDown) }
    }

    const val PREF_KEY_BLUR_INTENSITY = "blurIntensityPref"
    const val DEFAULT_BLUR_INTENSITY = 25f

    fun getBlurIntensity(prefs: SharedPreferences): Float =
        prefs.getFloat(PREF_KEY_BLUR_INTENSITY, DEFAULT_BLUR_INTENSITY)

    fun setBlurIntensity(prefs: SharedPreferences, intensity: Float) {
        prefs.edit { putFloat(PREF_KEY_BLUR_INTENSITY, intensity) }
    }

    const val PREF_KEY_USR_M3_TO_PICK_COLORS = "usrM3ToPickColorsPref"
    const val DEFAULT_USR_M3_TO_PICK_COLORS = false

    fun getUsrM3ToPickColors(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_KEY_USR_M3_TO_PICK_COLORS, DEFAULT_USR_M3_TO_PICK_COLORS)

    fun setUsrM3ToPickColors(prefs: SharedPreferences, value: Boolean) {
        prefs.edit { putBoolean(PREF_KEY_USR_M3_TO_PICK_COLORS, value) }
    }

    const val PREF_KEY_GLASS_SIDE_MENU_WALLPAPER = "glassSideMenuWallpaperPref"
    const val DEFAULT_GLASS_SIDE_MENU_WALLPAPER = true

    fun getGlassSideMenuWallpaper(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_KEY_GLASS_SIDE_MENU_WALLPAPER, DEFAULT_GLASS_SIDE_MENU_WALLPAPER)

    fun setGlassSideMenuWallpaper(prefs: SharedPreferences, value: Boolean) {
        prefs.edit { putBoolean(PREF_KEY_GLASS_SIDE_MENU_WALLPAPER, value) }
    }

    const val PREF_KEY_SIDE_MENU_SPRING_ANIMATION = "sideMenuSpringAnimationPref"
    const val DEFAULT_SIDE_MENU_SPRING_ANIMATION = true

    fun getSideMenuSpringAnimation(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_KEY_SIDE_MENU_SPRING_ANIMATION, DEFAULT_SIDE_MENU_SPRING_ANIMATION)

    fun setSideMenuSpringAnimation(prefs: SharedPreferences, value: Boolean) {
        prefs.edit { putBoolean(PREF_KEY_SIDE_MENU_SPRING_ANIMATION, value) }
    }

    const val PREF_KEY_EXPERIMENTAL_HAZE = "experimentalHazePref"
    const val DEFAULT_EXPERIMENTAL_HAZE = false

    fun getExperimentalHaze(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_KEY_EXPERIMENTAL_HAZE, DEFAULT_EXPERIMENTAL_HAZE)

    fun setExperimentalHaze(prefs: SharedPreferences, value: Boolean) {
        prefs.edit { putBoolean(PREF_KEY_EXPERIMENTAL_HAZE, value) }
    }

    const val PREF_KEY_USE_DIRECT_INTENT = "useDirectIntentPref"
    const val DEFAULT_USE_DIRECT_INTENT = true

    fun getUseDirectIntent(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_KEY_USE_DIRECT_INTENT, DEFAULT_USE_DIRECT_INTENT)

    fun setUseDirectIntent(prefs: SharedPreferences, value: Boolean) {
        prefs.edit { putBoolean(PREF_KEY_USE_DIRECT_INTENT, value) }
    }
}
