package com.practice.advanced_compose_ui_practice.shorts.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.practice.advanced_compose_ui_practice.shorts.utils.PagerState
import com.practice.advanced_compose_ui_practice.shorts.utils.clickableWithoutRipple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayer(
    videoUrl: String,
    pagerState: PagerState,
    pager: Int,
    pauseIconVisibleState: MutableState<Boolean>
) {
    val context = LocalContext.current
    val scope= rememberCoroutineScope()

    val exoPlayer = remember {
        SimpleExoPlayer.Builder(context)
            .build()
            .apply {
                val simpleExoPlayer = SimpleExoPlayer.Builder(context).build()

                // MediaItem을 만들고
                val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))

                // MediaSource를 만들고
                val userAgent = Util.getUserAgent(context, context.applicationInfo.name)
                val factory = DefaultDataSourceFactory(context, userAgent)
                val progressiveMediaSource = ProgressiveMediaSource.Factory(factory).createMediaSource(mediaItem)

                // 만들어진 MediaSource를 연결
                simpleExoPlayer.setMediaSource(progressiveMediaSource)

                // Player 준비
                simpleExoPlayer.prepare()
            }
    }
    if (pager == pagerState.currentPage) {
        exoPlayer.playWhenReady = true
        exoPlayer.play()
    } else {
        exoPlayer.pause()
    }
    exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE

    DisposableEffect(
        Box(modifier = Modifier.fillMaxSize()){
            AndroidView(factory = {
                PlayerView(context).apply {
                    hideController()
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },modifier = Modifier.clickableWithoutRipple {
                pauseIconVisibleState.value=true
                exoPlayer.pause()
                scope.launch {
                    delay(500)
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        pauseIconVisibleState.value=false
                        exoPlayer.play()
                    }
                }
            })
            if (pauseIconVisibleState.value)
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(80.dp))
        }
    ) {
        onDispose {
            exoPlayer.release()
        }
    }

}