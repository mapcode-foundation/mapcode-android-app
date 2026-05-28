package com.mapcode.map

import com.mapcode.Mapcode

fun sortMapcodesByHint(mapcodes: List<Mapcode>, hints: List<String>): List<Mapcode> {
    val hintIndex = hints.withIndex().associate { (i, alphaCode) -> alphaCode to i }
    return mapcodes.sortedWith(
        compareBy(
            { hintIndex[it.territory.name] ?: Int.MAX_VALUE },
            { it.code.length }
        )
    )
}
