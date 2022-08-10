/*
 * Copyright (C) 2022, Stichting Mapcode Foundation (http://www.mapcode.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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