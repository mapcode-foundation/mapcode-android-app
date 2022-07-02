package com.mapcode.util

/**
 * Created by sds100 on 01/07/2022.
 */
object LocationUtils {
    fun cleanseLatitude(latitude: Double): Double {
        return when {
            latitude > 90 -> 90.0
            latitude < -90 -> -90.0
            else -> latitude
        }
    }

    fun cleanseLongitude(longitude: Double): Double {
        return when {
            longitude > 180 -> 180.0
            longitude < -180 -> -180.0
            else -> longitude
        }
    }
}