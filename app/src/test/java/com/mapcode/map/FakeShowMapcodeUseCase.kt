package com.mapcode.map

import com.mapcode.Mapcode
import com.mapcode.UnknownMapcodeException
import com.mapcode.util.Location
import com.mapcode.util.NoAddressException
import com.mapcode.util.UnknownAddressException
import java.io.IOException
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

/**
 * Created by sds100 on 02/06/2022.
 */
class FakeShowMapcodeUseCase : ShowMapcodeUseCase {
    var clipboard: String? = null
    var hasInternetConnection: Boolean = true
    var currentLocation: Location? = null

    val knownLocations: MutableList<FakeLocation> = mutableListOf()

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

    override fun geocode(address: String): Result<Location> {
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

    override fun reverseGeocode(lat: Double, long: Double): Result<String> {
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
}