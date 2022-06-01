package com.mapcode.map

import com.mapcode.Mapcode
import com.mapcode.MapcodeCodec
import javax.inject.Inject

/**
 * Created by sds100 on 01/06/2022.
 */

class ShowMapcodeUseCaseImpl @Inject constructor() : ShowMapcodeUseCase {
    override fun getMapcodes(lat: Double, long: Double): List<Mapcode> {
        return MapcodeCodec.encode(lat, long)
    }
}

/**
 * This handles getting mapcode information for the UI layer.
 */
interface ShowMapcodeUseCase {
    fun getMapcodes(lat: Double, long: Double): List<Mapcode>
}