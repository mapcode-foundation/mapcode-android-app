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

package com.mapcode.map

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * A text field that has a clear button and handles refilling the text if it is cleared.
 */
@Composable
fun ClearableTextField(
    modifier: Modifier = Modifier,
    text: String,
    label: String,
    clearButtonContentDescription: String,
    /**
     * When the user presses the "go" keyboard action and submits the text.
     */
    onSubmit: (String) -> Unit,
    /**
     * When the user is typing and the text changes.
     */
    onChange: (String) -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val focusManager = LocalFocusManager.current
    var isFocussed by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val textFieldValue: String

    if (isFocussed) {
        textFieldValue = query
    } else {
        textFieldValue = text
        query = text
    }

    OutlinedTextField(
        modifier = modifier
            .onFocusChanged {
                isFocussed = it.isFocused
            }
            .focusRequester(focusRequester),
        value = textFieldValue,
        singleLine = true,
        label = { Text(label, maxLines = 1) },
        onValueChange = {
            query = it
            onChange(it)
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions(onGo = {
            focusManager.clearFocus()
            onSubmit(query)
        }),
        placeholder = { Text(text, maxLines = 1) },
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        focusRequester.requestFocus()
                        query = ""
                    }) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = clearButtonContentDescription
                    )
                }
            }
        })
}