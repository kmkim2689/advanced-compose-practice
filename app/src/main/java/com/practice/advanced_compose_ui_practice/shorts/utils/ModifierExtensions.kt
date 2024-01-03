package com.practice.advanced_compose_ui_practice.shorts.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

inline fun Modifier.clickableWithoutRipple(
    crossinline onClick: () -> Unit
): Modifier = composed {
    this.clickable(
        indication = null,
        interactionSource = remember {
            MutableInteractionSource()
        },
        onClick = {
            onClick()
        }
    )
}