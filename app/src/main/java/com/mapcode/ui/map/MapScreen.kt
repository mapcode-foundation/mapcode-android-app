package com.mapcode.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.mapcode.R

/**
 * Created by sds100 on 31/05/2022.
 */

@Composable
fun Map(modifier: Modifier = Modifier) {
    var uiSettings by remember { mutableStateOf(MapUiSettings(myLocationButtonEnabled = true)) }
    val properties by remember { mutableStateOf(MapProperties(isMyLocationEnabled = true)) }

    uiSettings = uiSettings.copy(myLocationButtonEnabled = true)
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        uiSettings = uiSettings,
        properties = properties,
        onMyLocationButtonClick = {
            false
        },
        onMyLocationClick = {},
        contentDescription = stringResource(R.string.google_maps_content_description),
    )
}

@Composable
fun MapcodeInfo(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize())
}

@Composable
fun MapScreen(viewModel: MapViewModel) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Column {
            Map(Modifier.weight(0.7f))
            MapcodeInfo(Modifier.weight(0.3f))
        }
    }
}

@Preview
@Composable
fun MapScreenPreview() {
    MapScreen(viewModel = hiltViewModel())
}