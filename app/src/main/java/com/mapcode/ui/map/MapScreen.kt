package com.mapcode.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Created by sds100 on 31/05/2022.
 */

@Composable
fun MapScreen() {
    val viewModel: MapViewModel = viewModel()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Text("Test")
    }
}

@Preview
@Composable
fun MapScreenPreview() {
    MapScreen()
}