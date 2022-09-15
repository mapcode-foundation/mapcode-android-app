/*
 * Copyright (C) 2022, Stichting Mapcode Foundation (http://www.mapcode.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mapcode.util

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomDialog(
    title: String,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable () -> Unit,
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        //must be set false so that the dialog resizes when content changes
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.padding(50.dp),
            color = MaterialTheme.colors.surface, shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    modifier = Modifier.wrapContentSize(),
                    text = title,
                    style = MaterialTheme.typography.h6
                )
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .padding(top = 16.dp)
                ) {
                    content()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.End),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun ScrollableDialog(
    onDismiss: () -> Unit,
    title: String,
    buttonText: String,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = MaterialTheme.colors.surface, shape = MaterialTheme.shapes.medium) {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .height(64.dp)
                        .wrapContentSize()
                        .padding(start = 24.dp, end = 24.dp),
                    text = title,
                    style = MaterialTheme.typography.h6
                )
                Divider(Modifier.height(1.dp))
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    content()
                }
                Divider(Modifier.height(1.dp))
                TextButton(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(8.dp),
                    onClick = onDismiss
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}