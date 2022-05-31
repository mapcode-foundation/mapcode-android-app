package com.mapcode.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.mapcode.ui.theme.MapcodeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MapcodeApp()
        }
    }
}

@Composable
fun MapcodeApp() {
    MapcodeTheme {
        val navController = rememberNavController()
        MapcodeNavHost(navController)
    }
}