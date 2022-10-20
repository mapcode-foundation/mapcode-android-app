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

package com.mapcode.map

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mapcode.BuildConfig
import com.mapcode.R
import com.mapcode.destinations.FavouritesScreenDestination
import com.mapcode.favourites.Favourite
import com.mapcode.theme.Green600
import com.mapcode.theme.MapcodeTheme
import com.mapcode.theme.Yellow300
import com.mapcode.util.Location
import com.mapcode.util.ScrollableDialog
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import kotlinx.coroutines.launch

@Destination
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<FavouritesScreenDestination, Location>,
    /**
     * This option makes instrumentation tests much quicker and easier to implement.
     */
    renderGoogleMaps: Boolean = true,
    layoutType: LayoutType = LayoutType.HorizontalInfoArea
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val cantFindLocationMessage = stringResource(R.string.cant_find_my_location_snackbar)
    val cantFindMapsAppMessage = stringResource(R.string.no_map_app_installed_error)

    val showSnackbar: (String) -> Unit = { message ->
        scope.launch {
            //dismiss current snack bar so they aren't queued up
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
            scaffoldState.snackbarHostState.showSnackbar(message)
        }
    }

    if (viewModel.showCantFindLocationSnackBar) {
        LaunchedEffect(viewModel.showCantFindLocationSnackBar) {
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
            val result = scaffoldState.snackbarHostState.showSnackbar(cantFindLocationMessage)

            if (result == SnackbarResult.Dismissed) {
                viewModel.showCantFindLocationSnackBar = false
            }
        }
    }

    if (viewModel.showCantFindMapsAppSnackBar) {
        LaunchedEffect(viewModel.showCantFindMapsAppSnackBar) {
            scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
            val result = scaffoldState.snackbarHostState.showSnackbar(cantFindMapsAppMessage)

            if (result == SnackbarResult.Dismissed) {
                viewModel.showCantFindMapsAppSnackBar = false
            }
        }
    }

    Surface {
        Scaffold(
            modifier = modifier,
            scaffoldState = scaffoldState
        ) { padding ->
            when (layoutType) {
                LayoutType.VerticalInfoArea -> VerticalInfoAreaLayout(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    viewModel,
                    navigator,
                    showSnackbar,
                    renderGoogleMaps
                )
                LayoutType.HorizontalInfoArea -> HorizontalInfoAreaLayout(
                    Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    viewModel,
                    navigator,
                    showSnackbar,
                    renderGoogleMaps
                )
                LayoutType.FloatingInfoArea -> FloatingInfoAreaLayout(
                    Modifier.padding(padding),
                    viewModel,
                    navigator,
                    showSnackbar,
                    renderGoogleMaps
                )
            }
        }
    }

    resultRecipient.onNavResult { result ->
        if (result is NavResult.Value) {
            val location = result.value
            val update = CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude),
                17f
            )
            viewModel.cameraPositionState.move(update)
        }
    }
}

@Composable
private fun VerticalInfoAreaLayout(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    navigator: DestinationsNavigator,
    showSnackbar: (String) -> Unit,
    renderGoogleMaps: Boolean
) {
    Row(modifier, horizontalArrangement = Arrangement.End) {
        Box(Modifier.weight(1f)) {
            MapWithCrossHairs(
                Modifier.fillMaxSize(),
                viewModel,
                renderGoogleMaps = renderGoogleMaps
            )

            MapControls(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                viewModel = viewModel,
                navigator = navigator,
                showSnackbar = showSnackbar
            )
        }

        InfoArea(
            Modifier
                .width(300.dp)
                .align(Alignment.Bottom)
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .systemBarsPadding(),
            viewModel,
            showSnackbar = showSnackbar,
            isVerticalLayout = true
        )
    }
}

@Composable
private fun HorizontalInfoAreaLayout(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    navigator: DestinationsNavigator,
    showSnackbar: (String) -> Unit,
    renderGoogleMaps: Boolean
) {
    Column(modifier, verticalArrangement = Arrangement.Bottom) {
        Box(Modifier.weight(1f)) {
            MapWithCrossHairs(Modifier.fillMaxSize(), viewModel, renderGoogleMaps)

            MapControls(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),

                viewModel = viewModel,
                navigator = navigator,
                showSnackbar = showSnackbar
            )
        }

        InfoArea(
            modifier = Modifier
                .wrapContentHeight()
                .padding(8.dp),
            viewModel = viewModel,
            showSnackbar = showSnackbar,
            isVerticalLayout = false,
        )
    }
}

