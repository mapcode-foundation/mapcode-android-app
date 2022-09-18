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

package com.mapcode.map

import com.mapcode.Mapcode
import com.mapcode.UnknownMapcodeException
import com.mapcode.favourites.Favourite
import com.mapcode.util.Location
import com.mapcode.util.NoAddressException
import com.mapcode.util.UnknownAddressException
import java.io.IOException
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

class FakeShowMapcodeUseCase : ShowMapcodeUseCase {
    var clipboard: String? = null
        private set

    var hasInternetConnection: Boolean = true
    var currentLocation: Location? = null
    var isMapsAppInstalled: Boolean = true

    val knownLocations: MutableList<FakeLocation> = mutableListOf()
    val matchingAddresses: MutableMap<String, List<String>> = mutableMapOf()
    val favourites: MutableList<Favourite> = mutableListOf()
    var sharedMapcode: Mapcode? = null
        private set

    override fun getMapcodes(lat: Double, long: Double): List<Mapcode> {
        return knownLocations
            .find { it.latitude == lat && it.longitude == long }
            ?.mapcodes ?: emptyList()
    }

    override fun decodeMapcode(mapcode: String): Result<Location> {
        val matchingLocation = knownLocations.find { location ->
            location.mapcodes.any { it.codeWithTerritory == mapcode }
        }

        if (matchingLocation == null) {
            return failure(UnknownMapcodeException(""))
        } else {
            return success(Location(matchingLocation.latitude, matchingLocation.longitude))
        }
    }

    override fun copyToClipboard(text: String) {
        clipboard = text
    }

    override suspend fun geocode(address: String): Result<Location> {
        if (!hasInternetConnection) {
            return failure(IOException())
        }

        val fakeLocation = knownLocations.find { it.addresses.contains(address) }

        if (fakeLocation == null) {
            return failure(UnknownAddressException())
        } else {
            return success(Location(fakeLocation.latitude, fakeLocation.longitude))
        }
    }

    override suspend fun reverseGeocode(lat: Double, long: Double): Result<String> {
        if (!hasInternetConnection) {
            return failure(IOException())
        }

        val fakeLocation = knownLocations.find { it.latitude == lat && it.longitude == long }

        if (fakeLocation == null || fakeLocation.addresses.isEmpty()) {
            return failure(NoAddressException())
        } else {
            return success(fakeLocation.addresses.first())
        }
    }

    override suspend fun getLastLocation(): Location? {
        return currentLocation
    }

    override suspend fun getMatchingAddresses(
        query: String,
        maxResults: Int,
        southwest: Location,
        northeast: Location
    ): Result<List<String>> {
        return success(matchingAddresses[query] ?: emptyList())
    }

    override fun saveLastLocationAndZoom(location: Location, zoom: Float) {

    }

    override suspend fun getLastLocationAndZoom(): Pair<Location, Float>? {
        return null
    }

    override suspend fun saveFavourite(name: String, location: Location) {
        favourites.add(
            Favourite(
                name = name,
                location = location
            )
        )
    }

    override fun launchDirectionsToLocation(location: Location, zoom: Float): Boolean {
        return isMapsAppInstalled
    }

    override fun shareMapcode(mapcode: Mapcode) {
        sharedMapcode = mapcode
    }
}