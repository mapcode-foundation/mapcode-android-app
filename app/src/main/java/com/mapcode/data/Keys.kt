package com.mapcode.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey

/**
 * Created by sds100 on 02/07/2022.
 */
object Keys {
    val lastLocationLatitude: Preferences.Key<Double> = doublePreferencesKey("last_location_lat")
    val lastLocationLongitude: Preferences.Key<Double> = doublePreferencesKey("last_location_long")
    val lastLocationZoom: Preferences.Key<Float> = floatPreferencesKey("last_location_zoom")
}