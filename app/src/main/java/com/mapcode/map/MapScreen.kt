package com.mapcode.map

import android.Manifest
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.mapcode.theme.Green600
import com.mapcode.theme.MapcodeTheme
import com.mapcode.theme.Yellow300
import com.mapcode.util.ScrollableDialog
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 31/05/2022.
 */

@Composable
fun AboutDialog(onDismiss: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    val websiteUrl = stringResource(R.string.website_url)
    val sourceCodeUrl = stringResource(R.string.source_code_url)

    ScrollableDialog(
        onDismiss = onDismiss, title = stringResource(R.string.about_dialog_title, BuildConfig.VERSION_NAME),
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
                        painter = painterResource(R.drawable.ic_outline_directions_24),
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
            DialogContentButton(
                icon = painterResource(R.drawable.ic_outline_article_24),
                text = stringResource(R.string.changelog_button)
            ) {
            }
            DialogContentButton(
                icon = painterResource(R.drawable.ic_outline_code_24),
                text = stringResource(R.string.source_code_button)
            ) {
                uriHandler.openUri(sourceCodeUrl)
            }
        }
    }
}

@Composable
fun DialogContentButton(icon: Painter, text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Preview(heightDp = 500)
@Composable
fun AboutDialogPreview() {
    MapcodeTheme {
        AboutDialog()
    }
}

@Composable
fun greyButtonColors(): ButtonColors {
    return ButtonDefaults.buttonColors(backgroundColor = Color.LightGray, contentColor = Color.DarkGray)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapControls(
    modifier: Modifier,
    viewModel: MapViewModel
) {
    val scope = rememberCoroutineScope()
    val isSatelliteModeEnabled by derivedStateOf { viewModel.mapProperties.mapType == MapType.HYBRID }

    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

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

    MapControls(
        modifier = modifier,
        onSatelliteButtonClick = viewModel::onSatelliteButtonClick,
        isSatelliteModeEnabled = isSatelliteModeEnabled,
        onZoomInClick = {
            scope.launch {
                viewModel.cameraPositionState.animate(
                    CameraUpdateFactory.zoomIn(),
                    ANIMATE_CAMERA_UPDATE_DURATION_MS
                )
            }
        },
        onZoomOutClick = {
            scope.launch {
                viewModel.cameraPositionState.animate(
                    CameraUpdateFactory.zoomOut(),
                    ANIMATE_CAMERA_UPDATE_DURATION_MS
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
        onDirectionsClick = viewModel::onDirectionsClick,
        onShareMapcodeClick = viewModel::shareMapcode,
        onAboutClick = { showAboutDialog = true }
    )
}

@Composable
fun MapControls(
    modifier: Modifier = Modifier,
    onSatelliteButtonClick: () -> Unit = {},
    isSatelliteModeEnabled: Boolean,
    onZoomInClick: () -> Unit = {},
    onZoomOutClick: () -> Unit = {},
    onMyLocationClick: () -> Unit = {},
    onDirectionsClick: () -> Unit = {},
    onShareMapcodeClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val greenButtonColors = ButtonDefaults.buttonColors(backgroundColor = Green600, contentColor = Color.White)
    val satelliteButtonColors: ButtonColors = if (isSatelliteModeEnabled) {
        ButtonDefaults.buttonColors(backgroundColor = Yellow300, contentColor = Color.Black)
    } else {
        greenButtonColors
    }

    val satelliteButtonIcon: Painter = if (isSatelliteModeEnabled) {
        painterResource(R.drawable.map_outline)
    } else {
        painterResource(R.drawable.satellite)
    }

    Row(modifier) {
        Button(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Bottom),
            onClick = onAboutClick,
            contentPadding = PaddingValues(8.dp),
            colors = greyButtonColors()
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.about_content_description)
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Bottom),
            onClick = onShareMapcodeClick,
            contentPadding = PaddingValues(8.dp),
            colors = greyButtonColors()
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.share_mapcode_content_description)
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.Bottom),
            onClick = onDirectionsClick,
            contentPadding = PaddingValues(8.dp),
            colors = greenButtonColors
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_directions_24),
                contentDescription = stringResource(R.string.open_external_maps_app_content_description)
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
                painter = satelliteButtonIcon,
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

@Composable
fun ZoomControls(
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

private const val ANIMATE_CAMERA_UPDATE_DURATION_MS = 200

@Composable
fun MapWithCrossHairs(
    modifier: Modifier,
    viewModel: MapViewModel,
    renderGoogleMaps: Boolean = true
) {
    Box(modifier) {
        if (renderGoogleMaps) {
            Map(
                properties = viewModel.mapProperties,
                onCameraMoved = viewModel::onCameraMoved,
                cameraPositionState = viewModel.cameraPositionState
            )
        }

        Icon(
            modifier = Modifier.align(Alignment.Center),
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
fun Map(
    modifier: Modifier = Modifier,
    properties: MapProperties,
    onCameraMoved: (Double, Double, Float) -> Unit,
    cameraPositionState: CameraPositionState
) {
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            onCameraMoved(
                cameraPositionState.position.target.latitude,
                cameraPositionState.position.target.longitude,
                cameraPositionState.position.zoom
            )
        }
    }

    val scope = rememberCoroutineScope()

    val uiSettings = remember { MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false) }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
        properties = properties,
        onMapClick = { position ->
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(position),
                    ANIMATE_CAMERA_UPDATE_DURATION_MS
                )
            }
        },
        contentDescription = stringResource(R.string.google_maps_content_description),
        onMyLocationClick = { location ->
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)),
                    ANIMATE_CAMERA_UPDATE_DURATION_MS
                )
            }
        },
        onPOIClick = { poi ->
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(poi.latLng),
                    ANIMATE_CAMERA_UPDATE_DURATION_MS
                )
            }
        },
        contentPadding = WindowInsets.statusBars.asPaddingValues()
    )
}

