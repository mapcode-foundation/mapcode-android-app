package com.mapcode.map

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Created by sds100 on 31/05/2022.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapcodeUseCase: ShowMapcodeUseCase
) : ViewModel() {
    private val _mapcodeInfoState: MutableStateFlow<MapcodeInfoState> = MutableStateFlow(MapcodeInfoState.EMPTY)
    val mapcodeInfoState: StateFlow<MapcodeInfoState> = _mapcodeInfoState.asStateFlow()

    /**
     * When the camera has moved the mapcode information should be updated.
     */
    fun onCameraMoved(lat: Double, long: Double) {
        val mapcodes = mapcodeUseCase.getMapcodes(lat, long)
        val firstMapcode = mapcodes[0]

        _mapcodeInfoState.update { state ->
            state.copy(mapcode = firstMapcode.code, territory = firstMapcode.territory.name)
        }
    }
}

data class MapcodeInfoState(val mapcode: String, val territory: String) {
    companion object {
        val EMPTY: MapcodeInfoState = MapcodeInfoState("", "")
    }
}