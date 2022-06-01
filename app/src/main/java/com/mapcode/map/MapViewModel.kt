package com.mapcode.map

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
}

data class MapcodeInfoState(val mapcode: String)