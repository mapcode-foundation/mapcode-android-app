package com.mapcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mapcode.map.MapScreen
import com.mapcode.map.MapViewModel

/**
 * Created by sds100 on 31/05/2022.
 */

@Composable
fun MapcodeNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = MapcodeDestination.Map.name,
        modifier = modifier
    ) {
        composable(MapcodeDestination.Map.name) {
            val viewModel: MapViewModel = hiltViewModel()
            MapScreen(viewModel)
        }
    }
}