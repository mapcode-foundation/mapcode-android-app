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

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.mapcode.Mapcode
import com.mapcode.Territory
import com.mapcode.favourites.Favourite
import com.mapcode.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val useCase: ShowMapcodeUseCase,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {
    companion object {
        private const val UNKNOWN_ADDRESS_ERROR_TIMEOUT: Long = 3000
        const val ANIMATE_CAMERA_UPDATE_DURATION_MS: Int = 200
        private const val AUTOCOMPLETE_ADDRESS_DELAY_MS: Long = 1500
    }

    private val latLngNumberFormat: NumberFormat by lazy { NumberFormat.getNumberInstance(Locale.getDefault()) }

    private val mapcodes: MutableStateFlow<List<Mapcode>> = MutableStateFlow(emptyList())
    private val mapcodeIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val addressUi: MutableStateFlow<AddressUi> =
        MutableStateFlow(AddressUi("", emptyList(), AddressError.None, AddressHelper.None))

    private val mapcodeUi: Flow<MapcodeUi> =
        combine(mapcodeIndex, mapcodes) { mapcodeIndex, mapcodes ->
            val mapcode = mapcodes.getOrNull(mapcodeIndex)

            if (mapcode == null) {
                MapcodeUi("", "", "", 0, 0)
            } else {
                MapcodeUi.fromMapcode(mapcode, mapcodeIndex, mapcodes.size)
            }
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val zoom: MutableStateFlow<Float> = MutableStateFlow(0f)
    private val location: MutableStateFlow<Location> = MutableStateFlow(Location(0.0, 0.0))
    private val locationStringFormat = "%.7f"
    private val locationUi: MutableStateFlow<LocationUi> = MutableStateFlow(LocationUi.EMPTY)

    val uiState: StateFlow<UiState> =
        combine(
            addressUi,
            mapcodeUi,
            location,
            locationUi,
            useCase.getFavouriteLocations()
        ) { addressUi, mapcodeUi, location, locationUi, favouriteLocations ->
            val isFavouriteLocation = isFavouriteLocation(location, favouriteLocations)
            UiState(
                addressUi = addressUi,
                mapcodeUi = mapcodeUi,
                locationUi = locationUi,
                favouriteLocations = favouriteLocations,
                isFavouriteLocation = isFavouriteLocation
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.EMPTY)

    private var clearUnknownAddressErrorJob: Job? = null
    private var getMatchingAddressesJob: Job? = null

    var isGoogleMapsSdkLoaded: Boolean = false
    var cameraPositionState: CameraPositionState by mutableStateOf(getInitialCameraPositionState())

    var mapProperties: MapProperties by mutableStateOf(MapProperties())
        private set

    var showCantFindLocationSnackBar: Boolean by mutableStateOf(false)

    var showCantFindMapsAppSnackBar: Boolean by mutableStateOf(false)

    init {
        val isMovingFlow = snapshotFlow { cameraPositionState.isMoving }

        snapshotFlow { cameraPositionState.position }
            .dropWhile { !isGoogleMapsSdkLoaded }
            .distinctUntilChanged()
            .combine(isMovingFlow) { position, isMoving ->
                if (!isMoving) {
                    onCameraMoved(
                        lat = position.target.latitude,
                        long = position.target.longitude,
                        zoom = position.zoom,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * When the camera has moved the mapcode information should be updated.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onCameraMoved(lat: Double, long: Double, zoom: Float) {
        location.value = Location(lat, long)
        locationUi.update {
            val latitudeText = String.format(locationStringFormat, lat)
            val longitudeText = String.format(locationStringFormat, long)

            LocationUi(
                latitudeText = latitudeText,
                latitudePlaceholder = latitudeText,
                showLatitudeInvalidError = false,
                longitudeText = longitudeText,
                longitudePlaceholder = longitudeText,
                showLongitudeInvalidError = false
            )
        }
        this.zoom.value = zoom

        //update the mapcode when the map moves
        updateMapcodes(lat, long)

        viewModelScope.launch {
            //update the address when the map moves
            val addressResult = useCase.reverseGeocode(lat, long)
            updateAddress(addressResult)
        }
    }

    private fun moveCamera(lat: Double, long: Double, zoom: Float, animate: Boolean = false) {
        viewModelScope.launch(dispatchers.main) {
            if (isGoogleMapsSdkLoaded) {
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(lat, long), zoom)

                if (animate) {
                    cameraPositionState.animate(cameraUpdate, ANIMATE_CAMERA_UPDATE_DURATION_MS)
                } else {
                    cameraPositionState.move(cameraUpdate)
                }
            }

            onCameraMoved(lat, long, zoom)
        }
    }

    fun goToMyLocation() {
        viewModelScope.launch {
            val myLocation = useCase.getLastLocation()

            if (myLocation == null) {
                showCantFindLocationSnackBar = true
            } else {
                showCantFindLocationSnackBar = false
                moveCamera(myLocation.latitude, myLocation.longitude, 16f, animate = true)
            }
        }
    }

    fun onSatelliteButtonClick() {
        val mapType = if (mapProperties.mapType == MapType.NORMAL) {
            MapType.HYBRID
        } else {
            MapType.NORMAL
        }

        mapProperties = mapProperties.copy(mapType = mapType)
    }

    fun setMyLocationEnabled(enabled: Boolean) {
        mapProperties = mapProperties.copy(isMyLocationEnabled = enabled)
    }

    /**
     * When the mapcode text is clicked.
     *
     * @return whether it copied successfully.
     */
    fun copyMapcode(): Boolean {
        if (mapcodes.value.isEmpty() || mapcodeIndex.value == -1) {
            return false
        }

        val mapcode = mapcodes.value[mapcodeIndex.value]

        val text = if (mapcode.territory == Territory.AAA) {
            mapcode.code
        } else {
            "${mapcode.territory.name} ${mapcode.code}"
        }

        useCase.copyToClipboard(text)
        return true
    }

    fun onAddressTextChange(text: String) {
        //if the user is continuing the same search query then do not clear the matching addresses
        val clearMatchingAddresses = text.dropLast(1) != addressUi.value.address

        addressUi.update {
            if (clearMatchingAddresses) {
                it.copy(address = text, matchingAddresses = emptyList())
            } else {
                it.copy(address = text)
            }
        }

        getMatchingAddressesJob?.cancel()

        if (text.isNotEmpty()) {
            getMatchingAddressesJob = viewModelScope.launch(dispatchers.default) {
                delay(AUTOCOMPLETE_ADDRESS_DELAY_MS)

                val latLngBounds = withContext(dispatchers.main) {
                    cameraPositionState.projection?.visibleRegion?.latLngBounds
                }
                val maxResults = 10

                val matchingAddresses = if (latLngBounds != null) {
                    useCase.getMatchingAddresses(
                        text,
                        maxResults = maxResults,
                        southwest = Location(
                            latLngBounds.southwest.latitude,
                            latLngBounds.southwest.longitude
                        ),
                        northeast = Location(
                            latLngBounds.northeast.latitude,
                            latLngBounds.northeast.longitude
                        )
                    ).getOrNull()
                } else {
                    useCase.getMatchingAddresses(
                        text,
                        maxResults = maxResults,
                        southwest = Location.GLOBE_SOUTH_WEST,
                        northeast = Location.GLOBE_NORTH_EAST
                    ).getOrNull()
                }

                if (matchingAddresses == null) {
                    return@launch
                }

                addressUi.update { it.copy(matchingAddresses = matchingAddresses.distinct()) }
            }
        }
    }

    /**
     * Find the address or mapcode and move to that location on the map.
     */
    fun onSubmitAddress() {
        val addressText = addressUi.value.address
        if (addressText.isEmpty()) {
            return
        }

        getMatchingAddressesJob?.cancel()

        //first try to decode it as a mapcode, if that fails then try to geocode it as an address
        viewModelScope.launch(dispatchers.io) {
            val resolveAddressResult: Result<Location> =
                useCase.decodeMapcode(addressText).recoverCatching {
                    val mapcode = mapcodes.value[mapcodeIndex.value]
                    val queryWithTerritory = "${mapcode.territory.name} $addressText"

                    val decodeQueryWithCurrentTerritoryResult =
                        useCase.decodeMapcode(queryWithTerritory)

                    decodeQueryWithCurrentTerritoryResult.getOrThrow()
                }.recoverCatching {
                    useCase.geocode(addressText).getOrThrow()
                }

            onResolveAddressQuery(addressText, resolveAddressResult)
        }
    }

    fun onLatitudeTextChanged(text: String) {
        val isDecimal = try {
            latLngNumberFormat.parse(text)
            true
        } catch (e: ParseException) {
            false
        }

        val isValid = text.isEmpty() || isDecimal

        locationUi.update {
            it.copy(latitudeText = text, showLatitudeInvalidError = !isValid)
        }
    }

    fun onSubmitLatitude() {
        val text = locationUi.value.latitudeText

        if (text.isEmpty()) {
            val latitudeText = String.format(locationStringFormat, location.value.latitude)
            locationUi.update {
                it.copy(
                    latitudeText = latitudeText,
                    latitudePlaceholder = latitudeText,
                    showLatitudeInvalidError = false
                )
            }
        } else {
            val latitude = try {
                latLngNumberFormat.parse(text)!!.toDouble()
            } catch (e: ParseException) {
                return
            }

            val cleansedLatitude = LocationUtils.cleanseLatitude(latitude)
            moveCamera(cleansedLatitude, location.value.longitude, 17f)
        }
    }

    fun onLongitudeTextChanged(text: String) {
        val isDecimal = try {
            latLngNumberFormat.parse(text)
            true
        } catch (e: ParseException) {
            false
        }

        val isValid = text.isEmpty() || isDecimal

        locationUi.update {
            it.copy(longitudeText = text, showLongitudeInvalidError = !isValid)
        }
    }

    fun onSubmitLongitude() {
        val text = locationUi.value.longitudeText

        if (text.isEmpty()) {
            val longitudeText = String.format(locationStringFormat, location.value.longitude)
            locationUi.update {
                it.copy(
                    longitudeText = longitudeText,
                    longitudePlaceholder = longitudeText,
                    showLongitudeInvalidError = false
                )
            }
        } else {
            val longitude = try {
                latLngNumberFormat.parse(text)!!.toDouble()
            } catch (e: ParseException) {
                return
            }

            val cleansedLongitude = LocationUtils.cleanseLongitude(longitude)
            moveCamera(location.value.latitude, cleansedLongitude, 17f)
        }
    }

    /**
     * Copy the latitude and longitude to the clipboard: <lat>.<long>
     */
    fun copyLocation() {
        location.value.also { location ->
            val decimalFormat = DecimalFormat("0.#######").apply {
                roundingMode = RoundingMode.HALF_DOWN
            }

            val latitudeText = decimalFormat.format(location.latitude).toString()
            val longitudeText = decimalFormat.format(location.longitude).toString()

            useCase.copyToClipboard("$latitudeText,$longitudeText")
        }
    }

    fun onTerritoryClick() {
        if (mapcodeIndex.value == -1) {
            return
        }

        if (mapcodeIndex.value == mapcodes.value.size - 1) {
            mapcodeIndex.value = 0
        } else {
            mapcodeIndex.value++
        }
    }

    fun saveLocation() {
        useCase.saveLastLocationAndZoom(location = location.value, zoom = zoom.value)
    }

    fun onDirectionsClick() {
        val success = useCase.launchDirectionsToLocation(location.value, zoom.value)

        showCantFindMapsAppSnackBar = !success
    }

    fun onSaveFavouriteClick(name: String) {
        viewModelScope.launch {
            useCase.saveFavourite(name = name, location = location.value)
        }
    }

    fun onDeleteFavouriteClick() {
        viewModelScope.launch {
            useCase.deleteFavourite(location.value)
        }
    }

    private fun getInitialCameraPositionState(): CameraPositionState {
        val lastCameraPosition = getLastCameraPosition()
        if (lastCameraPosition == null) {
            return CameraPositionState()
        } else {
            return CameraPositionState(lastCameraPosition)
        }
    }

    private fun getLastCameraPosition(): CameraPosition? {
        return runBlocking {
            val lastLocationAndZoom = useCase.getLastLocationAndZoom() ?: return@runBlocking null

            CameraPosition.fromLatLngZoom(
                LatLng(
                    lastLocationAndZoom.first.latitude,
                    lastLocationAndZoom.first.longitude
                ), lastLocationAndZoom.second
            )
        }
    }

    /**
     * After querying the address information update the UI state.
     */
    private fun onResolveAddressQuery(query: String, result: Result<Location>) {
        result.onSuccess { newLocation ->
            moveCamera(newLocation.latitude, newLocation.longitude, 17f)
        }.onFailure { error ->
            val addressHelper: AddressHelper
            val addressError: AddressError

            when (error) {
                is IOException -> {
                    addressHelper = AddressHelper.NoInternet
                    addressError = AddressError.None
                }
                is UnknownAddressException -> {
                    addressError = AddressError.UnknownAddress(query)
                    addressHelper = AddressHelper.None

                    clearUnknownAddressErrorJob?.cancel()
                    clearUnknownAddressErrorJob = viewModelScope.launch {
                        delay(UNKNOWN_ADDRESS_ERROR_TIMEOUT)
                        addressUi.update { it.copy(error = AddressError.None) }
                    }
                }
                else -> throw error
            }

            addressUi.value = AddressUi(
                address = "",//clear address if error
                helper = addressHelper,
                error = addressError,
                matchingAddresses = emptyList()
            )
        }
    }

    private fun updateAddress(addressResult: Result<String>) {
        addressResult.onSuccess { newAddress ->
            // only show the last 2 parts of the address if the address is longer than 2 parts
            val addressHelper: AddressHelper = if (newAddress.split(',').size <= 2) {
                AddressHelper.None
            } else {
                val lastTwoParts = getLastTwoPartsOfAddress(newAddress)
                AddressHelper.Location(lastTwoParts)
            }

            addressUi.value = AddressUi(
                address = newAddress,
                helper = addressHelper,
                error = AddressError.None,
                matchingAddresses = emptyList()
            )
        }.onFailure { error ->
            val addressHelper: AddressHelper = when (error) {
                is IOException -> AddressHelper.NoInternet
                is NoAddressException -> AddressHelper.NoAddress
                else -> AddressHelper.None
            }

            addressUi.value = AddressUi(
                address = "",
                helper = addressHelper,
                error = AddressError.None,
                matchingAddresses = emptyList()
            )
        }
    }

    private fun getLastTwoPartsOfAddress(address: String): String {
        return address.split(',')
            .map { it.trim() }
            .takeLast(2)
            .joinToString()
    }

    private fun updateMapcodes(lat: Double, long: Double) {
        //remove duplicate mapcodes for a territory because only the highest priority one should be shown.
        val newMapcodes = useCase.getMapcodes(lat, long).distinctBy { it.territory }
        mapcodes.value = newMapcodes

        if (newMapcodes.isEmpty()) {
            mapcodeIndex.value = -1
        } else {
            mapcodeIndex.value = 0
        }
    }

    fun shareMapcode() {
        val mapcode = mapcodes.value.getOrNull(mapcodeIndex.value) ?: return
        useCase.shareMapcode(mapcode)
    }

    private fun isFavouriteLocation(location: Location, favourites: List<Favourite>): Boolean {
        return favourites.any { it.location == location }
    }
}

data class UiState(
    val mapcodeUi: MapcodeUi,
    val addressUi: AddressUi,
    val locationUi: LocationUi,
    val favouriteLocations: List<Favourite>,
    val isFavouriteLocation: Boolean
) {
    companion object {
        val EMPTY: UiState = UiState(
            mapcodeUi = MapcodeUi("", "", "", 0, 0),
            addressUi = AddressUi("", emptyList(), AddressError.None, AddressHelper.None),
            locationUi = LocationUi.EMPTY,
            favouriteLocations = emptyList(),
            isFavouriteLocation = false
        )
    }
}