package com.mapcode.map

/**
 * Created by sds100 on 30/06/2022.
 */
data class MapcodeUi(
    val code: String,
    /**
     * The short name of the territory. E.g NLD.
     */
    val territoryShortName: String,

    /**
     * The full name of the territory. E.g Netherlands.
     */
    val territoryFullName: String,

    /**
     * The number of the territory in the list. E.g 1 out of 3.
     */
    val number: Int,

    /**
     * How many different territories have mapcodes for this location.
     */
    val count: Int
)