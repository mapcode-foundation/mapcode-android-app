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

@file:OptIn(ExperimentalFoundationApi::class)

package com.mapcode.map

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapcode.R
import com.mapcode.theme.MapcodeColor
import com.mapcode.theme.MapcodeTheme

@Composable
fun InfoArea(
    modifier: Modifier,
    viewModel: MapViewModel,
    showSnackbar: (String) -> Unit,
    isVerticalLayout: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val copiedMessageStr = stringResource(R.string.copied_to_clipboard_snackbar_text)
    val onMapcodeClick = remember {
        {
            val copied = viewModel.copyMapcode()
            if (copied && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                showSnackbar(copiedMessageStr)
            }
        }
    }
    val onLocationClick = remember {
        {
            viewModel.copyLocation()
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                showSnackbar(copiedMessageStr)
            }
        }
    }

    InfoArea(
        modifier,
        uiState,
        onMapcodeClick = onMapcodeClick,
        onAddressChange = viewModel::onAddressTextChange,
        onSubmitAddress = viewModel::onSubmitAddress,
        onTerritoryClick = viewModel::onTerritoryClick,
        onChangeLatitude = viewModel::onLatitudeTextChanged,
        onSubmitLatitude = viewModel::onSubmitLatitude,
        onCopyLatitude = onLocationClick,
        onChangeLongitude = viewModel::onLongitudeTextChanged,
        onSubmitLongitude = viewModel::onSubmitLongitude,
        onCopyLongitude = onLocationClick,
        isVerticalLayout = isVerticalLayout
    )
}

