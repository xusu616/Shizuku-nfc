package org.shizuku.nfc.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI 设置状态 ViewModel（完全复刻 shizuku-ftp/UiPreferencesViewModel）
 */
class UiPreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("ui_state", Context.MODE_PRIVATE)

    // 1. 顶部组件下压适配
    private val _topComponentPressedDown = MutableStateFlow(
        UiPreferences.getTopComponentPressedDown(prefs)
    )
    val topComponentPressedDown = _topComponentPressedDown.asStateFlow()

    fun setTopComponentPressedDown(value: Boolean) {
        UiPreferences.setTopComponentPressedDown(prefs, value)
        _topComponentPressedDown.value = value
    }

    fun getTopComponentPressedDown(): Boolean =
        UiPreferences.getTopComponentPressedDown(prefs)

    // 2. 背景毛玻璃模糊强度
    private val _blurIntensity = MutableStateFlow(
        UiPreferences.getBlurIntensity(prefs)
    )
    val blurIntensity = _blurIntensity.asStateFlow()

    fun setBlurIntensity(value: Float) {
        UiPreferences.setBlurIntensity(prefs, value)
        _blurIntensity.value = value
    }

    fun getBlurIntensity(): Float =
        UiPreferences.getBlurIntensity(prefs)

    // 3. 使用 M3 取色
    private val _usrM3ToPickColors = MutableStateFlow(
        UiPreferences.getUsrM3ToPickColors(prefs)
    )
    val usrM3ToPickColors = _usrM3ToPickColors.asStateFlow()

    fun setUsrM3ToPickColors(value: Boolean) {
        UiPreferences.setUsrM3ToPickColors(prefs, value)
        _usrM3ToPickColors.value = value
    }

    fun getUsrM3ToPickColors(): Boolean =
        UiPreferences.getUsrM3ToPickColors(prefs)

    // 4. 侧栏毛玻璃背景壁纸投影
    private val _glassSideMenuWallpaper = mutableStateOf(
        UiPreferences.getGlassSideMenuWallpaper(prefs)
    )
    val glassSideMenuWallpaper: State<Boolean> = _glassSideMenuWallpaper

    fun setGlassSideMenuWallpaper(value: Boolean) {
        _glassSideMenuWallpaper.value = value
        UiPreferences.setGlassSideMenuWallpaper(prefs, value)
    }

    fun getGlassSideMenuWallpaper(): Boolean =
        _glassSideMenuWallpaper.value

    // 5. 侧栏弹性动画
    private val _sideMenuSpringAnimation = MutableStateFlow(
        UiPreferences.getSideMenuSpringAnimation(prefs)
    )
    val sideMenuSpringAnimation = _sideMenuSpringAnimation.asStateFlow()

    fun setSideMenuSpringAnimation(value: Boolean) {
        UiPreferences.setSideMenuSpringAnimation(prefs, value)
        _sideMenuSpringAnimation.value = value
    }

    fun getSideMenuSpringAnimation(): Boolean =
        UiPreferences.getSideMenuSpringAnimation(prefs)

    // 6. 实验性低版本模糊
    private val _experimentalHaze = MutableStateFlow(
        UiPreferences.getExperimentalHaze(prefs)
    )
    val experimentalHaze = _experimentalHaze.asStateFlow()

    fun setExperimentalHaze(value: Boolean) {
        UiPreferences.setExperimentalHaze(prefs, value)
        _experimentalHaze.value = value
    }

    fun getExperimentalHaze(): Boolean =
        UiPreferences.getExperimentalHaze(prefs)

    // 7. 使用显式 Intent 穿透（免 Shizuku）
    private val _useDirectIntent = MutableStateFlow(
        UiPreferences.getUseDirectIntent(prefs)
    )
    val useDirectIntent = _useDirectIntent.asStateFlow()

    fun setUseDirectIntent(value: Boolean) {
        UiPreferences.setUseDirectIntent(prefs, value)
        _useDirectIntent.value = value
    }

    fun getUseDirectIntent(): Boolean =
        UiPreferences.getUseDirectIntent(prefs)
}
