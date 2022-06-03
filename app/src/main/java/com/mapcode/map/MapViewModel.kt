package com.mapcode.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapcode.Mapcode
import com.mapcode.util.Location
import com.mapcode.util.NoAddressException
import com.mapcode.util.UnknownAddressException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject

/**
 * Created by sds100 on 31/05/2022.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val useCase: ShowMapcodeUseCase,
) : ViewModel() {

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
    private val location: MutableStateFlow<Location> = MutableStateFlow(Location(0.0, 0.0))

    val mapcodeInfoState: StateFlow<MapcodeInfoState> =
        combine(
            mapcode,
            address,
            addressError,
            addressHelper,
            location
        ) { mapcode, address, addressError, addressHelper, location ->
            val code: String
            val territory: String

            if (mapcode == null) {
                code = ""
                territory = ""
            } else {
                code = mapcode.code
                territory = mapcode.territory.name
            }

            MapcodeInfoState(
                code = code,
                territory = territory,
                address = address,
                addressError = addressError,
                addressHelper = addressHelper,
                latitude = location.latitude.toString(),
                longitude = location.longitude.toString()
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MapcodeInfoState.EMPTY)

    /**
     * When the camera has moved the mapcode information should be updated.
     */
    fun onCameraMoved(lat: Double, long: Double) {
        location.value = Location(lat, long)

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
    fun queryAddress(newAddress: String) {
        //first try to decode it as a mapcode, if that fails then try to geocode it as an address
        val newLocationResult: Result<Location>

        val decodeMapcodeResult = useCase.decodeMapcode(newAddress)

        if (decodeMapcodeResult.isSuccess) {
            newLocationResult = decodeMapcodeResult
        } else {
            newLocationResult = useCase.geocode(newAddress)
        }

        newLocationResult
            .onSuccess { newLocation ->
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
            }
            .onFailure { error ->
                when (error) {
                    is IOException -> {
                        addressHelper.value = AddressHelper.NoInternet
                    }
                    is UnknownAddressException -> {
                        addressError.value = AddressError.UnknownAddress(newAddress)
                    }
                }

                address.value = "" //clear address if error
            }
    }

    private fun updateAddress(addressResult: Result<String>) {
        addressResult
            .onSuccess { newAddress ->
                address.value = newAddress
                addressHelper.value = AddressHelper.None
            }
            .onFailure { error ->
                when (error) {
                    is IOException -> addressHelper.value = AddressHelper.NoInternet
                    is NoAddressException -> addressHelper.value = AddressHelper.NoAddress
                }
            }
    }
}

data class MapcodeInfoState(
    val code: String,
    val territory: String,
    val address: String,
    val addressHelper: AddressHelper,
    val addressError: AddressError,
    val latitude: String,
    val longitude: String
) {
    companion object {
        val EMPTY: MapcodeInfoState = MapcodeInfoState(
            code = "",
            territory = "",
            address = "",
            addressHelper = AddressHelper.None,
            addressError = AddressError.None,
            latitude = "",
            longitude = ""
        )
    }
}