@Composable
private fun InfoArea(
    modifier: Modifier = Modifier,
    state: UiState,
    onAddressChange: (String) -> Unit = {},
    onSubmitAddress: () -> Unit = {},
    onChangeLatitude: (String) -> Unit = {},
    onSubmitLatitude: () -> Unit = {},
    onCopyLatitude: () -> Unit = {},
    onChangeLongitude: (String) -> Unit = {},
    onSubmitLongitude: () -> Unit = {},
    onCopyLongitude: () -> Unit = {},
    onTerritoryClick: () -> Unit = {},
    onMapcodeClick: () -> Unit = {},
    onAddFavouriteClick: () -> Unit = {},
    onViewFavouritesClick: () -> Unit = {},
    isVerticalLayout: Boolean
) {
    if (isVerticalLayout) {
        VerticalInfoArea(
            modifier,
            state,
            onAddressChange,
            onSubmitAddress,
            onChangeLatitude,
            onSubmitLatitude,
            onCopyLatitude,
            onChangeLongitude,
            onSubmitLongitude,
            onCopyLongitude,
            onTerritoryClick,
            onMapcodeClick,
            onAddFavouriteClick,
            onViewFavouritesClick
        )
    } else {
        HorizontalInfoArea(
            modifier,
            state,
            onAddressChange,
            onSubmitAddress,
            onChangeLatitude,
            onSubmitLatitude,
            onCopyLatitude,
            onChangeLongitude,
            onSubmitLongitude,
            onCopyLongitude,
            onTerritoryClick,
            onMapcodeClick,
            onAddFavouriteClick,
            onViewFavouritesClick
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_3)
@Composable
private fun VerticalInfoAreaPreview() {
    MapcodeTheme {
        val state = UiState(
            mapcodeUi = MapcodeUi("AB.XY", "NLD", "Netherlands", 1, 1),
            addressUi = AddressUi(
                "I am a very very very very very very extremely long address",
                emptyList(),
                AddressError.UnknownAddress("Street, City"),
                AddressHelper.NoInternet,
            ),
            locationUi = LocationUi("1.0", "1.0", true, "1.0", "1.0", true)
        )
        InfoArea(modifier = Modifier.padding(8.dp), state = state, isVerticalLayout = true)
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_3)
@Composable
private fun HorizontalInfoAreaPreview() {
    MapcodeTheme {
        val state = UiState(
            mapcodeUi = MapcodeUi("AB.XY", "NLD", "Netherlands", 1, 1),
            addressUi = AddressUi(
                "I am a very very very very very very extremely long address",
                emptyList(),
                AddressError.UnknownAddress("Street, City"),
                AddressHelper.NoInternet,
            ),
            locationUi = LocationUi("1.0", "1.0", true, "1.0", "1.0", true)
        )
        InfoArea(modifier = Modifier.padding(8.dp), state = state, isVerticalLayout = false)
    }
}

@Composable
private fun VerticalInfoArea(
    modifier: Modifier = Modifier,
    state: UiState,
    onAddressChange: (String) -> Unit,
    onSubmitAddress: () -> Unit,
    onChangeLatitude: (String) -> Unit,
    onSubmitLatitude: () -> Unit,
    onCopyLatitude: () -> Unit,
    onChangeLongitude: (String) -> Unit,
    onSubmitLongitude: () -> Unit,
    onCopyLongitude: () -> Unit,
    onTerritoryClick: () -> Unit,
    onMapcodeClick: () -> Unit,
    onAddFavouriteClick: () -> Unit,
    onViewFavouritesClick: () -> Unit
) {
    Column(modifier) {
        AddressArea(
            modifier = Modifier,
            address = state.addressUi.address,
            matchingAddresses = state.addressUi.matchingAddresses,
            onChange = onAddressChange,
            onSubmit = onSubmitAddress,
            helper = state.addressUi.helper,
            error = state.addressUi.error
        )
        Spacer(Modifier.height(8.dp))
        TerritoryBox(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTerritoryClick() },
            index = state.mapcodeUi.number,
            count = state.mapcodeUi.count,
            territoryName = state.mapcodeUi.territoryFullName
        )
        Spacer(Modifier.height(8.dp))
        MapcodeBox(
            Modifier
                .fillMaxWidth()
                .clickable { onMapcodeClick() },
            state.mapcodeUi.code,
            state.mapcodeUi.territoryShortName
        )
        Spacer(Modifier.height(8.dp))
        LatitudeTextBox(
            modifier = Modifier.fillMaxWidth(),
            text = state.locationUi.latitudeText,
            placeHolder = state.locationUi.latitudePlaceholder,
            showInvalidError = state.locationUi.showLatitudeInvalidError,
            onSubmit = onSubmitLatitude,
            onChange = onChangeLatitude,
            onCopy = onCopyLatitude
        )
        Spacer(Modifier.height(8.dp))
        LongitudeTextBox(
            modifier = Modifier.fillMaxWidth(),
            text = state.locationUi.longitudeText,
            placeHolder = state.locationUi.longitudePlaceholder,
            showInvalidError = state.locationUi.showLongitudeInvalidError,
            onSubmit = onSubmitLongitude,
            onChange = onChangeLongitude,
            onCopy = onCopyLongitude
        )

        FavouritesButtons(
            modifier = Modifier.imePadding(),
            onAddClick = onAddFavouriteClick,
            onViewClick = onViewFavouritesClick
        )
    }
}

@Composable
private fun HorizontalInfoArea(
    modifier: Modifier = Modifier,
    state: UiState,
    onAddressChange: (String) -> Unit,
    onSubmitAddress: () -> Unit,
    onChangeLatitude: (String) -> Unit,
    onSubmitLatitude: () -> Unit,
    onCopyLatitude: () -> Unit,
    onChangeLongitude: (String) -> Unit,
    onSubmitLongitude: () -> Unit,
    onCopyLongitude: () -> Unit,
    onTerritoryClick: () -> Unit,
    onMapcodeClick: () -> Unit,
    onAddFavouriteClick: () -> Unit,
    onViewFavouritesClick: () -> Unit
) {
    Column(modifier) {
        AddressArea(
            modifier = Modifier.fillMaxWidth(),
            address = state.addressUi.address,
            matchingAddresses = state.addressUi.matchingAddresses,
            onChange = onAddressChange,
            onSubmit = onSubmitAddress,
            helper = state.addressUi.helper,
            error = state.addressUi.error
        )
        Row(Modifier.padding(top = 8.dp)) {
            TerritoryBox(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(end = 8.dp)
                    .clickable { onTerritoryClick() },
                index = state.mapcodeUi.number,
                count = state.mapcodeUi.count,
                territoryName = state.mapcodeUi.territoryFullName
            )
            MapcodeBox(
                Modifier
                    .weight(0.5f)
                    .padding(start = 8.dp)
                    .clickable { onMapcodeClick() },
                state.mapcodeUi.code,
                state.mapcodeUi.territoryShortName
            )
        }

        Row(Modifier.padding(top = 8.dp)) {
            LatitudeTextBox(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(end = 8.dp),
                text = state.locationUi.latitudeText,
                placeHolder = state.locationUi.latitudePlaceholder,
                showInvalidError = state.locationUi.showLatitudeInvalidError,
                onSubmit = onSubmitLatitude,
                onChange = onChangeLatitude,
                onCopy = onCopyLatitude
            )
            LongitudeTextBox(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(start = 8.dp),
                text = state.locationUi.longitudeText,
                placeHolder = state.locationUi.longitudePlaceholder,
                showInvalidError = state.locationUi.showLongitudeInvalidError,
                onSubmit = onSubmitLongitude,
                onChange = onChangeLongitude,
                onCopy = onCopyLongitude
            )
        }
        FavouritesButtons(
            modifier = Modifier.imePadding(),
            onAddClick = onAddFavouriteClick,
            onViewClick = onViewFavouritesClick
        )
    }
}

@Composable
private fun FavouritesButtons(
    modifier: Modifier,
    onAddClick: () -> Unit,
    onViewClick: () -> Unit
) {
    Row(modifier = modifier) {
        Button(
            modifier = Modifier.weight(0.5f),
            onClick = onAddClick,
            colors = MapcodeColor.addFavouritesButton()
        ) {
            Text(stringResource(R.string.add_favourite_button))
        }
        Spacer(Modifier.width(16.dp))
        OutlinedButton(
            modifier = Modifier.weight(0.5f), onClick = onViewClick,
            colors = MapcodeColor.viewFavouritesButton()
        ) {
            Text(stringResource(R.string.view_favourites_button))
        }
    }
}


/**
 * The box that shows the territory.
 */
@Composable
private fun TerritoryBox(
    modifier: Modifier = Modifier,
    index: Int,
    count: Int,
    territoryName: String
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(8.dp)) {
            val headerText = stringResource(R.string.territory_header_button, index, count)
            HeaderWithIcon(
                modifier = Modifier.fillMaxWidth(),
                headerText,
                R.drawable.ic_outline_fast_forward_24
            )

            Text(
                text = territoryName,
                style = MaterialTheme.typography.body1,
                modifier = Modifier
            )
        }
    }
}

