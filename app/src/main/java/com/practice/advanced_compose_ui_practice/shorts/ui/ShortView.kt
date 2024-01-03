package com.practice.advanced_compose_ui_practice.shorts.ui

import android.app.Activity
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.practice.advanced_compose_ui_practice.shorts.utils.PagerState
import com.practice.advanced_compose_ui_practice.shorts.utils.immersive
import kotlinx.coroutines.delay

@Composable
fun ShortView(
    activity: Activity,
    videoItemsUrl: List<String>,
    clickItemPosition: Int = 0,
    videoHeader: @Composable () -> Unit = {},
    videoBottom: @Composable () -> Unit = {}
) {
   activity.immersive(darkMode = true)
   val pagerState: PagerState = run {
       remember {
           PagerState(
               clickItemPosition, 0, videoItemsUrl.size - 1
           )
       }
   }

    val initialLayout = remember {
        mutableStateOf(true)
    }

    val pauseIconVisibleState = remember {
        mutableStateOf(false)
    }

    Pager(
        state = pagerState,
        orientation = Orientation.Vertical,
        offScreenLimit = 1
    ) {
        pauseIconVisibleState.value = false
        SingleVideoItemContent(
            videoItemsUrl[page],
            pagerState,
            page,
            initialLayout,
            pauseIconVisibleState,
            videoHeader,
            videoBottom
        )
    }

    LaunchedEffect(key1 = clickItemPosition) {
        delay(200)
        initialLayout.value = false
    }
}

