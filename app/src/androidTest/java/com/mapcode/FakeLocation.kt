package com.mapcode

/**
 * Created by sds100 on 02/06/2022.
 */
data class FakeLocation(
    val latitude: Double,
    val longitude: Double,
    val addresses: List<String>,
    val mapcodes: List<Mapcode>
)
