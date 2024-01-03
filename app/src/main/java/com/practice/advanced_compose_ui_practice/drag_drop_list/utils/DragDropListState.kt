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
    private var draggedDistance by mutableStateOf(0f)

    //-------------------------------드래그 시작 시 설정-------------------------------

    // 드래그가 되는 대상의 아이템에 대한 초기(드래그 전)정보 : LazyListItemInfo
    private var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

    // 드래르가 되는 대상의 아이템이 현재 어느 인덱스에 위치하는지에 대한 정보 : 정수
    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

    //------------------------------------------------------------------------------

    // 드래그 되는 대상의 드래그 전 상단 끝, 하단 끝 오프셋을 얻어옴
    private val initialOffsets: Pair<Int, Int>?
        get() = initiallyDraggedElement?.let { lazyListItemInfo ->
            // 현재 드래그 되고 있는 아이템의 상단 끝 오프셋과 하단 끝 오프셋을 얻어옴
            Pair(lazyListItemInfo.offset, lazyListItemInfo.offsetEnd)
        }

    // 현재 드래그되고 있는 아이템의 이동 거리를 나타내는 속성.
    // 이 값은 픽셀 단위로 표시되며, 드래그가 진행될 때마다 업데이트
    val elementDisplacement: Float?
        get() = currentIndexOfDraggedItem?.let {
            lazyListState.getVisibleItemInfoFor(absolute = it) // LazyListItemInfo
        }?.let { lazyListItemInfo ->
            // LazyListItemInfo를 사용하여 현재 드래그 중인 아이템의 이동 거리를 계산
            // 아이템의 초기 오프셋에서 드래그된 거리를 더하고,
            // 그 값에서 현재 아이템의 오프셋을 빼서 이동 거리를 얻음
            (initiallyDraggedElement?.offset?.toFloat() ?: 0f) + draggedDistance - lazyListItemInfo.offset
        }

    private val currentElement: LazyListItemInfo?
        get() = currentIndexOfDraggedItem?.let {
            lazyListState.getVisibleItemInfoFor(it)
        }

    private var overScrollJob by mutableStateOf<Job?>(null)

    // 드래그가 시작될 때 호출되며, 드래그 대상 아이템의 인덱스와 초기 정보를 설정합니다.
    // offset : representing the last known pointer position relative to the containing element.
    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { lazyListItemInfo ->
            offset.y.toInt() in lazyListItemInfo.offset..(lazyListItemInfo.offset + lazyListItemInfo.size)
        }?.also {
            currentIndexOfDraggedItem = it.index
            initiallyDraggedElement = it
        }
    }

    // 드래그 동작이 비정상적인 모종의 사정으로 중단되었을 때 원상태로 복구시키기 위한 함수
    fun onDragInterrupted() {
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        initiallyDraggedElement = null
        overScrollJob?.cancel()
    }

    // 드래그가 진행될 때 호출되며, 드래그 된 거리를 계산하고, 아이템 이동을 처리
    // offset : 드래그 된 양(위아래로 드래그된 만큼을 나타내는 것이 offset.y -> float 형태로, 위면 +, 아래면 -)
    fun onDrag(offset: Offset) {
        // 현재 드래그된 거리를 누적하여 업데이트
        draggedDistance += offset.y

        initialOffsets?.let { (topOffset, bottomOffset) ->
            // 드래그된 거리에 따라 현재 시작 및 끝 오프셋을 계산
            val startOffset = topOffset + draggedDistance
            val endOffset = bottomOffset + draggedDistance

            // 현재 드래그 중인 아이템에 대한 정보
            currentElement?.let { hovered ->
                lazyListState.layoutInfo.visibleItemsInfo.filterNot { lazyListItemInfo ->
                    // 현재 화면에 보이는 아이템 중에서 드래그 중인 아이템(즉 1개 -> 이후 firstOrNull로 걸러냄)을 제외하고 필터링
                    lazyListItemInfo.offsetEnd < startOffset || lazyListItemInfo.offset > endOffset || hovered.index == lazyListItemInfo.index
                }.firstOrNull { lazyListItemInfo ->
                    val delta = startOffset - hovered.offset
                    when {
                        // 위로 이동했는지
                        delta > 0 -> (endOffset > lazyListItemInfo.offsetEnd)
                        // 아래로 이동했는지
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