/**
 * This is the button to request location permission.
 */
@Composable
fun MyLocationButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
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

/**
 * A text field that has a clear button and handles refilling the text if it is cleared.
 */
@Composable
fun ClearableTextField(
    modifier: Modifier = Modifier,
    text: String,
    label: String,
    clearButtonContentDescription: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val focusManager = LocalFocusManager.current
    var isFocussed by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val textFieldValue: String

    if (isFocussed) {
        textFieldValue = query
    } else {
        textFieldValue = text
        query = text
    }

    OutlinedTextField(
        modifier = modifier
            .onFocusChanged {
                isFocussed = it.isFocused
            }
            .focusRequester(focusRequester),
        value = textFieldValue,
        singleLine = true,
        label = { Text(label, maxLines = 1) },
        onValueChange = { query = it },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go,
            keyboardType = keyboardType
        ),
        keyboardActions = KeyboardActions(onGo = {
            focusManager.clearFocus()
            onChange(query)
        }),
        placeholder = { Text(text, maxLines = 1) },
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        focusRequester.requestFocus()
                        query = ""
                    }) {
                    Icon(
                        Icons.Outlined.Clear,
                        contentDescription = clearButtonContentDescription
                    )
                }
            }
        })
}

@Composable
fun HeaderWithIcon(modifier: Modifier = Modifier, text: String, @DrawableRes icon: Int) {
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            modifier = Modifier.fillMaxHeight(),
            text = text,
            style = MaterialTheme.typography.subtitle2
        )

        Icon(
            modifier = Modifier.height(20.dp),
            painter = painterResource(icon),
            contentDescription = ""
        )
    }
}

/**
 * The box that shows the mapcode.
 */
@Composable
fun MapcodeBox(
    modifier: Modifier = Modifier,
    code: String,
    territory: String
) {
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.primary
    ) {
        Column(Modifier.padding(8.dp)) {
            HeaderWithIcon(
                Modifier.fillMaxWidth(),
                stringResource(R.string.mapcode_header_button),
                R.drawable.ic_outline_content_copy_24
            )

            val codeSpanStyle: SpanStyle =
                MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold).toSpanStyle()

            val styledString = buildAnnotatedString {
                pushStyle(MaterialTheme.typography.body2.toSpanStyle())
                append(territory)
                pop()
                append(" ")
                pushStyle(codeSpanStyle)
                append(code)
                pop()
            }

            Text(text = styledString)
        }
    }
}

/**
 * The box that shows the territory.
 */
@Composable
fun TerritoryBox(
    modifier: Modifier = Modifier,
    index: Int,
    count: Int,
    territoryName: String
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(8.dp)) {
            val headerText = stringResource(R.string.territory_header_button, index, count)
            HeaderWithIcon(
                modifier = Modifier.fillMaxWidth(),
                headerText,
                R.drawable.ic_outline_fast_forward_24
            )

            Text(
                text = territoryName,
                style = MaterialTheme.typography.body1,
                modifier = Modifier
            )
        }
    }
}

/**
 * The box that shows the latitude.
 */
