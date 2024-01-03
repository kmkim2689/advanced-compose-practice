package com.practice.advanced_compose_ui_practice.shorts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.practice.advanced_compose_ui_practice.shorts.utils.PagerState

@Composable
fun SingleVideoItemContent(
    videoUrl: String,
    pagerState: PagerState,
    pager: Int,
    initialLayout: MutableState<Boolean>,
    pauseIconVisibleState: MutableState<Boolean>,
    videoHeader: @Composable () -> Unit,
    videoBottom: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()){
        VideoPlayer(videoUrl,pagerState,pager,pauseIconVisibleState)
        videoHeader.invoke()
        Box(modifier = Modifier.align(Alignment.BottomStart)){
            videoBottom.invoke()
        }
        if (initialLayout.value) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black))
        }
    }
}