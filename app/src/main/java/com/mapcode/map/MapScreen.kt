package com.mapcode.map

import android.Manifest
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.mapcode.R
import com.mapcode.theme.MapcodeTheme
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 31/05/2022.
 */

/**
 * The top portion of the screen containing the map and
 * the button to request location.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapBox(
    modifier: Modifier = Modifier,
    onCameraMoved: (Double, Double, Float) -> Unit,
    cameraPositionState: CameraPositionState,
    onMapLoaded: () -> Unit
) {
    Box(modifier) {
        val locationPermissionsState = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        val isMyLocationEnabled by remember {
            derivedStateOf {
                val coarseLocationPermission = locationPermissionsState.permissions
                    .single { it.permission == Manifest.permission.ACCESS_COARSE_LOCATION }

                coarseLocationPermission.status == PermissionStatus.Granted
            }
        }

        Map(
            isMyLocationEnabled = isMyLocationEnabled,
            onCameraFinishedMoving = onCameraMoved,
            cameraPositionState = cameraPositionState,
            onMapLoaded = onMapLoaded
        )
    
        Icon(
            modifier = Modifier
                .align(Alignment.Center),
            painter = painterResource(R.drawable.crosshairs),
            contentDescription = ""
        )

        //overlay the button to request location permission if my location is disabled.
        if (!isMyLocationEnabled) {
            RequestLocationPermissionButton(
                onClick = { locationPermissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

private const val ANIMATE_CAMERA_UPDATE_DURATION_MS = 500

/**
 * This handles the Google Map component.
 */
@Composable
fun Map(
    modifier: Modifier = Modifier,
    isMyLocationEnabled: Boolean,
    onCameraFinishedMoving: (Double, Double, Float) -> Unit,
    cameraPositionState: CameraPositionState,
    onMapLoaded: () -> Unit
) {
    var uiSettings by remember { mutableStateOf(MapUiSettings()) }
    var properties by remember { mutableStateOf(MapProperties()) }

    uiSettings = uiSettings.copy(myLocationButtonEnabled = isMyLocationEnabled)
    properties = properties.copy(isMyLocationEnabled = isMyLocationEnabled)

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            onCameraFinishedMoving(
                cameraPositionState.position.target.latitude,
                cameraPositionState.position.target.longitude,
                cameraPositionState.position.zoom
            )
        }
    }

    val scope = rememberCoroutineScope()

    GoogleMap(
        cameraPositionState = cameraPositionState,
        modifier = modifier.fillMaxSize(),
        uiSettings = uiSettings,
        properties = properties,
        onMapClick = { position ->
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(position),
                    ANIMATE_CAMERA_UPDATE_DURATION_MS
                )
            }
        },
        contentDescription = stringResource(R.string.google_maps_content_description),
        onMapLoaded = onMapLoaded
    )
}

/**
 * This is the button to request location permission.
 */
@Composable
fun RequestLocationPermissionButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Button(onClick = onClick, modifier) {
        Text("Request location permission")
    }
}

@Composable
fun AddressArea(
    modifier: Modifier = Modifier,
    address: String,
    onChange: (String) -> Unit,
    helper: AddressHelper,
    error: AddressError
) {
    Column(modifier) {
        ClearableTextField(
            text = address,
            onChange = onChange,
            label = stringResource(R.string.address_bar_label),
            clearButtonContentDescription = stringResource(R.string.clear_address_content_description)
        )
        AddressHelper(helper = helper)
        AddressError(error = error)
    }
}

/**
 * A text field that has a clear button and handles refilling the text if it is cleared.
 */
