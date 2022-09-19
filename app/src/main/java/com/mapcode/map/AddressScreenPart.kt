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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapcode.R
import com.mapcode.theme.MapcodeColor
import com.mapcode.theme.MapcodeTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddressArea(
    modifier: Modifier = Modifier,
    address: String,
    matchingAddresses: List<String>,
    onChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    helper: AddressHelper,
    error: AddressError,
    onAddFavouriteClick: () -> Unit = {},
    onDeleteFavouriteClick: () -> Unit = {},
    isFavouriteLocation: Boolean
) {
    Column(modifier) {

        var menuExpanded by remember { mutableStateOf(false) }
        val focusRequester = FocusRequester()
        val focusManager = LocalFocusManager.current

        Row {
            ExposedDropdownMenuBox(
                modifier = Modifier.weight(1f),
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = it }) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    value = address,
                    onValueChange = {
                        onChange(it)
                        if (!menuExpanded) {
                            menuExpanded = true
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(onSearch = {
                        menuExpanded = false
                        focusManager.clearFocus()
                        onSubmit()
                    }),
                    label = { Text(stringResource(R.string.address_bar_label), maxLines = 1) },
                    placeholder = { Text(address, maxLines = 1) },
                    trailingIcon = {
                        if (address.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    focusRequester.requestFocus()
                                    onChange("")
                                }) {
                                Icon(
                                    Icons.Outlined.Clear,
                                    contentDescription = stringResource(R.string.clear_address_content_description)
                                )
                            }
                        }
                    }
                )

                if (matchingAddresses.isNotEmpty()) {
                    ExposedDropdownMenu(
                        modifier = Modifier.testTag("address_dropdown"),
                        expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        matchingAddresses.forEach { address ->
                            DropdownMenuItem(onClick = {
                                menuExpanded = false
                                focusManager.clearFocus()
                                onChange(address)
                                onSubmit()
                            }) {
                                Text(address)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            FavouriteButton(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(top = 8.dp),
                isFavouriteLocation = isFavouriteLocation,
                addFavourite = onAddFavouriteClick,
                deleteFavourite = onDeleteFavouriteClick
            )
        }

        val extraTextHeight = 20.dp

        //to prevent the layout jumping up and down fill with empty space if no helper or error
        if (helper == AddressHelper.None && error == AddressError.None) {
            Spacer(Modifier.height(extraTextHeight))
        } else {
            AddressHelper(
                Modifier
                    .height(extraTextHeight)
                    .padding(start = 4.dp), helper = helper
            )
            AddressError(
                Modifier
                    .height(extraTextHeight)
                    .padding(start = 4.dp), error = error
            )
        }
    }
}

@Preview
@Composable
private fun AddressAreaPreview() {
    MapcodeTheme {
        Surface {
            AddressArea(
                address = "10 Street, City, Country",
                helper = AddressHelper.None,
                error = AddressError.None,
                matchingAddresses = listOf("Address 1", "VERY VERY VERY VERY VERY LONG ADDRESS"),
                isFavouriteLocation = true
            )
        }
    }
}

/**
 * Create the correct components for the helper message state.
 */
@Composable
private fun AddressHelper(modifier: Modifier = Modifier, helper: AddressHelper) {
    val helperMessage = when (helper) {
        AddressHelper.NoInternet -> stringResource(R.string.no_internet_error)
        AddressHelper.NoAddress -> stringResource(R.string.no_address_error)
        is AddressHelper.Location -> helper.location
        AddressHelper.None -> null
    }

    if (helperMessage != null) {
        HelperText(modifier, message = helperMessage)
    }
}

/**
 * Create the correct components for the error message state.
 */
@Composable
private fun AddressError(modifier: Modifier = Modifier, error: AddressError) {
    val errorMessage = when (error) {
        is AddressError.UnknownAddress -> stringResource(
            R.string.cant_find_address_error,
            error.addressQuery
        )
        AddressError.None -> null
    }

    if (errorMessage != null) {
        ErrorText(modifier, message = errorMessage)
    }
}

/**
 * This creates a Text styled as an error.
 */
@Composable
private fun ErrorText(modifier: Modifier = Modifier, message: String) {
    Text(
        modifier = modifier,
        text = message,
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.body1,
        fontWeight = FontWeight.Bold
    )
}

/**
 * This creates a Text styled for the helper messages.
 */
@Composable
private fun HelperText(modifier: Modifier = Modifier, message: String) {
    Text(
        modifier = modifier,
        text = message,
        style = MaterialTheme.typography.body1,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun FavouriteButton(
    modifier: Modifier = Modifier,
    isFavouriteLocation: Boolean,
    deleteFavourite: () -> Unit,
    addFavourite: () -> Unit
) {
    IconButton(
        modifier = modifier,
        onClick = {
            if (isFavouriteLocation) {
                deleteFavourite()
            } else {
                addFavourite()
            }
        }
    ) {
        if (isFavouriteLocation) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Outlined.Bookmark,
                contentDescription = stringResource(R.string.delete_favourite_button_content_description),
                tint = MapcodeColor.addFavouritesButton()
            )
        } else {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Outlined.BookmarkAdd,
                contentDescription = stringResource(R.string.add_favourite_button_content_description),
                tint = MapcodeColor.addFavouritesButton()
            )
        }
    }

}