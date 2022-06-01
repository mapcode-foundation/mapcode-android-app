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
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.mapcode.R
import com.mapcode.theme.MapcodeTheme

/**
 * Created by sds100 on 31/05/2022.
 */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapBox(modifier: Modifier = Modifier) {
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

        Map(isMyLocationEnabled)

        if (!isMyLocationEnabled) {
            MyLocationPlaceholderButton(
                onClick = { locationPermissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
fun Map(isMyLocationEnabled: Boolean, modifier: Modifier = Modifier) {
    var uiSettings by remember { mutableStateOf(MapUiSettings()) }
    var properties by remember { mutableStateOf(MapProperties()) }

    uiSettings = uiSettings.copy(myLocationButtonEnabled = isMyLocationEnabled)
    properties = properties.copy(isMyLocationEnabled = isMyLocationEnabled)

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        uiSettings = uiSettings,
        properties = properties,
        contentDescription = stringResource(R.string.google_maps_content_description)
    )
}

@Composable
fun MyLocationPlaceholderButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Button(onClick = onClick, modifier) {
        Text("Request location permission")
    }
}

@Preview(showBackground = true)
@Composable
fun MyLocationPlaceholderButtonPreview() {
    MapcodeTheme {
        MyLocationPlaceholderButton()
        MyLocationPlaceholderButton()
        Text(text = "test")
    }
}

@Composable
fun MapcodeInfo(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize())
}

@Composable
fun MapScreen(viewModel: MapViewModel) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Column {
            MapBox(Modifier.weight(0.7f))
            MapcodeInfo(Modifier.weight(0.3f))
        }
    }
}

@Preview
@Composable
fun MapScreenPreview() {
    MapScreen(viewModel = hiltViewModel())
}