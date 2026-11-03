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
 * 侧栏及详情页状态管理（完全对齐 shizuku-ftp/SideMenuState，支持从主屏幕直接打开详情并直退主屏幕）
 */
@Stable
class SideMenuState(
    menu: SideMenu? = null,
    detailRoute: String? = null,
    openedDirectly: Boolean = false,
) {
    var menu by mutableStateOf(menu)
        private set
    var detailRoute by mutableStateOf(detailRoute)
        private set
    var openedDirectly by mutableStateOf(openedDirectly)
        private set

    val isOpen: Boolean get() = menu != null

    fun open(menu: SideMenu) {
        openedDirectly = false
        detailRoute = null
        this.menu = menu
    }

    fun openDirectDetail(menu: SideMenu, route: String) {
        openedDirectly = true
        this.menu = menu
        this.detailRoute = route
    }

    fun showDetail(route: String) {
        openedDirectly = false
        detailRoute = route
    }

    fun dismissDetail() {
        if (openedDirectly) {
            close()
        } else {
            detailRoute = null
        }
    }

    fun close() {
        openedDirectly = false
        detailRoute = null
        menu = null
    }

    fun back() {
        if (detailRoute != null) {
            if (openedDirectly) {
                close()
            } else {
                dismissDetail()
            }
        } else {
            close()
        }
    }

    companion object {
        val Saver = listSaver<SideMenuState, String>(
            save = { listOf(it.menu?.name.orEmpty(), it.detailRoute.orEmpty(), it.openedDirectly.toString()) },
            restore = {
                SideMenuState(
                    menu = it[0].takeIf(String::isNotEmpty)?.let(SideMenu::valueOf),
                    detailRoute = it[1].takeIf(String::isNotEmpty),
                    openedDirectly = it.getOrNull(2)?.toBooleanStrictOrNull() ?: false,
                )
            },
        )
    }
}

@Composable
fun rememberSideMenuState(): SideMenuState = rememberSaveable(saver = SideMenuState.Saver) {
    SideMenuState()
}
