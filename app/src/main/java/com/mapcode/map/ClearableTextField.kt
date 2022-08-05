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