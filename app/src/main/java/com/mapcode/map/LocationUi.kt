package com.mapcode.map

/**
 * Created by sds100 on 05/08/2022.
 */
data class LocationUi(
    val latitudeText: String,
    val latitudePlaceholder: String,
    val showLatitudeInvalidError: Boolean,
    val longitudeText: String,
    val longitudePlaceholder: String,
    val showLongitudeInvalidError: Boolean
) {
    companion object {
        val EMPTY = LocationUi("", "", false, "", "", false)
    }
}