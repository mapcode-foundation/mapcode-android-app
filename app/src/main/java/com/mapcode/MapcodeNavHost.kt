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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.mapcode.destinations.FavouritesScreenDestination
import com.mapcode.destinations.MapScreenDestination
import com.mapcode.destinations.OnboardingScreenDestination
import com.mapcode.favourites.FavouritesScreen
import com.mapcode.map.MapScreen
import com.mapcode.map.MapScreenLayoutType
import com.mapcode.map.MapViewModel
import com.mapcode.onboarding.OnboardingScreen
import com.mapcode.onboarding.OnboardingScreenLayoutType
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.scope.resultBackNavigator
import com.ramcosta.composedestinations.scope.resultRecipient

@Composable
fun MapcodeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mapViewModel: MapViewModel,
    windowSizeClass: WindowSizeClass
) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colors.isLight

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )

        systemUiController.setNavigationBarColor(color = Color.Black, darkIcons = false)
    }

    val appViewModel: AppViewModel = hiltViewModel()
    val showOnboarding: Boolean by appViewModel.showOnboarding.collectAsState()

    val startDestination = if (showOnboarding) {
        OnboardingScreenDestination
    } else {
        MapScreenDestination
    }

    DestinationsNavHost(
        navController = navController,
        navGraph = NavGraphs.root,
        modifier = modifier,
        startRoute = startDestination
    ) {
        composable(MapScreenDestination) {
            MapScreen(
                Modifier.fillMaxSize(),
                mapViewModel,
                layoutType = determineMapScreenLayout(windowSizeClass),
                navigator = destinationsNavigator,
                resultRecipient = resultRecipient()
            )
        }

        composable(FavouritesScreenDestination) {
            FavouritesScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = hiltViewModel(),
                resultBackNavigator = resultBackNavigator()
            )
        }

        composable(OnboardingScreenDestination) {
            OnboardingScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = appViewModel,
                navigator = destinationsNavigator,
                layoutType = determineOnboardingScreenLayout(windowSizeClass)
            )
        }
    }
}

private fun determineOnboardingScreenLayout(windowSizeClass: WindowSizeClass): OnboardingScreenLayoutType {
    return when {
        windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> OnboardingScreenLayoutType.Horizontal
        else -> OnboardingScreenLayoutType.Vertical
    }
}

private fun determineMapScreenLayout(windowSizeClass: WindowSizeClass): MapScreenLayoutType {
    return when {
        windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
            && windowSizeClass.widthSizeClass < WindowWidthSizeClass.Expanded -> MapScreenLayoutType.VerticalInfoArea

        windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact
            && windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> MapScreenLayoutType.FloatingInfoArea

        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact -> MapScreenLayoutType.HorizontalInfoArea
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded -> MapScreenLayoutType.FloatingInfoArea
        else -> MapScreenLayoutType.HorizontalInfoArea
    }
}