@Composable
private fun FloatingInfoAreaLayout(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    navigator: DestinationsNavigator,
    showSnackbar: (String) -> Unit,
    renderGoogleMaps: Boolean
) {
    Box(modifier) {
        MapWithCrossHairs(Modifier.fillMaxSize(), viewModel, renderGoogleMaps)

        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            MapControls(
                modifier = Modifier.align(Alignment.Bottom),
                viewModel = viewModel,
                navigator = navigator,
                showSnackbar = showSnackbar
            )

            Spacer(Modifier.width(8.dp))

            Card(
                Modifier.width(400.dp),
                elevation = 4.dp
            ) {
                InfoArea(
                    modifier = Modifier.padding(8.dp),
                    viewModel = viewModel,
                    showSnackbar = showSnackbar,
                    isVerticalLayout = false,
                )
            }
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    val websiteUrl = stringResource(R.string.website_url)
//    val sourceCodeUrl = stringResource(R.string.source_code_url)
//    val changelogUrl = stringResource(R.string.changelog_url)

    ScrollableDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.about_dialog_title, BuildConfig.VERSION_NAME),
        buttonText = stringResource(R.string.close_dialog_button)
    ) {
        Column {
            val dialogTextStyle = MaterialTheme.typography.body1
            val inlineContent = mapOf(
                "share_icon" to InlineTextContent(
                    Placeholder(
                        width = dialogTextStyle.fontSize,
                        height = dialogTextStyle.fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Icon(Icons.Outlined.Share, "")
                },
                "directions_icon" to InlineTextContent(
                    Placeholder(
                        width = dialogTextStyle.fontSize,
                        height = dialogTextStyle.fontSize,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Directions,
                        contentDescription = ""
                    )
                },
            )

            val aboutString = buildAnnotatedString {
                pushStyle(dialogTextStyle.toSpanStyle())

                append(stringResource(R.string.copyright_welcome))
                append("\n\n")

                pushStyle(MaterialTheme.typography.subtitle2.toSpanStyle())
                append(stringResource(R.string.how_to_use_header))
                pop()

                append("\n\n")
                append(stringResource(R.string.how_to_use_address))
                append("\n\n")
                append(stringResource(R.string.how_to_use_territory))

                append("\n\n")
                append(stringResource(R.string.how_to_use_share_1))
                appendInlineContent("share_icon")
                append(stringResource(R.string.how_to_use_share_2))

                append("\n\n")
                append(stringResource(R.string.how_to_use_directions_1))
                appendInlineContent("directions_icon")
                append(stringResource(R.string.how_to_use_directions_2))

                append("\n\n")
                append(stringResource(R.string.visit_website_notice))

                pop()
            }

            Text(text = aboutString, inlineContent = inlineContent)
            Spacer(Modifier.height(8.dp))
            DialogContentButton(
                icon = painterResource(R.drawable.web),
                text = stringResource(R.string.website_button)
            ) {
                uriHandler.openUri(websiteUrl)
            }
            // HIDE THESE UNTIL REPO IS MADE PUBLIC
//            DialogContentButton(
//                icon = painterResource(R.drawable.ic_outline_article_24),
//                text = stringResource(R.string.changelog_button)
//            ) {
//                uriHandler.openUri(changelogUrl)
//            }
//            DialogContentButton(
//                icon = painterResource(R.drawable.ic_outline_code_24),
//                text = stringResource(R.string.source_code_button)
//            ) {
//                uriHandler.openUri(sourceCodeUrl)
//            }
        }
    }
}

@Preview(heightDp = 500)
@Composable
private fun AboutDialogPreview() {
    MapcodeTheme {
        AboutDialog()
    }
}

