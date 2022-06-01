package com.mapcode.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapcode.Mapcode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Created by sds100 on 31/05/2022.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val useCase: ShowMapcodeUseCase
) : ViewModel() {

    private val mapcodes: MutableStateFlow<List<Mapcode>> = MutableStateFlow(emptyList())
    private val mapcodeIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    private val address: MutableStateFlow<String> = MutableStateFlow("")

    val mapcodeInfoState: StateFlow<MapcodeInfoState> =
        combine(mapcodes, mapcodeIndex, address) { mapcodes, mapcodeIndex, address ->
            if (mapcodeIndex == -1) {
                return@combine MapcodeInfoState.EMPTY
            }

            val mapcode = mapcodes[mapcodeIndex]

            MapcodeInfoState(
                code = mapcode.code,
                territory = mapcode.territory.name,
                address = address,
                addressError = AddressError.None
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, MapcodeInfoState.EMPTY)

    /**
     * When the camera has moved the mapcode information should be updated.
     */
    fun onCameraMoved(lat: Double, long: Double) {
        val newMapcodes = useCase.getMapcodes(lat, long)
        mapcodes.value = newMapcodes

        if (newMapcodes.isNotEmpty()) {
            mapcodeIndex.value = 0
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
    fun findAddress(newAddress: String) {
        address.value = newAddress
    }
}

data class MapcodeInfoState(
    val code: String,
    val territory: String,
    val address: String,
    val addressError: AddressError
) {
    companion object {
        val EMPTY: MapcodeInfoState = MapcodeInfoState("", "", "", AddressError.None)
    }
}