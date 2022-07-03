package com.mapcode

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.MapsInitializer
import com.mapcode.map.MapViewModel
import com.mapcode.theme.MapcodeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        MapsInitializer.initialize(this)
        viewModel.isGoogleMapsSdkLoaded = true

        setContent {
            MapcodeApp(viewModel)
        }
    }

    override fun onPause() {
        viewModel.saveLocation()
        super.onPause()
    }
}

@Composable
fun MapcodeApp(viewModel: MapViewModel) {
    MapcodeTheme {
        val navController = rememberNavController()
        MapcodeNavHost(navController = navController, viewModel = viewModel)
    }
}