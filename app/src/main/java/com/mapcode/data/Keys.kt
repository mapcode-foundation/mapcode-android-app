/*
 * Copyright (C) 2022, Stichting Mapcode Foundation (http://www.mapcode.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mapcode.data

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object Keys {
    val lastLocationLatitude: Preferences.Key<Double> = doublePreferencesKey("last_location_lat")
    val lastLocationLongitude: Preferences.Key<Double> = doublePreferencesKey("last_location_long")
    val lastLocationZoom: Preferences.Key<Float> = floatPreferencesKey("last_location_zoom")
    val favourites: Preferences.Key<Set<String>> = stringSetPreferencesKey("favourites")
}