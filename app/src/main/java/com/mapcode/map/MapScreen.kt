package com.mapcode.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.mapcode.R
import com.mapcode.theme.MapcodeTheme
import kotlinx.coroutines.flow.combine
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
    cameraPositionState: CameraPositionState
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
            cameraPositionState = cameraPositionState
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
    cameraPositionState: CameraPositionState
) {
    var uiSettings by remember { mutableStateOf(MapUiSettings()) }
    var properties by remember { mutableStateOf(MapProperties()) }

    uiSettings = uiSettings.copy(myLocationButtonEnabled = isMyLocationEnabled)
    properties = properties.copy(isMyLocationEnabled = isMyLocationEnabled)

    if (!cameraPositionState.isMoving) {
        onCameraFinishedMoving(
            cameraPositionState.position.target.latitude,
            cameraPositionState.position.target.longitude,
            cameraPositionState.position.zoom
        )
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
        contentDescription = stringResource(R.string.google_maps_content_description)
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
fun AddressTextField(
    modifier: Modifier = Modifier,
    address: String,
    onChange: (String) -> Unit,
    helper: AddressHelper,
    error: AddressError
) {
    val focusManager = LocalFocusManager.current
    var isFocussed by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val textFieldValue: String

    if (isFocussed) {
        textFieldValue = query
    } else {
        textFieldValue = address
        query = address
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
        label = { Text(stringResource(R.string.address_bar_label)) },
        onValueChange = { query = it },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(onGo = {
            focusManager.clearFocus()
            onChange(query)
        }),
        placeholder = { Text(address) },
        trailingIcon = {
            if (address.isNotEmpty()) {
                IconButton(
                    onClick = {
                        focusRequester.requestFocus()
                        query = ""
                    }) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = stringResource(R.string.clear_address_content_description)
                    )
                }
            }
        })

    AddressHelper(helper = helper)
    AddressError(error = error)
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
fun Header(text: String, onClick: () -> Unit = {}) {
    ClickableText(
        text = AnnotatedString(text, SpanStyle(color = MaterialTheme.colors.onSurface)),
        onClick = { onClick() },
        style = MaterialTheme.typography.h6,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * The part of the screen that shows the mapcode and territory.
 */
@Composable
fun MapcodeTextArea(
    modifier: Modifier = Modifier,
    code: String,
    territory: String,
    onClick: () -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        Header(stringResource(R.string.mapcode_header_button), onClick)
        Row(Modifier.fillMaxWidth()) {
            ClickableText(
                text = AnnotatedString(territory, SpanStyle(color = MaterialTheme.colors.onSurface)),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.align(Alignment.Bottom),
                onClick = { onClick() }
            )
            ClickableText(
                text = AnnotatedString(code, SpanStyle(color = MaterialTheme.colors.onSurface)),
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onClick() }
            )
        }
    }
}

/**
 * The bottom portion of the screen containing all the mapcode and
 * address information.
 */
@Composable
fun MapcodeInfoBox(
    modifier: Modifier = Modifier,
    state: MapcodeInfoState,
    onMapcodeClick: () -> Unit = {},
    onAddressChange: (String) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        AddressTextField(
            modifier = Modifier.fillMaxWidth(),
            address = state.address,
            onChange = onAddressChange,
            helper = state.addressHelper,
            error = state.addressError
        )
        MapcodeTextArea(
            modifier = Modifier.padding(top = 8.dp),
            code = state.code,
            territory = state.territory,
            onClick = onMapcodeClick
        )
    }
}

@Composable
fun MapcodeInfoBox(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    onCopiedMapcode: () -> Unit = {}
) {
    val mapcodeInfoState by viewModel.mapcodeInfoState.collectAsState()

    MapcodeInfoBox(
        modifier,
        mapcodeInfoState,
        onMapcodeClick = {
            val copied = viewModel.copyMapcode()
            if (copied) {
                onCopiedMapcode()
            }
        },
        onAddressChange = { viewModel.queryAddress(it) }
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 300)
@Composable
fun MapcodeInfoBoxPreview() {
    MapcodeTheme {
        val state =
            MapcodeInfoState(
                code = "1AB.XY",
                territory = "NLD",
                address = "I am a very very very very very very extremely long address",
                AddressHelper.NoInternet,
                AddressError.UnknownAddress("Street, City"),
                "1.0",
                "2.0"
            )
        MapcodeInfoBox(state = state)
    }
}

@Composable
fun MapScreen(viewModel: MapViewModel) {
    val scaffoldState = rememberScaffoldState()
    val cameraPosition by combine(viewModel.location, viewModel.zoom) { location, zoom ->
        val latLng = LatLng(location.latitude, location.longitude)

        CameraPositionState(CameraPosition.fromLatLngZoom(latLng, zoom))
    }.collectAsState(initial = CameraPositionState())
    val scope = rememberCoroutineScope()
    val copiedMessageStr = stringResource(R.string.copied_to_clipboard_snackbar_text)

    Scaffold(scaffoldState = scaffoldState) {
        Column {
            MapBox(
                Modifier.weight(0.7f),
                onCameraMoved = { lat, long, zoom -> viewModel.onCameraMoved(lat, long, zoom) },
                cameraPositionState = cameraPosition
            )

            MapcodeInfoBox(
                modifier = Modifier
                    .weight(0.3f)
                    .padding(8.dp),
                viewModel = viewModel,
                onCopiedMapcode = {
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar(copiedMessageStr)
                    }
                }
            )
        }
    }
}
