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
    private val _mapcodeInfoState: MutableStateFlow<MapcodeInfoState> = MutableStateFlow(MapcodeInfoState(""))
    val mapcodeInfoState: StateFlow<MapcodeInfoState> = _mapcodeInfoState.asStateFlow()

    fun onCameraMoved(lat: Double, long: Double) {
        val mapcodes = mapcodeUseCase.getMapcodes(lat, long)

        _mapcodeInfoState.update { state ->
            state.copy(mapcode = mapcodes[0].code)
        }
    }
}

data class MapcodeInfoState(val mapcode: String)