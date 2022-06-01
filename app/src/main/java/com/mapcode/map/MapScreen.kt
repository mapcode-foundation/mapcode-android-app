package com.mapcode.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
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

/**
 * The part of the screen that shows the mapcode and territory.
 */
@Composable
fun MapcodeTextArea(
    modifier: Modifier = Modifier,
    mapcode: String,
    territory: String
) {
    Column(modifier.fillMaxSize()) {
        ClickableText(
            text = AnnotatedString(stringResource(R.string.mapcode_header_button)),
            onClick = { /*TODO*/ },
            style = MaterialTheme.typography.h6,
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = territory,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.align(Alignment.Bottom)
            )
            Text(
                text = mapcode,
                style = MaterialTheme.typography.subtitle1,
                modifier = modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * The bottom portion of the screen containing all the mapcode and
 * address information.
 */
@Composable
fun MapcodeInfoBox(modifier: Modifier = Modifier, state: MapcodeInfoState) {
    Box(modifier = modifier.fillMaxSize()) {
        MapcodeTextArea(mapcode = state.mapcode, territory = state.territory)
    }
}

@Preview(showBackground = true, widthDp = 600, heightDp = 300)
@Composable
fun MapcodeInfoBoxPreview() {
    MapcodeTheme {
        val state = MapcodeInfoState(mapcode = "1AB.XY", territory = "NLD")
        MapcodeInfoBox(state = state)
    }
}

@Composable
fun MapScreen(viewModel: MapViewModel) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Column {
            MapBox(Modifier.weight(0.7f), onCameraMoved = { lat, long -> viewModel.onCameraMoved(lat, long) })

            val mapcodeInfoState by viewModel.mapcodeInfoState.collectAsState()
            MapcodeInfoBox(Modifier.weight(0.3f), mapcodeInfoState)
        }
    }
}