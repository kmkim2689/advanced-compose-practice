package com.practice.advanced_compose_ui_practice.light_dark.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ThemeSettingPreference @Inject constructor(
    @ApplicationContext appContext: Context
) : ThemeSetting {

    override val themeStream: MutableStateFlow<AppTheme>
//    override var theme: AppTheme by AppThemePreferenceDelegate("app_theme", AppTheme.MODE_AUTO)

    override var theme: AppTheme = AppTheme.MODE_AUTO

    init {
        themeStream = MutableStateFlow(theme)
    }

    private val preferences: SharedPreferences = appContext.getSharedPreferences("sample_theme", Context.MODE_PRIVATE)

    inner class AppThemePreferenceDelegate(
        private val name: String,
        private val default: AppTheme
    ) : ReadWriteProperty<Any?, AppTheme> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): AppTheme {
            return AppTheme.fromOrdinal(preferences.getInt(name, default.order))!!
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: AppTheme) {
            themeStream.value = value
            val editor = preferences.edit()
            editor.putInt(name, value.order)
            editor.apply()
        }
    }
}