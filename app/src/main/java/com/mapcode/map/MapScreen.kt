package com.mapcode.map

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.mapcode.R
import com.mapcode.theme.MapcodeTheme
import timber.log.Timber

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

    GoogleMap(
        cameraPositionState = cameraPositionState,
        modifier = modifier.fillMaxSize(),
        uiSettings = uiSettings,
        properties = properties,
        onMapClick = {
            Timber.e(it.toString())
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
fun MapcodeTextArea(mapcode: String) {
    Text(text = mapcode, style = MaterialTheme.typography.subtitle1)
}

/**
 * The bottom portion of the screen containing all the mapcode and
 * address information.
 */
@Composable
fun MapcodeInfoBox(modifier: Modifier = Modifier, state: MapcodeInfoState) {
    Box(modifier = modifier.fillMaxSize()) {
        MapcodeTextArea(mapcode = state.mapcode)
    }
}

@Preview(showBackground = true)
@Composable
fun MapcodeInfoPreview() {
    MapcodeTheme {
        val state = MapcodeInfoState(mapcode = "1AB.XY")
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