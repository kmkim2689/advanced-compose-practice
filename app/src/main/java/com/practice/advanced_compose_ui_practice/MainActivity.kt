package com.practice.advanced_compose_ui_practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import com.practice.advanced_compose_ui_practice.drag_drop_list.DragDropList
import com.practice.advanced_compose_ui_practice.drag_drop_list.utils.move
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
//                    ComposeThemeScreen(
//                        modifier = Modifier.fillMaxSize(),
//                        onItemSelected = { changedTheme ->
//                            theme = changedTheme
//                        }
//                    )
                    DragDropList(
                        items = reorderItems,
                        modifier = Modifier.fillMaxSize(),
                        onMove = { fromIndex, toIndex ->
                            reorderItems.move(fromIndex, toIndex)
                        }
                    )
                }
            }
        }
    }
}

// 왠지 모르겠지만, 이 리스트의 위치가 상당히 중요
// 액티비티 내부에 이것을 넣으면 변경되지 않음...
// 즉, 1을 2 뒤로 옮기면 2, 1 순서가 되어야 하는데 1,2 순서로 바뀌지 않는다는 것...
val reorderItems = listOf<String>(
    "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"
).toMutableStateList() // 이렇게 해주어야 위치 변경 시 올바르게 값이 바뀌지 않고 원하는 대로 유지된다.

