package com.mapcode.map

import javax.inject.Inject

/**
 * Created by sds100 on 01/06/2022.
 */

class ShowMapcodeUseCaseImpl @Inject constructor() : ShowMapcodeUseCase {
    override fun getMapcodes(): List<String> {
        TODO("Not yet implemented")
    }
}

interface ShowMapcodeUseCase {
    fun getMapcodes(): List<String>
}