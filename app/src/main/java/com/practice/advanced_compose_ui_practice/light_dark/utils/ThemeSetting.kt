package com.practice.advanced_compose_ui_practice.light_dark.utils

import kotlinx.coroutines.flow.StateFlow

enum class AppTheme(val order: Int) {
    MODE_AUTO(0), // 0
    MODE_DAY(1), // 1
    MODE_NIGHT(2); // 2

    companion object {
        // index로 enum요소에 접근
        fun fromOrdinal(ordinal: Int) = values().find { it.order == ordinal }
    }
}

interface ThemeSetting {
    val themeStream: StateFlow<AppTheme>
    var theme: AppTheme
}