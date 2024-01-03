package com.practice.advanced_compose_ui_practice.shorts.utils

import androidx.compose.runtime.Immutable
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density

@Immutable
data class PageData(val page: Int): ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any? = this@PageData
}