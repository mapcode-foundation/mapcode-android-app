package com.mapcode.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapcode.Mapcode
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
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    companion object {
        private const val UNKNOWN_ADDRESS_ERROR_TIMEOUT: Long = 3000
    }

    private val mapcodes: MutableStateFlow<List<Mapcode>> = MutableStateFlow(emptyList())
    private val mapcodeIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val mapcode: Flow<Mapcode?> = combine(mapcodes, mapcodeIndex) { mapcodes, index ->
        if (index == -1) {
            null
        } else {
            mapcodes[index]
        }
    }

    private val address: MutableStateFlow<String> = MutableStateFlow("")
    private val addressError: MutableStateFlow<AddressError> = MutableStateFlow(AddressError.None)
    private val addressHelper: MutableStateFlow<AddressHelper> = MutableStateFlow(AddressHelper.None)
    val location: MutableStateFlow<Location> = MutableStateFlow(Location(0.0, 0.0))
    val zoom: MutableStateFlow<Float> = MutableStateFlow(1f)

    val mapcodeInfoState: StateFlow<MapcodeInfoState> =
        combine(
            mapcode,
            address,
            addressError,
            addressHelper,
            location
        ) { mapcode, address, addressError, addressHelper, location ->
            val code: String
            val territoryShort: String
            val territoryLong: String

            if (mapcode == null) {
                code = ""
                territoryShort = ""
                territoryLong = ""
            } else {
                code = mapcode.code
                territoryShort = mapcode.territory.name
                territoryLong = mapcode.territory.fullName
            }

            MapcodeInfoState(
                code = code,
                territoryShort = territoryShort,
                territoryLong = territoryLong,
                address = address,
                addressError = addressError,
                addressHelper = addressHelper,
                latitude = location.latitude.toString(),
                longitude = location.longitude.toString()
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MapcodeInfoState.EMPTY)

    private var clearUnknownAddressErrorJob: Job? = null

    /**
     * When the camera has moved the mapcode information should be updated.
     */
    fun onCameraMoved(lat: Double, long: Double, zoom: Float) {
        this.location.value = Location(lat, long)
        this.zoom.value = zoom

        //update the mapcode when the map moves
        val newMapcodes = useCase.getMapcodes(lat, long)
        mapcodes.value = newMapcodes

        if (newMapcodes.isEmpty()) {
            mapcodeIndex.value = -1
        } else {
            mapcodeIndex.value = 0
        }

        //update the address when the map moves
        val addressResult = useCase.reverseGeocode(lat, long)
        updateAddress(addressResult)
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

    /**
     * After querying the address information update the UI state.
     */
    private fun onResolveAddressQuery(query: String, result: Result<Location>) {
        result.onSuccess { newLocation ->
            location.value = newLocation

            val newMapcodes = useCase.getMapcodes(newLocation.latitude, newLocation.longitude)
            mapcodes.value = newMapcodes

            if (newMapcodes.isEmpty()) {
                mapcodeIndex.value = -1
            } else {
                mapcodeIndex.value = 0
            }

            val newAddressResult = useCase.reverseGeocode(newLocation.latitude, newLocation.longitude)
            updateAddress(newAddressResult)
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
}

data class MapcodeInfoState(
    val code: String,
    val territoryShort: String,
    val territoryLong: String,
    val address: String,
    val addressHelper: AddressHelper,
    val addressError: AddressError,
    val latitude: String,
    val longitude: String
) {
    companion object {
        val EMPTY: MapcodeInfoState = MapcodeInfoState(
            code = "",
            territoryShort = "",
            territoryLong = "",
            address = "",
            addressHelper = AddressHelper.None,
            addressError = AddressError.None,
            latitude = "",
            longitude = ""
        )
    }
}