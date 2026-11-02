package org.shizuku.nfc.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

enum class SideMenu { SETTINGS, TOOLS }

/**
 * 侧栏及详情页状态管理（完全对齐 shizuku-ftp/SideMenuState）
 */
@Stable
class SideMenuState(
    menu: SideMenu? = null,
    detailRoute: String? = null,
) {
    var menu by mutableStateOf(menu)
        private set
    var detailRoute by mutableStateOf(detailRoute)
        private set

    val isOpen: Boolean get() = menu != null

    fun open(menu: SideMenu) {
        detailRoute = null
        this.menu = menu
    }

    fun showDetail(route: String) {
        detailRoute = route
    }

    fun dismissDetail() {
        detailRoute = null
    }

    fun close() {
        detailRoute = null
        menu = null
    }

    fun back() {
        if (detailRoute != null) dismissDetail() else close()
    }

    companion object {
        val Saver = listSaver<SideMenuState, String>(
            save = { listOf(it.menu?.name.orEmpty(), it.detailRoute.orEmpty()) },
            restore = {
                SideMenuState(
                    menu = it[0].takeIf(String::isNotEmpty)?.let(SideMenu::valueOf),
                    detailRoute = it[1].takeIf(String::isNotEmpty),
                )
            },
        )
    }
}

@Composable
fun rememberSideMenuState(): SideMenuState = rememberSaveable(saver = SideMenuState.Saver) {
    SideMenuState()
}