@Composable
fun ClearableTextField(
    modifier: Modifier = Modifier,
    text: String,
    label: String,
    clearButtonContentDescription: String,
    onChange: (String) -> Unit,
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
            .fillMaxWidth()
            .onFocusChanged {
                isFocussed = it.isFocused
            }
            .focusRequester(focusRequester),
        value = textFieldValue,
        singleLine = true,
        label = { Text(label, maxLines = 1) },
        onValueChange = { query = it },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions(onGo = {
            focusManager.clearFocus()
            onChange(query)
        }),
        placeholder = { Text(text) },
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

/**
 * Create the correct components for the helper message state.
 */
@Composable
fun AddressHelper(helper: AddressHelper) {
    val helperMessage = when (helper) {
        AddressHelper.NoInternet -> stringResource(R.string.no_internet_error)
        AddressHelper.NoAddress -> stringResource(R.string.no_address_error)
        is AddressHelper.Location -> helper.location
        AddressHelper.None -> null
    }

    if (helperMessage != null) {
        HelperText(message = helperMessage)
    }
}

/**
 * Create the correct components for the error message state.
 */
@Composable
fun AddressError(error: AddressError) {
    val errorMessage = when (error) {
        is AddressError.UnknownAddress -> stringResource(R.string.cant_find_address_error, error.addressQuery)
        AddressError.None -> null
    }

    if (errorMessage != null) {
        ErrorText(message = errorMessage)
    }
}

/**
 * This creates a Text styled as an error.
 */
@Composable
fun ErrorText(modifier: Modifier = Modifier, message: String) {
    Text(modifier = modifier, text = message, color = MaterialTheme.colors.error)
}

/**
 * This creates a Text styled for the helper messages.
 */
@Composable
fun HelperText(modifier: Modifier = Modifier, message: String) {
    Text(modifier = modifier, text = message)
}

@Composable
fun HeaderWithIcon(modifier: Modifier = Modifier, text: String, @DrawableRes icon: Int) {
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
fun MapcodeBox(
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
            Row {
                Text(
                    text = territory,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.align(Alignment.Bottom)
                )
                Text(
                    text = code,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * The box that shows the territory.
 */
@Composable
fun TerritoryBox(
    modifier: Modifier = Modifier,
    index: Int,
    count: Int,
    territoryName: String
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(8.dp)) {
            val headerText = stringResource(R.string.territory_header_button, index, count)
            HeaderWithIcon(modifier = Modifier.fillMaxWidth(), headerText, R.drawable.ic_outline_fast_forward_24)

            Text(
                text = territoryName,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * The box that shows the latitude.
 */
@Composable
fun LatitudeTextBox(
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
fun LongitudeTextBox(
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

/**
 * The bottom portion of the screen containing all the mapcode and
 * address information.
 */
@Composable
fun InfoArea(
    modifier: Modifier = Modifier,
    state: UiState,
    onMapcodeClick: () -> Unit = {},
    onAddressChange: (String) -> Unit = {},
    onTerritoryClick: () -> Unit = {},
    onLatitudeChange: (String) -> Unit = {},
    onLongitudeChange: (String) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        AddressArea(
            modifier = Modifier.fillMaxWidth(),
            address = state.addressUi.address,
            onChange = onAddressChange,
            helper = state.addressUi.helper,
            error = state.addressUi.error
        )
        Row(Modifier.padding(top = 8.dp)) {
            MapcodeBox(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(end = 8.dp)
                    .clickable { onMapcodeClick() },
                code = state.code,
                territory = state.territoryUi.shortName
            )

            TerritoryBox(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(start = 8.dp)
                    .clickable { onTerritoryClick() },
                index = state.territoryUi.number,
                count = state.territoryUi.count,
                territoryName = state.territoryUi.fullName
            )
        }

        Row(Modifier.padding(top = 8.dp)) {
            LatitudeTextBox(
                Modifier
                    .weight(0.5f)
                    .padding(end = 8.dp),
                latitude = state.latitude,
                onChange = onLatitudeChange
            )

            LongitudeTextBox(
                Modifier
                    .weight(0.5f)
                    .padding(start = 8.dp),
                longitude = state.longitude,
                onChange = onLongitudeChange
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 300)
@Composable
fun MapcodeInfoBoxPreview() {
    MapcodeTheme {
        val state = UiState(
            code = "1AB.XY",
            territoryUi = TerritoryUi("NLD", "Netherlands", 1, 1),
            addressUi = AddressUi(
                "I am a very very very very very very extremely long address",
                AddressError.UnknownAddress("Street, City"),
                AddressHelper.NoInternet,
            ),
            "1.0",
            "2.0"
        )
        InfoArea(modifier = Modifier.padding(8.dp), state = state)
    }
}

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    /**
     * This option makes instrumentation tests much quicker and easier to implement.
     */
    showMap: Boolean = true
) {
    val scaffoldState = rememberScaffoldState()
    val uiState by viewModel.uiState.collectAsState()

    val scope = rememberCoroutineScope()
    val copiedMessageStr = stringResource(R.string.copied_to_clipboard_snackbar_text)

    Scaffold(scaffoldState = scaffoldState) {
        Column {
            if (showMap) {
                MapBox(
                    Modifier.weight(0.65f),
                    onCameraMoved = { lat, long, zoom -> viewModel.onCameraMoved(lat, long, zoom) },
                    cameraPositionState = viewModel.cameraPositionState,
                    onMapLoaded = {
                        viewModel.isGoogleMapsSdkLoaded = true
                        viewModel.restoreLastLocation()
                    }
                )
            }

            InfoArea(
                Modifier
                    .weight(0.35f)
                    .padding(8.dp),
                uiState,
                onMapcodeClick = {
                    val copied = viewModel.copyMapcode()
                    if (copied) {
                        scope.launch {
                            //dismiss current snack bar so they aren't queued up
                            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                            scaffoldState.snackbarHostState.showSnackbar(copiedMessageStr)
                        }
                    }
                },
                onAddressChange = { viewModel.queryAddress(it) },
                onTerritoryClick = { viewModel.onTerritoryClick() },
                onLatitudeChange = { viewModel.queryLatitude(it) },
                onLongitudeChange = { viewModel.queryLongitude(it) }
            )
        }
    }
}
