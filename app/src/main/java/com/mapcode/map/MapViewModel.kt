package com.mapcode.map

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.mapcode.Mapcode
import com.mapcode.data.Keys
import com.mapcode.data.PreferenceRepository
import com.mapcode.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sds100 on 31/05/2022.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val useCase: ShowMapcodeUseCase,
    private val preferences: PreferenceRepository,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    companion object {
        private const val UNKNOWN_ADDRESS_ERROR_TIMEOUT: Long = 3000
        const val ANIMATE_CAMERA_UPDATE_DURATION_MS = 200
    }

    private val mapcodes: MutableStateFlow<List<Mapcode>> = MutableStateFlow(emptyList())
    private val mapcodeIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val addressUi: MutableStateFlow<AddressUi> =
        MutableStateFlow(AddressUi("", AddressError.None, AddressHelper.None))

    private val mapcodeUi: Flow<MapcodeUi> = combine(mapcodeIndex, mapcodes) { mapcodeIndex, mapcodes ->
        val mapcode = mapcodes.getOrNull(mapcodeIndex)

        if (mapcode == null) {
            MapcodeUi("", "", "", 0, 0)
        } else {
            MapcodeUi(
                mapcode.code,
                mapcode.territory.name,
                mapcode.territory.fullName,
                mapcodeIndex + 1,
                mapcodes.size
            )
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
            locationUi
        ) { addressUi, mapcodeUi, locationUi ->
            UiState(
                addressUi = addressUi,
                mapcodeUi = mapcodeUi,
                locationUi = locationUi
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.EMPTY)

    private var clearUnknownAddressErrorJob: Job? = null

    var isGoogleMapsSdkLoaded: Boolean = false
    var cameraPositionState: CameraPositionState by mutableStateOf(getInitialCameraPositionState())
        private set

    var mapProperties: MapProperties by mutableStateOf(MapProperties())
        private set

    var showCantFindLocationSnackBar: Boolean by mutableStateOf(false)

    var showCantFindMapsAppSnackBar: Boolean by mutableStateOf(false)

    /**
     * When the camera has moved the mapcode information should be updated.
     */
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
        useCase.copyToClipboard("${mapcode.territory.name} ${mapcode.code}")
        return true
    }

    /**
     * Find the address or mapcode and move to that location on the map.
     */
    fun queryAddress(query: String) {
        if (query.isEmpty()) {
            return
        }

        //first try to decode it as a mapcode, if that fails then try to geocode it as an address
        viewModelScope.launch(dispatchers.io) {
            val resolveAddressResult: Result<Location> =
                useCase.decodeMapcode(query).recoverCatching {
                    val mapcode = mapcodes.value[mapcodeIndex.value]
                    val queryWithTerritory = "${mapcode.territory.name} $query"

                    val decodeQueryWithCurrentTerritoryResult = useCase.decodeMapcode(queryWithTerritory)

                    decodeQueryWithCurrentTerritoryResult.getOrThrow()
                }.recoverCatching {
                    useCase.geocode(query).getOrThrow()
                }

            onResolveAddressQuery(query, resolveAddressResult)
        }
    }

    fun onLatitudeTextChanged(text: String) {
        val isValid = text.isEmpty() || text.toDoubleOrNull() != null

        locationUi.update {
            it.copy(latitudeText = text, showLatitudeInvalidError = !isValid)
        }
    }

    fun onSubmitLatitude() {
        val text = locationUi.value.latitudeText

        if (text.isNotEmpty() && text.toDoubleOrNull() == null) {
            return
        }

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
            val cleansedLatitude = LocationUtils.cleanseLatitude(text.toDouble())
            moveCamera(cleansedLatitude, location.value.longitude, 17f)
        }
    }

    fun onLongitudeTextChanged(text: String) {
        val isValid = text.isEmpty() || text.toDoubleOrNull() != null

        locationUi.update {
            it.copy(longitudeText = text, showLongitudeInvalidError = !isValid)
        }
    }

    fun onSubmitLongitude() {
        val text = locationUi.value.longitudeText

        if (text.isNotEmpty() && text.toDoubleOrNull() == null) {
            return
        }

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
            val cleansedLongitude = LocationUtils.cleanseLongitude(text.toDouble())
            moveCamera(location.value.latitude, cleansedLongitude, 17f)
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
        preferences.set(Keys.lastLocationLatitude, location.value.latitude)
        preferences.set(Keys.lastLocationLongitude, location.value.longitude)
        preferences.set(Keys.lastLocationZoom, zoom.value)
    }

    fun onDirectionsClick() {
        val success = useCase.launchDirectionsToLocation(location.value, zoom.value)

        showCantFindMapsAppSnackBar = !success
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
            val lastLatitude = preferences.get(Keys.lastLocationLatitude).first() ?: return@runBlocking null
            val lastLongitude = preferences.get(Keys.lastLocationLongitude).first() ?: return@runBlocking null
            val lastZoom = preferences.get(Keys.lastLocationZoom).first() ?: return@runBlocking null

            CameraPosition.fromLatLngZoom(LatLng(lastLatitude, lastLongitude), lastZoom)
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
                error = addressError
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
                error = AddressError.None
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
                error = AddressError.None
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
        useCase.shareText(text = "$mapcode", description = "Mapcode: $mapcode")
    }
}

data class UiState(
    val mapcodeUi: MapcodeUi,
    val addressUi: AddressUi,
    val locationUi: LocationUi
) {
    companion object {
        val EMPTY: UiState = UiState(
            mapcodeUi = MapcodeUi("", "", "", 0, 0),
            addressUi = AddressUi("", AddressError.None, AddressHelper.None),
            locationUi = LocationUi.EMPTY
        )
    }
}