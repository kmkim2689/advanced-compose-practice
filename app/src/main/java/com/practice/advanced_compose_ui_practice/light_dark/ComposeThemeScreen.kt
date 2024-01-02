package com.practice.advanced_compose_ui_practice.light_dark

import android.graphics.drawable.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.practice.advanced_compose_ui_practice.R
import com.practice.advanced_compose_ui_practice.light_dark.utils.AppTheme
import com.practice.advanced_compose_ui_practice.light_dark.utils.ThemeSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeThemeScreen(
    modifier: Modifier = Modifier,
    onItemSelected: (AppTheme) -> Unit
) {
    var menuExpanded by remember {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Compose Theme")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            menuExpanded = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null
                        )
                    }

                    // selection menu(dropdown) : implement on the "actions" area
                    Column(
                        modifier = Modifier.wrapContentSize(Alignment.TopStart)
                    ) {
                        DropdownMenu(
                            modifier = Modifier
                                .width(200.dp)
                                .wrapContentSize(Alignment.TopStart),
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(text = "Auto")
                                },
                                onClick = {
                                    menuExpanded = false
                                    onItemSelected(AppTheme.fromOrdinal(AppTheme.MODE_AUTO.order)!!)
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(text = "Day")
                                },
                                onClick = {
                                    menuExpanded = false
                                    onItemSelected(AppTheme.fromOrdinal(AppTheme.MODE_DAY.order)!!)
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(text = "Night")
                                },
                                onClick = {
                                    menuExpanded = false
                                    onItemSelected(AppTheme.fromOrdinal(AppTheme.MODE_NIGHT.order)!!)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.bodyMedium, fontSize = 18.sp)
        }
    }
}