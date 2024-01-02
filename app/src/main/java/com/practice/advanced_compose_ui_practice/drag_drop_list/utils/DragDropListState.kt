package com.practice.advanced_compose_ui_practice.drag_drop_list.utils

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Job

@Composable
fun rememberDragDropListState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (Int, Int) -> Unit
): DragDropListState {
    return remember {
        DragDropListState(lazyListState, onMove)
    }
}

class DragDropListState(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    // 해당 아이템이 얼마만큼 드래그 되었는지를 거리로 나타냄(픽셀)
    var draggedDistance by mutableStateOf(0f)

    // 드래그가 되는 대상의 아이템에 대한 정보 : LazyListItemInfo
    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

    // 드래르가 되는 대상의 아이템이 현재 어느 인덱스에 위치하는지에 대한 정보 : wjdtn
    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

    // 드래그 되는 대상의 드래그 전 상단 끝, 하단 끝 오프셋을 얻어옴
    val initialOffsets: Pair<Int, Int>?
        get() = initiallyDraggedElement?.let { lazyListItemInfo ->
            // 현재 드래그 되고 있는 아이템의 상단 끝 오프셋과 하단 끝 오프셋을 얻어옴
            Pair(lazyListItemInfo.offset, lazyListItemInfo.offsetEnd)
        }

    val elementDisplacement: Float?
        get() = currentIndexOfDraggedItem?.let {
            lazyListState.getVisibleItemInfoFor(absolute = it)
        }?.let { lazyListItemInfo ->
            (initiallyDraggedElement?.offset?.toFloat() ?: 0f) + draggedDistance - lazyListItemInfo.offset
        }

    val currentElement: LazyListItemInfo?
        get() = currentIndexOfDraggedItem?.let {
            lazyListState.getVisibleItemInfoFor(it)
        }

    var overScrollJob by mutableStateOf<Job?>(null)

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { lazyListItemInfo ->
            offset.y.toInt() in lazyListItemInfo.offset..(lazyListItemInfo.offset + lazyListItemInfo.size)
        }?.also {
            currentIndexOfDraggedItem = it.index
            initiallyDraggedElement = it
        }
    }

    fun onDragInterrupted() {
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        initiallyDraggedElement = null
        overScrollJob?.cancel()
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset.y

        initialOffsets?.let { (topOffset, bottomOffset) ->
            val startOffset = topOffset + draggedDistance
            val endOffset = bottomOffset + draggedDistance

            currentElement?.let { hovered ->
                lazyListState.layoutInfo.visibleItemsInfo.filterNot { lazyListItemInfo ->
                    lazyListItemInfo.offsetEnd < startOffset || lazyListItemInfo.offset > endOffset || hovered.index == lazyListItemInfo.index
                }.firstOrNull { lazyListItemInfo ->
                    val delta = startOffset - hovered.offset
                    when {
                        delta > 0 -> (endOffset > lazyListItemInfo.offsetEnd)
                        else -> (startOffset < lazyListItemInfo.offset)
                    }
                }?.also { lazyListItemInfo ->
                    currentIndexOfDraggedItem?.let { current ->
                        onMove.invoke(current, lazyListItemInfo.index)
                    }
                    currentIndexOfDraggedItem = lazyListItemInfo.index
                }
            }
        }
    }

    fun checkForOverScroll(): Float {
        return initiallyDraggedElement?.let {
            val startOffset = it.offset + draggedDistance
            val endOffset = it.offsetEnd + draggedDistance

            return@let when {
                draggedDistance > 0 -> (endOffset - lazyListState.layoutInfo.viewportEndOffset).takeIf { diff ->
                    diff > 0
                }
                draggedDistance < 0 -> (startOffset - lazyListState.layoutInfo.viewportStartOffset).takeIf { diff ->
                    diff < 0
                }
                else -> null
            }
        } ?: 0f
    }
}