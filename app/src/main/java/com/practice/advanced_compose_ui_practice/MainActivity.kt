package com.practice.advanced_compose_ui_practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.practice.advanced_compose_ui_practice.light_dark.ComposeThemeScreen
import com.practice.advanced_compose_ui_practice.light_dark.utils.AppTheme
import com.practice.advanced_compose_ui_practice.light_dark.utils.ThemeSetting
import com.practice.advanced_compose_ui_practice.ui.theme.MyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeSetting: ThemeSetting

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var theme by rememberSaveable {
                mutableStateOf(AppTheme.MODE_AUTO)
            }
            // boolean value whether to use dark theme or not
            val useDarkColors = when (theme) {
                AppTheme.MODE_AUTO -> isSystemInDarkTheme()
                AppTheme.MODE_DAY -> false
                AppTheme.MODE_NIGHT -> true
            }

            MyTheme(
                // darktheme
                darkTheme = useDarkColors
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ComposeThemeScreen(
                        modifier = Modifier.fillMaxSize(),
                        onItemSelected = { changedTheme ->
                            theme = changedTheme
                        }
                    )
                }
            }
        }
    }
}
