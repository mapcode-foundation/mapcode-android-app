package com.mapcode.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.maps.android.compose.GoogleMap

/**
 * Created by sds100 on 31/05/2022.
 */

@Composable
fun Map(modifier: Modifier = Modifier) {
    GoogleMap(
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun MapScreen(viewModel: MapViewModel) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Column {
            Map(Modifier.weight(0.7f))
        }
    }
}

@Preview
@Composable
fun MapScreenPreview() {
    MapScreen(viewModel = hiltViewModel())
}