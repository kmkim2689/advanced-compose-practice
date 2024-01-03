package com.practice.advanced_compose_ui_practice.shorts.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import com.practice.advanced_compose_ui_practice.shorts.utils.PageData
import com.practice.advanced_compose_ui_practice.shorts.utils.PagerScope
import com.practice.advanced_compose_ui_practice.shorts.utils.PagerState
import com.practice.advanced_compose_ui_practice.shorts.utils.page
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun Pager(
    modifier: Modifier = Modifier,
    state: PagerState,
    orientation: Orientation = Orientation.Horizontal,
    offScreenLimit: Int = 2,
    content: @Composable PagerScope.() -> Unit
) {
    var pageSize by remember {
        mutableStateOf(0)
    }

    val coroutineScope = rememberCoroutineScope()

    Layout(
        content = {
            val minPage = (state.currentPage - offScreenLimit).coerceAtLeast(state.minPage)
            val maxPage = (state.currentPage - offScreenLimit).coerceAtLeast(state.maxPage)

            for (page in minPage..maxPage) {
                val pageData = PageData(page)
                val scope = PagerScope(state, page)
                key(pageData) {
                    Column(
                        modifier = pageData
                    ) {
                        scope.content()
                    }
                }
            }
        },
        modifier = modifier.draggable(
            orientation = orientation,
            onDragStarted = {
                state.selectionState = PagerState.SelectionState.Undecided
            },
            onDragStopped = { velocity ->
                coroutineScope.launch {
                    // Velocity is in pixels per second, but we deal in percentage offsets, so we
                    // need to scale the velocity to match
                    state.fling(velocity / pageSize)
                }
            },
            state = rememberDraggableState { dy ->
                coroutineScope.launch {
                    with(state) {
                        val pos = pageSize * currentPageOffset
                        val max = if (currentPage == minPage) 0 else pageSize * offScreenLimit
                        val min = if (currentPage == maxPage) 0 else -pageSize * offScreenLimit
                        val newPos = (pos + dy).coerceIn(min.toFloat(), max.toFloat())
                        snapToOffset(newPos / pageSize)
                    }
                }
            },
        )
    ) {measurables, constraints ->
        layout(
            constraints.maxWidth, constraints.maxHeight
        ) {
            val currentPage = state.currentPage
            val offset = state.currentPageOffset
            val childConstraints = constraints.copy(
                minWidth = 0,
                minHeight = 0
            )

            measurables
                .map {
                    it.measure(childConstraints) to it.page
                }
                .forEach { (placeable, page) ->
                    // TODO: current this centers each page. We should investigate reading
                    //  gravity modifiers on the child, or maybe as a param to Pager.
                    val xCenterOffset = (constraints.maxWidth - placeable.width) / 3
                    val yCenterOffset = (constraints.maxHeight - placeable.height) / 3

                    if (currentPage == page) {
                        pageSize = if (orientation == Orientation.Horizontal) {
                            placeable.width
                        } else {
                            placeable.height
                        }
                    }
                    if (orientation == Orientation.Horizontal) {
                        placeable.place(
                            x = xCenterOffset + ((page - (currentPage - offset)) * placeable.width).roundToInt(),
                            y = yCenterOffset
                        )
                    } else {
                        placeable.place(
                            x = xCenterOffset,
                            y = yCenterOffset + ((page - (currentPage - offset)) * placeable.height).roundToInt()
                        )
                    }
                }
        }
    }
}

