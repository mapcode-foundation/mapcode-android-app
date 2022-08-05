package com.mapcode.map

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
        onLatitudeChange = viewModel::queryLatitude,
        onLongitudeChange = viewModel::queryLongitude,
        isVerticalLayout = isVerticalLayout
    )
}

@Composable
private fun InfoArea(
    modifier: Modifier = Modifier,
    state: UiState,
    onAddressChange: (String) -> Unit = {},
    onLatitudeChange: (String) -> Unit = {},
    onLongitudeChange: (String) -> Unit = {},
    onTerritoryClick: () -> Unit = {},
    onMapcodeClick: () -> Unit = {},
    isVerticalLayout: Boolean
) {
    if (isVerticalLayout) {
        VerticalInfoArea(
            modifier,
            state,
            onAddressChange,
            onLatitudeChange,
            onLongitudeChange,
            onTerritoryClick,
            onMapcodeClick
        )
    } else {
        HorizontalInfoArea(
            modifier,
            state,
            onAddressChange,
            onLatitudeChange,
            onLongitudeChange,
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
            "1.0",
            "2.0"
        )
        InfoArea(modifier = Modifier.padding(8.dp), state = state, isVerticalLayout = false)
    }
}

@Composable
private fun VerticalInfoArea(
    modifier: Modifier = Modifier,
    state: UiState,
    onAddressChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onTerritoryClick: () -> Unit,
    onMapcodeClick: () -> Unit
) {
    Column(modifier) {
        AddressArea(Modifier, state.addressUi.address, onAddressChange, state.addressUi.helper, state.addressUi.error)
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
        LatitudeTextBox(Modifier.fillMaxWidth(), state.latitude, onLatitudeChange)
        Spacer(Modifier.height(8.dp))
        LongitudeTextBox(Modifier.fillMaxWidth(), state.longitude, onLongitudeChange)
    }
}

@Composable
private fun HorizontalInfoArea(
    modifier: Modifier = Modifier,
    state: UiState,
    onAddressChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
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
                Modifier
                    .weight(0.5f)
                    .padding(end = 8.dp)
                    .fillMaxWidth(), state.latitude, onLatitudeChange
            )
            LongitudeTextBox(
                Modifier
                    .weight(0.5f)
                    .padding(start = 8.dp), state.longitude, onLongitudeChange
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
    latitude: String,
    onChange: (String) -> Unit
) {
    ClearableTextField(
        modifier = modifier,
        text = latitude,
        onChange = onChange,
        label = stringResource(R.string.latitude_text_field_label),
        clearButtonContentDescription = stringResource(R.string.clear_latitude_content_description),
        keyboardType = KeyboardType.Decimal
    )
}

/**
 * The box that shows the longitude.
 */
@Composable
private fun LongitudeTextBox(
    modifier: Modifier = Modifier,
    longitude: String,
    onChange: (String) -> Unit
) {
    ClearableTextField(
        modifier = modifier,
        text = longitude,
        onChange = onChange,
        label = stringResource(R.string.longitude_text_field_label),
        clearButtonContentDescription = stringResource(R.string.clear_longitude_content_description),
        keyboardType = KeyboardType.Decimal
    )
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