package com.mapcode.map

import kotlinx.serialization.Serializable

@Serializable
data class ApiMapcodeResponse(
    val mapcodes: List<ApiMapcode> = emptyList(),
    val territories: List<ApiTerritoryHint>? = null
)

@Serializable
data class ApiMapcode(
    val mapcode: String,
    val territory: String? = null
)

@Serializable
data class ApiTerritoryHint(val alphaCode: String)
