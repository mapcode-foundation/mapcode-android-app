package com.mapcode.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
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
fun MapBox(modifier: Modifier = Modifier, onCameraMoved: (Double, Double) -> Unit) {
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

        Map(isMyLocationEnabled = isMyLocationEnabled, onCameraFinishedMoving = onCameraMoved)

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
    onCameraFinishedMoving: (Double, Double) -> Unit
) {
    var uiSettings by remember { mutableStateOf(MapUiSettings()) }
    var properties by remember { mutableStateOf(MapProperties()) }

    uiSettings = uiSettings.copy(myLocationButtonEnabled = isMyLocationEnabled)
    properties = properties.copy(isMyLocationEnabled = isMyLocationEnabled)

    val cameraPositionState = rememberCameraPositionState()

    if (!cameraPositionState.isMoving) {
        onCameraFinishedMoving(
            cameraPositionState.position.target.latitude,
            cameraPositionState.position.target.longitude
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
    error: AddressError
) {
    var inputtedText by remember { mutableStateOf(address) }
    val focusManager = LocalFocusManager.current

    Column {
        OutlinedTextField(
            modifier = modifier.fillMaxWidth(),
            value = inputtedText,
            singleLine = true,
            label = { Text(stringResource(R.string.address_bar_label)) },
            onValueChange = { inputtedText = it },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    focusManager.clearFocus()
                    onChange(inputtedText)
                }
            )
        )

        val errorMessage = when (error) {
            AddressError.NoInternet -> stringResource(R.string.no_internet_error)
            AddressError.CantFindAddress -> "TODO"
            AddressError.NoAddress -> "TODO"
            AddressError.None -> null
        }

        if (errorMessage != null) {
            TextFieldErrorText(message = errorMessage)
        }
    }
}

@Composable
fun TextFieldErrorText(message: String) {
    Text(message)
}

@Composable
fun Header(text: String, onClick: () -> Unit = {}) {
    ClickableText(
        text = AnnotatedString(text),
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
                text = AnnotatedString(territory),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.align(Alignment.Bottom),
                onClick = { onClick() }
            )
            ClickableText(
                text = AnnotatedString(code),
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
    Box(modifier = modifier.fillMaxSize()) {
        Column {
            AddressTextField(
                modifier = Modifier.fillMaxWidth(),
                address = state.address,
                onChange = onAddressChange,
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
}

@Preview(showBackground = true, widthDp = 600, heightDp = 300)
@Composable
fun MapcodeInfoBoxPreview() {
    MapcodeTheme {
        val state =
            MapcodeInfoState(
                code = "1AB.XY",
                territory = "NLD",
                address = "Street, City",
                AddressError.NoInternet,
                "1.0",
                "2.0"
            )
        MapcodeInfoBox(state = state)
    }
}

@Composable
fun MapScreen(viewModel: MapViewModel) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Scaffold(
            scaffoldState = scaffoldState,
        ) {
            Column {
                MapBox(Modifier.weight(0.7f), onCameraMoved = { lat, long -> viewModel.onCameraMoved(lat, long) })

                val copiedMessageStr = stringResource(R.string.copied_to_clipboard_snackbar_text)
                val mapcodeInfoState by viewModel.mapcodeInfoState.collectAsState()

                MapcodeInfoBox(
                    Modifier
                        .weight(0.3f)
                        .padding(8.dp),
                    mapcodeInfoState,
                    onMapcodeClick = {
                        val copied = viewModel.copyMapcode()
                        if (copied) {
                            scope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(copiedMessageStr)
                            }
                        }
                    },
                    onAddressChange = { viewModel.queryAddress(it) })
            }
        }
    }
}