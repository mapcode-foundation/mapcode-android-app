package com.mapcode.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapcode.R
import com.mapcode.theme.MapcodeTheme
import kotlinx.coroutines.launch

@Composable
fun VerticalInfoArea(
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
fun HorizontalInfoArea(
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

@Composable
fun InfoArea(
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

@Preview(showBackground = true, widthDp = 400, heightDp = 300)
@Composable
fun InfoAreaPreview() {
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