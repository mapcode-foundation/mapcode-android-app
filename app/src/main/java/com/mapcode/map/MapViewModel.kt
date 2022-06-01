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

    val mapcodeInfoState: StateFlow<MapcodeInfoState> = combine(mapcodes, mapcodeIndex) { mapcodes, mapcodeIndex ->
        if (mapcodeIndex == -1) {
            return@combine MapcodeInfoState.EMPTY
        }

        val mapcode = mapcodes[mapcodeIndex]

        MapcodeInfoState(
            code = mapcode.code,
            territory = mapcode.territory.name
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MapcodeInfoState.EMPTY)

    /**
     * When the camera has moved the mapcode information should be updated.
     */
    fun onCameraMoved(lat: Double, long: Double) {
        mapcodes.value = useCase.getMapcodes(lat, long)
        mapcodeIndex.value = 0
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
}

data class MapcodeInfoState(val code: String, val territory: String) {
    companion object {
        val EMPTY: MapcodeInfoState = MapcodeInfoState("", "")
    }
}