@Composable
private fun DialogContentButton(icon: Painter, text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun greyButtonColors(): ButtonColors {
    return ButtonDefaults.buttonColors(
        backgroundColor = Color.LightGray,
        contentColor = Color.DarkGray
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MapControls(
    modifier: Modifier,
    viewModel: MapViewModel,
    navigator: DestinationsNavigator,
    showSnackbar: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isSatelliteModeEnabled by remember { derivedStateOf { viewModel.mapProperties.mapType == MapType.HYBRID } }

    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    var showMoreDropdown by rememberSaveable { mutableStateOf(false) }

    val locationPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val isLocationPermissionGranted by remember {
        derivedStateOf {
            val coarseLocationPermission = locationPermissionsState.permissions
                .single { it.permission == Manifest.permission.ACCESS_COARSE_LOCATION }

            coarseLocationPermission.status == PermissionStatus.Granted
        }
    }

    viewModel.setMyLocationEnabled(isLocationPermissionGranted)

    val uiState by viewModel.uiState.collectAsState()
    val noFavouritesMessage = stringResource(R.string.no_favourites_snackbar)

    Column(modifier = modifier) {
        MapControls(
            onSatelliteButtonClick = viewModel::onSatelliteButtonClick,
            isSatelliteModeEnabled = isSatelliteModeEnabled,
            onZoomInClick = {
                scope.launch {
                    viewModel.cameraPositionState.animate(
                        CameraUpdateFactory.zoomIn(),
                        MapViewModel.ANIMATE_CAMERA_UPDATE_DURATION_MS
                    )
                }
            },
            onZoomOutClick = {
                scope.launch {
                    viewModel.cameraPositionState.animate(
                        CameraUpdateFactory.zoomOut(),
                        MapViewModel.ANIMATE_CAMERA_UPDATE_DURATION_MS
                    )
                }
            },
            onMyLocationClick = {
                if (isLocationPermissionGranted) {
                    viewModel.goToMyLocation()
                } else {
                    locationPermissionsState.launchMultiplePermissionRequest()
                }
            },
            onSavedLocationsClick = {
                if (uiState.favouriteLocations.isEmpty()) {
                    showSnackbar(noFavouritesMessage)
                } else {
                    navigator.navigate(FavouritesScreenDestination)
                }
            },
            onMoreClick = { showMoreDropdown = true }
        )

        Box {
            MoreDropdownMenu(
                expanded = showMoreDropdown,
                onAboutClick = {
                    showAboutDialog = true
                    showMoreDropdown = false
                },
                onShareMapcodeClick = {
                    viewModel.shareMapcode()
                    showMoreDropdown = false
                },
                onDirectionsClick = {
                    viewModel.onDirectionsClick()
                    showMoreDropdown = false
                },
                onDismiss = {
                    showMoreDropdown = false
                }
            )
        }
    }
}

@Composable
private fun MapControls(
    modifier: Modifier = Modifier,
    onSatelliteButtonClick: () -> Unit = {},
    isSatelliteModeEnabled: Boolean,
    onZoomInClick: () -> Unit = {},
    onZoomOutClick: () -> Unit = {},
    onMyLocationClick: () -> Unit = {},
    onSavedLocationsClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    val satelliteButtonColors: ButtonColors = if (isSatelliteModeEnabled) {
        ButtonDefaults.buttonColors(backgroundColor = Yellow300, contentColor = Color.Black)
    } else {
        ButtonDefaults.buttonColors(backgroundColor = Green600, contentColor = Color.White)
    }

    val satelliteButtonIcon: ImageVector = if (isSatelliteModeEnabled) {
        Icons.Outlined.Map
    } else {
        Icons.Outlined.Satellite
    }

    Row(modifier) {
        Button(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Bottom),
            onClick = onMoreClick,
            contentPadding = PaddingValues(8.dp),
            colors = greyButtonColors()
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_content_description)
            )
        }

        Spacer(Modifier.width(8.dp))

        Button(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Bottom),
            onClick = onSavedLocationsClick,
            contentPadding = PaddingValues(8.dp),
            colors = greyButtonColors()
        ) {
            Icon(
                imageVector = Icons.Outlined.Bookmarks,
                contentDescription = stringResource(R.string.view_favourites_button_content_description)
            )
        }

        Spacer(Modifier.width(8.dp))

        Button(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Bottom),
            onClick = onSatelliteButtonClick,
            contentPadding = PaddingValues(8.dp),
            colors = satelliteButtonColors
        ) {
            Icon(
                imageVector = satelliteButtonIcon,
                contentDescription = stringResource(R.string.satellite_mode_button_content_description)
            )
        }
        Spacer(Modifier.width(8.dp))
        MyLocationButton(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Bottom),
            onClick = onMyLocationClick
        )

        Spacer(Modifier.width(8.dp))
        ZoomControls(onZoomInClick = onZoomInClick, onZoomOutClick = onZoomOutClick)
    }
}

