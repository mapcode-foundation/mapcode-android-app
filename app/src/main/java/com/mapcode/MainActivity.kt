package com.mapcode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.MapsInitializer
import com.mapcode.map.MapViewModel
import com.mapcode.theme.MapcodeTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            Timber.e(widthSizeClass.toString())
            MapcodeApp(viewModel, widthSizeClass)
        }

        MapsInitializer.initialize(this)
        viewModel.isGoogleMapsSdkLoaded = true
    }

    override fun onPause() {
        viewModel.saveLocation()
        super.onPause()
    }
}

@Composable
fun MapcodeApp(viewModel: MapViewModel, widthSizeClass: WindowWidthSizeClass) {
    MapcodeTheme {
        val navController = rememberNavController()
        MapcodeNavHost(
            navController = navController,
            viewModel = viewModel,
            isExpandedScreen = widthSizeClass == WindowWidthSizeClass.Expanded
        )
    }
}