@Composable
fun LatitudeTextBox(
    modifier: Modifier = Modifier,
    latitude: String,
    onChange: (String) -> Unit
) {
    ClearableTextField(
        modifier = modifier,
        text = latitude,
        onChange = onChange,
        label = stringResource(R.string.latitude_text_field_label),
        clearButtonContentDescription = stringResource(R.string.clear_latitude_content_description),
        keyboardType = KeyboardType.Decimal
    )
}

/**
 * The box that shows the longitude.
 */
@Composable
fun LongitudeTextBox(
    modifier: Modifier = Modifier,
    longitude: String,
    onChange: (String) -> Unit
) {
    ClearableTextField(
        modifier = modifier,
        text = longitude,
        onChange = onChange,
        label = stringResource(R.string.longitude_text_field_label),
        clearButtonContentDescription = stringResource(R.string.clear_longitude_content_description),
        keyboardType = KeyboardType.Decimal
    )
}

@Preview(showBackground = true)
@Composable
fun MapControlsPreview() {
    var isSatelliteModeEnabled by remember { mutableStateOf(false) }
    MapcodeTheme {
        MapControls(
            isSatelliteModeEnabled = isSatelliteModeEnabled,
            onSatelliteButtonClick = { isSatelliteModeEnabled = !isSatelliteModeEnabled }
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 300)
@Composable
fun MapcodeInfoBoxPreview() {
    MapcodeTheme {
        val state = UiState(
            mapcodeUi = MapcodeUi("AB.XY", "NLD", "Netherlands", 1, 1),
            addressUi = AddressUi(
                "I am a very very very very very very extremely long address",
                AddressError.UnknownAddress("Street, City"),
                AddressHelper.NoInternet,
            ),
            "1.0",
            "2.0"
        )
        InfoArea(modifier = Modifier.padding(8.dp), state = state, isVerticalLayout = false)
    }
}

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    /**
     * This option makes instrumentation tests much quicker and easier to implement.
     */
    renderGoogleMaps: Boolean = true,
    layoutType: LayoutType = LayoutType.HorizontalInfoArea
) {
    val scaffoldState = rememberScaffoldState()
    val cantFindLocationMessage = stringResource(R.string.cant_find_my_location_snackbar)
    val cantFindMapsAppMessage = stringResource(R.string.no_map_app_installed_error)

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
        Scaffold(modifier = Modifier.navigationBarsPadding(), scaffoldState = scaffoldState) {
            when (layoutType) {
                LayoutType.VerticalInfoArea -> VerticalInfoAreaLayout(viewModel, scaffoldState, renderGoogleMaps)
                LayoutType.HorizontalInfoArea -> HorizontalInfoAreaLayout(viewModel, scaffoldState, renderGoogleMaps)
                LayoutType.FloatingInfoArea -> FloatingInfoAreaLayout(viewModel, scaffoldState, renderGoogleMaps)
            }
        }
    }
}

@Composable
fun VerticalInfoAreaLayout(viewModel: MapViewModel, scaffoldState: ScaffoldState, renderGoogleMaps: Boolean) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
        Box(Modifier.weight(1f)) {
            MapWithCrossHairs(Modifier.fillMaxSize(), viewModel)

            MapControls(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                viewModel
            )
        }

        InfoArea(
            Modifier
                .width(300.dp)
                .align(Alignment.Bottom)
                .padding(8.dp),
            viewModel,
            scaffoldState,
            isVerticalLayout = true
        )
    }
}

@Composable
fun HorizontalInfoAreaLayout(viewModel: MapViewModel, scaffoldState: ScaffoldState, renderGoogleMaps: Boolean) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Box(Modifier.weight(1f)) {
            MapWithCrossHairs(Modifier.fillMaxSize(), viewModel, renderGoogleMaps)

            MapControls(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                viewModel
            )
        }

        InfoArea(
            Modifier
                .wrapContentHeight()
                .padding(8.dp),
            viewModel,
            scaffoldState,
            isVerticalLayout = false
        )
    }
}

@Composable
fun FloatingInfoAreaLayout(viewModel: MapViewModel, scaffoldState: ScaffoldState, renderGoogleMaps: Boolean) {
    Box {
        MapWithCrossHairs(Modifier.fillMaxSize(), viewModel, renderGoogleMaps)

        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {

            MapControls(Modifier.align(Alignment.Bottom), viewModel)
            Spacer(Modifier.width(8.dp))

            Card(
                Modifier.width(400.dp),
                elevation = 4.dp
            ) {
                InfoArea(Modifier.padding(8.dp), viewModel, scaffoldState, isVerticalLayout = false)
            }
        }
    }
}