/**
 * The box that shows the latitude.
 */
@Composable
private fun LatitudeTextBox(
    modifier: Modifier = Modifier,
    text: String,
    placeHolder: String,
    showInvalidError: Boolean,
    onSubmit: () -> Unit,
    onChange: (String) -> Unit,
    onCopy: () -> Unit
) {
    LatLngTextField(
        modifier = modifier,
        text = text,
        placeholder = placeHolder,
        showInvalidError = showInvalidError,
        label = stringResource(R.string.latitude_text_field_label),
        onSubmit = onSubmit,
        onChange = onChange,
        onCopy = onCopy
    )
}

/**
 * The box that shows the longitude.
 */
@Composable
private fun LongitudeTextBox(
    modifier: Modifier = Modifier,
    text: String,
    placeHolder: String,
    showInvalidError: Boolean,
    onSubmit: () -> Unit,
    onChange: (String) -> Unit,
    onCopy: () -> Unit
) {
    LatLngTextField(
        modifier = modifier,
        text = text,
        placeholder = placeHolder,
        showInvalidError = showInvalidError,
        label = stringResource(R.string.longitude_text_field_label),
        onSubmit = onSubmit,
        onChange = onChange,
        onCopy = onCopy
    )
}

@SuppressLint("UnrememberedMutableState")
@Composable
private fun LatLngTextField(
    modifier: Modifier = Modifier,
    text: String,
    placeholder: String,
    showInvalidError: Boolean,
    label: String,
    onSubmit: () -> Unit,
    onChange: (String) -> Unit,
    onCopy: () -> Unit
) {
    Column(modifier) {
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }
        var textSelection: TextRange by remember { mutableStateOf(TextRange.Zero) }

        val textValue: TextFieldValue by derivedStateOf { TextFieldValue(text, textSelection) }

        var selectAllText: Boolean by remember { mutableStateOf(false) }

        if (selectAllText) {
            SideEffect {
                textSelection = TextRange(0, text.length)
                selectAllText = false
            }
        }

        OutlinedTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        selectAllText = true
                    }
                }
                .fillMaxWidth(),
            value = textValue,
            singleLine = true,
            label = {
                Text(
                    text = label,
                    maxLines = 1
                )
            },
            onValueChange = { value ->
                textSelection = value.selection
                onChange(value.text)
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Go,
                keyboardType = KeyboardType.Decimal
            ),
            keyboardActions = KeyboardActions(onGo = {
                if (!showInvalidError) {
                    focusManager.clearFocus()
                    onSubmit()
                }
            }),
            placeholder = { Text(placeholder, maxLines = 1) },
            trailingIcon = {
                IconButton(onClick = onCopy) {
                    Icon(
                        painterResource(R.drawable.ic_outline_content_copy_24),
                        contentDescription = stringResource(R.string.copy_location_content_description)
                    )
                }
            })

        if (showInvalidError) {
            ErrorText(
                modifier = Modifier
                    .height(20.dp)
                    .padding(start = 16.dp, top = 4.dp),
                text = stringResource(R.string.invalid_latlng_error)
            )
        } else {
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HeaderWithIcon(modifier: Modifier = Modifier, text: String, @DrawableRes icon: Int) {
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            modifier = Modifier.fillMaxHeight(),
            text = text,
            style = MaterialTheme.typography.subtitle2
        )

        Icon(
            modifier = Modifier.height(20.dp),
            painter = painterResource(icon),
            contentDescription = ""
        )
    }
}

/**
 * The box that shows the mapcode.
 */
@Composable
private fun MapcodeBox(
    modifier: Modifier = Modifier,
    code: String,
    territory: String?
) {
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.primary
    ) {
        Column(Modifier.padding(8.dp)) {
            HeaderWithIcon(
                Modifier.fillMaxWidth(),
                stringResource(R.string.mapcode_header_button),
                R.drawable.ic_outline_content_copy_24
            )

            val codeSpanStyle: SpanStyle =
                MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold).toSpanStyle()

            val styledString = buildAnnotatedString {
                if (territory != null) {
                    pushStyle(MaterialTheme.typography.body2.toSpanStyle())
                    append(territory)
                    pop()
                    append(" ")
                }
                pushStyle(codeSpanStyle)
                append(code)
                pop()
            }

            Text(text = styledString)
        }
    }
}

@Composable
private fun ErrorText(modifier: Modifier = Modifier, text: String) {
    Text(
        modifier = modifier,
        text = text,
        color = MaterialTheme.colors.error,
        style = MaterialTheme.typography.body2
    )
}