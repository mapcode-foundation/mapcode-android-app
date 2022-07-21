package com.mapcode

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mapcode.map.LayoutType
import com.mapcode.map.MapScreen
import com.mapcode.map.MapViewModel

/**
 * Created by sds100 on 31/05/2022.
 */

@Composable
fun MapcodeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: MapViewModel,
    windowSizeClass: WindowSizeClass
) {
    val layoutType: LayoutType = when {
        windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
            && windowSizeClass.widthSizeClass < WindowWidthSizeClass.Expanded -> LayoutType.VerticalInfoArea

        windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
            && windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> LayoutType.FloatingInfoArea

        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact -> LayoutType.HorizontalInfoArea
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> LayoutType.FloatingInfoArea
        else -> LayoutType.HorizontalInfoArea
    }

    NavHost(
        navController = navController,
        startDestination = MapcodeDestination.Map.name,
        modifier = modifier
    ) {
        composable(MapcodeDestination.Map.name) {
            MapScreen(viewModel, layoutType = layoutType)
        }
    }
}