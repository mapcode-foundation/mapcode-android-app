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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.MapsInitializer
import com.google.android.libraries.places.api.Places
import com.mapcode.map.MapViewModel
import com.mapcode.theme.MapcodeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            MapcodeApp(viewModel, windowSizeClass)
        }

        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        MapsInitializer.initialize(this)
        viewModel.isGoogleMapsSdkLoaded = true
    }

    override fun onPause() {
        viewModel.saveLocation()
        super.onPause()
    }
}

@Composable
fun MapcodeApp(viewModel: MapViewModel, windowSizeClass: WindowSizeClass) {
    MapcodeTheme {
        val navController = rememberNavController()
        MapcodeNavHost(
            modifier = Modifier.navigationBarsPadding(),
            navController = navController,
            viewModel = viewModel,
            windowSizeClass = windowSizeClass
        )
    }
}