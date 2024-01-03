package com.practice.advanced_compose_ui_practice.shorts.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

class PagerState(
    currentPage: Int = 0,
    minPage: Int = 0,
    maxPage: Int = 0
) {
    private var _minPage by mutableStateOf(minPage)
    // structuralEqualityPolicy() => 주소값 비교가 아닌 값 비교를 통해 변경 여부를 결정
    /*
    * A policy to treat values of a MutableState as equivalent
    * if they are structurally (==) equal.
    * Setting MutableState.value to its current structurally (==) equal value is not considered a change. When applying a MutableSnapshot, if the snapshot changes the value to the equivalent value the parent snapshot has is not considered a conflict.
    *
    * */
    private var _maxPage by mutableStateOf(value = maxPage, policy = structuralEqualityPolicy())
    private var _currentPage by mutableStateOf(value = currentPage.coerceIn(minPage, maxPage))

    var minPage: Int
        get() = _minPage
        // PagerState.minPage = 정수(=> 이것이 넘어오는 value 값임) 형식으로 사용했을 때,
        // _minPage의 값이 _maxPage 이하로 설정되도록 보장하도록 설정한다.
        // _currentPage의 값이 _minPage와 _maxPage 사이에 있도록 보장되도록 설정한다.
        set(value) {
            _minPage = value.coerceAtMost(_maxPage)
            _currentPage = _currentPage.coerceIn(_minPage, _maxPage)
        }

    var maxPage: Int
        get() = _maxPage
        set(value) {
            _maxPage = value.coerceAtLeast(_minPage)
            _currentPage = _currentPage.coerceIn(_minPage, _maxPage)
        }

    var currentPage: Int
        get() = _currentPage
        set(value) {
            _currentPage = value.coerceIn(minPage, maxPage)
        }

    enum class SelectionState {
        Selected, // 선택이 완료된 이후는 이것으로 설정
        Undecided // 선택되고 있는 와중에는 이것으로 설정
    }

    var selectionState by mutableStateOf(SelectionState.Selected)

    suspend inline fun <R> selectionPage(block: PagerState.() -> R): R = try {
        // block: PagerState.() -> R : 여기서 넘기는 함수는 PagerState 클래스의 확장함수로서 정의된다.
        // 즉, 클래스명.() -> 리턴타입 형태는 그 클래스의 확장함수화 시킴을 의미한다.
        // PagerState에서 this.block() 형태로 사용될 수 있다.

        // 페이지를 선택. -> 선택하는 와중에는 결정되지 않은 상태로 시작
        selectionState = SelectionState.Undecided
        block()
    } finally {
        selectPage()
    }

    suspend fun selectPage() {
        currentPage -= currentPageOffset.roundToInt()
        // 반올림의 원리 : 절반 이상 위/아래로 움직였을 때만
        // 이동한 오프셋을 반올림한 수치(음수 -1/양수 +1)만큼 페이지를 이동하도록 한다.

        snapToOffset(0f) // 페이지가 이동되었으므로 다시 0f로 초기화.

        selectionState = SelectionState.Selected // 선택 완료
    }

    // Animatable : import androidx.compose.animation."core".Animatable 선택해야 한다.
    private var _currentPageOffset = Animatable(0f).apply {
        // 현재 페이지가 위쪽 끝 / 아래쪽 끝까지 이동할 수 있도록 범위를 설정한다.
        updateBounds(-1f, 1f) // -1은 맨 아래를 의미하고, 1은 맨 위를 의미
    }

    val currentPageOffset: Float
        get() = _currentPageOffset.value // .value로 값을 읽는다.(소수)

    suspend fun snapToOffset(offset: Float) {
        val max = if (currentPage == minPage) 0f else 1f
        val min = if (currentPage == maxPage) 0f else -1f
        _currentPageOffset.snapTo(offset.coerceIn(min, max))
    }

    // 실제로 유저가 위/아래로 페이지를 넘기는 작업을 구현
    suspend fun fling(velocity: Float) {
        // 현재 페이지가 최대 페이지인데 아래로 넘기려고 한다면 아무것도 하지 않고 종료
        if (velocity < 0 && currentPage == maxPage) return
        // 현재 페이지가 최소 페이지인데 위로 넘기려고 한다면 아무것도 하지 않고 종료
        if (velocity > 0 && currentPage == minPage) return

        _currentPageOffset.animateTo(currentPageOffset.roundToInt().toFloat())
        selectPage()
    }

    override fun toString(): String = "PagerState{minPage=$minPage, maxPage=$maxPage, " +
            "currentPage=$currentPage, currentPageOffset=$currentPageOffset}"
}

@Immutable
private data class PageData(val page: Int): ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any? = this@PageData
}

private val Measurable.page: Int
    get() = (parentData as? PageData)?.page ?: error("no page data for measurable $this")
