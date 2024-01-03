package com.practice.advanced_compose_ui_practice.shorts.utils

class PagerScope(
    private val state: PagerState,
    val page: Int
) {
    val currentPage: Int
        get() = state.currentPage

    val currentPageOffset: Float
        get() = state.currentPageOffset

    val selectionState: PagerState.SelectionState
        get() = state.selectionState
}