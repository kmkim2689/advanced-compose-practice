package com.practice.advanced_compose_ui_practice.drag_drop_list.utils

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState

// 여기서 absolute란, Drag의 대상이 되는 / 선택된 아이템의 인덱스 번호를 가리킨다.
fun LazyListState.getVisibleItemInfoFor(absolute: Int): LazyListItemInfo? {
    return this.layoutInfo.visibleItemsInfo.getOrNull(absolute - this.layoutInfo.visibleItemsInfo.first().index)
}

// 선택된 LazyList의 아이템에 대해 하단 끝 부분의 offset을 픽셀 단위로 얻어오기 위함
val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size // offset은 상단의 offset 픽셀을 가리키고, size는 해당 아이템의 길이(높이)의 픽셀을 가리킨다.

fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return

    // removeAt : 리스트에서 n번 인덱스의 요소를 제거하고(drag), 그 요소를 "반환"
    val element = this.removeAt(from) ?: return

    // 임시로 element에 받아놓은 것을 목표로 하는 인덱스(drop한 곳)으로 위치 변경
    this.add(to, element)
}