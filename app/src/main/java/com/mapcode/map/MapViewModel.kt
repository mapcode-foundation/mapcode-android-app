package com.mapcode.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapcode.Mapcode
import com.mapcode.util.UnknownAddressException
import com.mapcode.util.Location
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
    private val address: MutableStateFlow<String> = MutableStateFlow("")
    private val addressError: MutableStateFlow<AddressError> = MutableStateFlow(AddressError.None)
    private val location: MutableStateFlow<Location> = MutableStateFlow(Location(0.0, 0.0))

    val mapcodeInfoState: StateFlow<MapcodeInfoState> =
        combine(
            mapcodes,
            mapcodeIndex,
            address,
            addressError,
            location
        ) { mapcodes, mapcodeIndex, address, addressError, location ->
            val code: String
            val territory: String

            if (mapcodeIndex == -1) {
                code = ""
                territory = ""
            } else {
                val mapcode = mapcodes[mapcodeIndex]
                code = mapcode.code
                territory = mapcode.territory.name
            }

            MapcodeInfoState(
                code = code,
                territory = territory,
                address = address,
                addressError = addressError,
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

        if (newMapcodes.isNotEmpty()) {
            mapcodeIndex.value = 0
        }

        //update the address when the map moves
        val addressResult = useCase.reverseGeocode(lat, long)

        addressResult
            .onSuccess { newAddress ->
                address.value = newAddress
                addressError.value = AddressError.None
            }
            .onFailure { error ->
                when (error) {
                    is IOException -> addressError.value = AddressError.NoInternet
                    is UnknownAddressException -> addressError.value = AddressError.CantFindAddress
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
    fun queryAddress(newAddress: String) {
        address.value = newAddress
    }
}

data class MapcodeInfoState(
    val code: String,
    val territory: String,
    val address: String,
    val addressError: AddressError,
    val latitude: String,
    val longitude: String
) {
    companion object {
        val EMPTY: MapcodeInfoState = MapcodeInfoState("", "", "", AddressError.None, "", "")
    }
}