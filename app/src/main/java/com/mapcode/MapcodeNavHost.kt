package com.mapcode

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mapcode.map.MapScreen
import com.mapcode.map.MapViewModel

/**
 * Created by sds100 on 31/05/2022.
 */

@Composable
fun MapcodeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: MapViewModel
) {
    NavHost(
        navController = navController,
        startDestination = MapcodeDestination.Map.name,
        modifier = modifier
    ) {
        composable(MapcodeDestination.Map.name) {
            MapScreen(viewModel)
        }
    }
}