@Preview(showBackground = true)
@Composable
private fun MapControlsPreview() {
    var isSatelliteModeEnabled by remember { mutableStateOf(false) }
    MapcodeTheme {
        MapControls(
            isSatelliteModeEnabled = isSatelliteModeEnabled,
            onSatelliteButtonClick = { isSatelliteModeEnabled = !isSatelliteModeEnabled }
        )
    }
}

@Composable
private fun MoreDropdownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onAboutClick: () -> Unit,
    onShareMapcodeClick: () -> Unit,
    onDirectionsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        modifier = modifier,
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(onClick = onAboutClick) {
            Text(text = stringResource(R.string.about_menu_item))
        }

        DropdownMenuItem(onClick = onShareMapcodeClick) {
            Text(text = stringResource(R.string.share_mapcode_menu_item))
        }

        DropdownMenuItem(onClick = onDirectionsClick) {
            Text(text = stringResource(R.string.directions_menu_item))
        }
    }
}

@Composable
private fun ZoomControls(
    modifier: Modifier = Modifier,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit
) {
    Column(modifier) {
        Button(
            modifier = Modifier.size(48.dp),
            onClick = onZoomInClick,
            colors = greyButtonColors(),
            contentPadding = PaddingValues(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.zoom_in_button_content_description)
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            modifier = Modifier.size(48.dp),
            onClick = onZoomOutClick,
            colors = greyButtonColors(),
            contentPadding = PaddingValues(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.minus),
                contentDescription = stringResource(R.string.zoom_out_button_content_description)
            )
        }
    }
}

@Composable
private fun MapWithCrossHairs(
    modifier: Modifier,
    viewModel: MapViewModel,
    renderGoogleMaps: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val contentPadding = WindowInsets.statusBars.asPaddingValues()

    Box(modifier) {
        if (renderGoogleMaps) {
            Map(
                properties = viewModel.mapProperties,
                cameraPositionState = viewModel.cameraPositionState,
                contentPadding = contentPadding,
                favouriteLocations = uiState.favouriteLocations
            )
        }

        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(contentPadding),
            painter = painterResource(R.drawable.crosshairs),
            contentDescription = null,
            tint = Color.Black
        )
    }
}

/**
 * This handles the Google Map component.
 */
@Composable
private fun Map(
    modifier: Modifier = Modifier,
    properties: MapProperties,
    cameraPositionState: CameraPositionState,
    favouriteLocations: List<Favourite>,
    contentPadding: PaddingValues
) {
    val scope = rememberCoroutineScope()

    val uiSettings =
        remember { MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false) }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
        properties = properties,
        onMapClick = { position ->
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(position),
                    MapViewModel.ANIMATE_CAMERA_UPDATE_DURATION_MS
                )
            }
        },
        contentDescription = stringResource(R.string.google_maps_content_description),
        onMyLocationClick = { location ->
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)),
                    MapViewModel.ANIMATE_CAMERA_UPDATE_DURATION_MS
                )
            }
        },
        onPOIClick = { poi ->
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(poi.latLng),
                    MapViewModel.ANIMATE_CAMERA_UPDATE_DURATION_MS
                )
            }
        },
        contentPadding = contentPadding
    ) {
        favouriteLocations.forEach { favourite ->
            Marker(
                state = MarkerState(
                    position = LatLng(
                        favourite.location.latitude,
                        favourite.location.longitude
                    )
                ),
                title = favourite.name
            )
        }
    }
}

/**
 * This is the button to request location permission.
 */
@Composable
private fun MyLocationButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Button(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = stringResource(R.string.my_location_button_content_description)
        )
    }
}