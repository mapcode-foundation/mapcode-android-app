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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapcode.R
import com.mapcode.theme.MapcodeTheme
import kotlinx.coroutines.launch

@Composable
fun InfoArea(
    modifier: Modifier,
    viewModel: MapViewModel,
    scaffoldState: ScaffoldState,
    isVerticalLayout: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val copiedMessageStr = stringResource(R.string.copied_to_clipboard_snackbar_text)
    val onMapcodeClick = remember {
        {
            val copied = viewModel.copyMapcode()
            if (copied) {
                scope.launch {
                    //dismiss current snack bar so they aren't queued up
                    scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                    scaffoldState.snackbarHostState.showSnackbar(copiedMessageStr)
                }
            }
        }
    }

    InfoArea(
        modifier,
        uiState,
        onMapcodeClick = onMapcodeClick,
        onAddressChange = viewModel::queryAddress,
        onTerritoryClick = viewModel::onTerritoryClick,
        onChangeLatitude = viewModel::onLatitudeTextChanged,
        onSubmitLatitude = viewModel::onSubmitLatitude,
        onChangeLongitude = viewModel::onLongitudeTextChanged,
        onSubmitLongitude = viewModel::onSubmitLongitude,
        isVerticalLayout = isVerticalLayout
    )
}

@Composable
private fun InfoArea(
    modifier: Modifier = Modifier,
    state: UiState,
    onAddressChange: (String) -> Unit = {},
    onChangeLatitude: (String) -> Unit = {},
    onSubmitLatitude: () -> Unit = {},
    onChangeLongitude: (String) -> Unit = {},
    onSubmitLongitude: () -> Unit = {},
    onTerritoryClick: () -> Unit = {},
    onMapcodeClick: () -> Unit = {},
    isVerticalLayout: Boolean
) {
    if (isVerticalLayout) {
        VerticalInfoArea(
            modifier,
            state,
            onAddressChange,
            onChangeLatitude,
            onSubmitLatitude,
            onChangeLongitude,
            onSubmitLongitude,
            onTerritoryClick,
            onMapcodeClick
        )
    } else {
        HorizontalInfoArea(
            modifier,
            state,
            onAddressChange,
            onChangeLatitude,
            onSubmitLatitude,
            onChangeLongitude,
            onSubmitLongitude,
            onTerritoryClick,
            onMapcodeClick
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 300)
@Composable
private fun InfoAreaPreview() {
    MapcodeTheme {
        val state = UiState(
            mapcodeUi = MapcodeUi("AB.XY", "NLD", "Netherlands", 1, 1),
            addressUi = AddressUi(
                "I am a very very very very very very extremely long address",
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
    onChangeLatitude: (String) -> Unit,
    onSubmitLatitude: () -> Unit,
    onChangeLongitude: (String) -> Unit,
    onSubmitLongitude: () -> Unit,
    onTerritoryClick: () -> Unit,
    onMapcodeClick: () -> Unit
) {
    Column(modifier) {
        AddressArea(
            Modifier,
            state.addressUi.address,
            onAddressChange,
            state.addressUi.helper,
            state.addressUi.error
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
        )
        Spacer(Modifier.height(8.dp))
        LongitudeTextBox(
            modifier = Modifier.fillMaxWidth(),
            text = state.locationUi.longitudeText,
            placeHolder = state.locationUi.longitudePlaceholder,
            showInvalidError = state.locationUi.showLongitudeInvalidError,
            onSubmit = onSubmitLongitude,
            onChange = onChangeLongitude,
        )
    }
}

@Composable
private fun HorizontalInfoArea(
    modifier: Modifier = Modifier,
    state: UiState,
    onAddressChange: (String) -> Unit,
    onChangeLatitude: (String) -> Unit,
    onSubmitLatitude: () -> Unit,
    onChangeLongitude: (String) -> Unit,
    onSubmitLongitude: () -> Unit,
    onTerritoryClick: () -> Unit,
    onMapcodeClick: () -> Unit
) {
    Column(modifier) {
        AddressArea(
            Modifier.fillMaxWidth(),
            state.addressUi.address,
            onAddressChange,
            state.addressUi.helper,
            state.addressUi.error
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
                    .padding(end = 8.dp)
                    .imePadding(),
                text = state.locationUi.latitudeText,
                placeHolder = state.locationUi.latitudePlaceholder,
                showInvalidError = state.locationUi.showLatitudeInvalidError,
                onSubmit = onSubmitLatitude,
                onChange = onChangeLatitude,
            )
            LongitudeTextBox(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(start = 8.dp)
                    .imePadding(),
                text = state.locationUi.longitudeText,
                placeHolder = state.locationUi.longitudePlaceholder,
                showInvalidError = state.locationUi.showLongitudeInvalidError,
                onSubmit = onSubmitLongitude,
                onChange = onChangeLongitude,
            )
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
    onChange: (String) -> Unit
) {
    LatLngTextField(
        modifier = modifier,
        text = text,
        placeholder = placeHolder,
        showInvalidError = showInvalidError,
        label = stringResource(R.string.latitude_text_field_label),
        clearButtonContentDescription = stringResource(R.string.clear_latitude_content_description),
        onSubmit = onSubmit,
        onChange = onChange
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
    onChange: (String) -> Unit
) {
    LatLngTextField(
        modifier = modifier,
        text = text,
        placeholder = placeHolder,
        showInvalidError = showInvalidError,
        label = stringResource(R.string.longitude_text_field_label),
        clearButtonContentDescription = stringResource(R.string.clear_longitude_content_description),
        onSubmit = onSubmit,
        onChange = onChange
    )
}

@Composable
private fun LatLngTextField(
    modifier: Modifier = Modifier,
    text: String,
    placeholder: String,
    showInvalidError: Boolean,
    label: String,
    clearButtonContentDescription: String,
    onSubmit: () -> Unit,
    onChange: (String) -> Unit
) {
    Column(modifier) {
        val focusManager = LocalFocusManager.current
        val focusRequester = remember { FocusRequester() }

        OutlinedTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            value = text,
            singleLine = true,
            label = { Text(label, maxLines = 1) },
            onValueChange = onChange,
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
                if (text.isNotEmpty()) {
                    IconButton(onClick = {
                        focusRequester.requestFocus()
                        onChange("")
                    }) {
                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = clearButtonContentDescription
                        )
                    }
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
    territory: String
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
                pushStyle(MaterialTheme.typography.body2.toSpanStyle())
                append(territory)
                pop()
                append(" ")
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