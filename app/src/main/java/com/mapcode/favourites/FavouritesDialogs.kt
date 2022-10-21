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

package com.mapcode.favourites

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapcode.R
import com.mapcode.util.CustomDialog
import com.mapcode.util.ErrorText

@Composable
fun CreateFavouriteDialog(
    name: String,
    mapcode: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmitClick: () -> Unit
) {
    FavouriteNameDialog(
        title = stringResource(R.string.add_favourite_dialog_title),
        name = name,
        onNameChange = onNameChange,
        onDismiss = onDismiss,
        onSubmitClick = onSubmitClick
    ) {
        Text(
            stringResource(R.string.add_favourite_dialog_message),
            style = MaterialTheme.typography.body2
        )
        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.add_favourite_dialog_mapcode, mapcode),
            style = MaterialTheme.typography.body2
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun RenameFavouriteDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmitClick: () -> Unit
) {
    FavouriteNameDialog(
        title = stringResource(R.string.rename_favourite_dialog_title),
        name = name,
        onNameChange = onNameChange,
        onDismiss = onDismiss,
        onSubmitClick = onSubmitClick
    ) {}
}

@Composable
private fun FavouriteNameDialog(
    title: String,
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmitClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val error: String? = when {
        name.isEmpty() -> stringResource(R.string.error_favourite_name_empty)
        else -> null
    }

    CustomDialog(
        title = title,
        confirmButton = {
            TextButton(onClick = onSubmitClick, enabled = error == null) {
                Text(stringResource(R.string.add_favourite_dialog_submit_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.add_favourite_dialog_cancel_button))
            }
        },
        onDismissRequest = onDismiss,
    ) {
        Column {
            content()
            FavouriteNameDialogContent(name = name, onNameChange = onNameChange, error = error)
        }
    }
}

@Composable
private fun FavouriteNameDialogContent(
    name: String,
    onNameChange: (String) -> Unit,
    error: String?
) {
    val focusRequester = remember { FocusRequester() }

    TextField(
        modifier = Modifier.focusRequester(focusRequester),
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.add_favourite_dialog_text_field_label)) },
        trailingIcon = {
            IconButton(onClick = {
                focusRequester.requestFocus()
                onNameChange("")
            }) {
                Icon(
                    Icons.Outlined.Clear,
                    contentDescription = stringResource(R.string.add_favourite_dialog_clear_name_button)
                )
            }
        }
    )

    if (error != null) {
        ErrorText(
            modifier = Modifier
                .height(24.dp)
                .padding(start = 16.dp, top = 4.dp),
            text = error
        )
    } else {
        Spacer(Modifier.height(24.dp))
    }
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun CreateFavouriteDialogPreview() {
    MaterialTheme {
        CreateFavouriteDialog(
            name = "",
            mapcode = "NLD AB.XY",
            onDismiss = { },
            onSubmitClick = {},
            onNameChange = {})
    }
}

@Composable
fun DeleteFavouriteDialog(
    favouriteName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        title = { Text(stringResource(R.string.delete_favourite_dialog_title)) },
        text = { Text(stringResource(R.string.delete_favourite_dialog_text, favouriteName)) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete_favourite_dialog_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_favourite_dialog_dismiss_button))
            }
        })
}