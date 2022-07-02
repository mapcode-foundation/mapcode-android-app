package com.mapcode.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.mapcode.Mapcode
import com.mapcode.data.Keys
import com.mapcode.data.PreferenceRepository
import com.mapcode.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    }

    private val mapcodes: MutableStateFlow<List<Mapcode>> = MutableStateFlow(emptyList())
    private val mapcodeIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val mapcode: Flow<Mapcode?> = combine(mapcodes, mapcodeIndex) { mapcodes, index ->
        if (index == -1 || index >= mapcodes.size) {
            null
        } else {
            mapcodes[index]
        }
    }

    private val address: MutableStateFlow<String> = MutableStateFlow("")
    private val addressError: MutableStateFlow<AddressError> = MutableStateFlow(AddressError.None)
    private val addressHelper: MutableStateFlow<AddressHelper> = MutableStateFlow(AddressHelper.None)
    private val addressUi: Flow<AddressUi> =
        combine(address, addressError, addressHelper) { address, addressError, addressHelper ->
            AddressUi(address, addressError, addressHelper)
        }

    private val territoryUi: Flow<TerritoryUi> =
        combine(mapcodeIndex, mapcodes, mapcode) { mapcodeIndex, mapcodes, mapcode ->
            if (mapcode == null) {
                TerritoryUi("", "", 0, 0)
            } else {
                TerritoryUi(
                    mapcode.territory.name,
                    mapcode.territory.fullName,
                    mapcodeIndex + 1,
                    mapcodes.size
                )
            }
        }

    val zoom: MutableStateFlow<Float> = MutableStateFlow(0f)
    val location: MutableStateFlow<Location> = MutableStateFlow(Location(0.0, 0.0))
    private val locationStringFormat = "%.7f"

    val uiState: StateFlow<UiState> =
        combine(
            mapcode,
            addressUi,
            territoryUi,
            location
        ) { mapcode, addressUi, territoryUi, location ->
            UiState(
                code = mapcode?.code ?: "",
                addressUi = addressUi,
                territoryUi = territoryUi,
                latitude = String.format(locationStringFormat, location.latitude),
                longitude = String.format(locationStringFormat, location.longitude)
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.EMPTY)

    private var clearUnknownAddressErrorJob: Job? = null

    var isGoogleMapsSdkLoaded: Boolean = false
    var cameraPositionState: CameraPositionState by mutableStateOf(CameraPositionState())
        private set

    var showCantFindLocationSnackBar: Boolean by mutableStateOf(false)

    /**
     * When the camera has moved the mapcode information should be updated.
     */
    fun onCameraMoved(lat: Double, long: Double, zoom: Float) {
        location.value = Location(lat, long)
        this.zoom.value = zoom

        //update the mapcode when the map moves
        updateMapcodes(lat, long)

        //update the address when the map moves
        val addressResult = useCase.reverseGeocode(lat, long)
        updateAddress(addressResult)
    }

    private fun moveCamera(lat: Double, long: Double, zoom: Float) {
        viewModelScope.launch(dispatchers.main) {
            if (isGoogleMapsSdkLoaded) {
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(lat, long), zoom)
                cameraPositionState.move(cameraUpdate)
            }

            onCameraMoved(lat, long, zoom)
        }
    }

    fun onMyLocationClick() {
        viewModelScope.launch {
            val myLocation = useCase.getLastLocation()

            if (myLocation == null) {
                showCantFindLocationSnackBar = true
            } else {
                showCantFindLocationSnackBar = false
                moveCamera(myLocation.latitude, myLocation.longitude, 16f)
            }
        }
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
            val resolveAddressResult: Result<Location>

            val decodeMapcodeResult = useCase.decodeMapcode(query)

            if (decodeMapcodeResult.isSuccess) {
                resolveAddressResult = decodeMapcodeResult
            } else {
                resolveAddressResult = useCase.geocode(query)
            }

            onResolveAddressQuery(query, resolveAddressResult)
        }
    }

    fun queryLatitude(query: String) {
        if (query.isEmpty()) {
            return
        }

        val cleansedLatitude = LocationUtils.cleanseLatitude(query.toDouble())
        moveCamera(cleansedLatitude, location.value.longitude, 0f)
    }

    fun queryLongitude(query: String) {
        if (query.isEmpty()) {
            return
        }

        val cleansedLongitude = LocationUtils.cleanseLongitude(query.toDouble())
        moveCamera(location.value.latitude, cleansedLongitude, 0f)
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

    fun restoreLastLocation() {
        viewModelScope.launch {
            val lastLatitude = preferences.get(Keys.lastLocationLatitude).first() ?: return@launch
            val lastLongitude = preferences.get(Keys.lastLocationLongitude).first() ?: return@launch
            val lastZoom = preferences.get(Keys.lastLocationZoom).first() ?: return@launch

            moveCamera(lastLatitude, lastLongitude, lastZoom)
        }
    }

    /**
     * After querying the address information update the UI state.
     */
    private fun onResolveAddressQuery(query: String, result: Result<Location>) {
        result.onSuccess { newLocation ->
            moveCamera(newLocation.latitude, newLocation.longitude, 0f)
        }.onFailure { error ->
            when (error) {
                is IOException -> {
                    addressHelper.value = AddressHelper.NoInternet
                }
                is UnknownAddressException -> {
                    addressError.value = AddressError.UnknownAddress(query)

                    clearUnknownAddressErrorJob?.cancel()
                    clearUnknownAddressErrorJob = viewModelScope.launch {
                        delay(UNKNOWN_ADDRESS_ERROR_TIMEOUT)
                        addressError.value = AddressError.None
                    }
                }
            }

            address.value = "" //clear address if error
        }
    }

    private fun updateAddress(addressResult: Result<String>) {
        addressResult
            .onSuccess { newAddress ->
                address.value = newAddress

                // only show the last 2 parts of the address if the address is longer than 2 parts
                if (newAddress.split(',').size <= 2) {
                    addressHelper.value = AddressHelper.None
                } else {
                    val lastTwoParts = getLastTwoPartsOfAddress(newAddress)
                    addressHelper.value = AddressHelper.Location(lastTwoParts)
                }
            }
            .onFailure { error ->
                when (error) {
                    is IOException -> addressHelper.value = AddressHelper.NoInternet
                    is NoAddressException -> addressHelper.value = AddressHelper.NoAddress
                }

                address.value = ""
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
}

data class UiState(
    val code: String,
    val territoryUi: TerritoryUi,
    val addressUi: AddressUi,
    val latitude: String,
    val longitude: String
) {
    companion object {
        val EMPTY: UiState = UiState(
            code = "",
            territoryUi = TerritoryUi("", "", 0, 0),
            addressUi = AddressUi("", AddressError.None, AddressHelper.None),
            latitude = "",
            longitude = ""
        )
    }
}