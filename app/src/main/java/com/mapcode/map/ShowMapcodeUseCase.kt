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

import android.annotation.SuppressLint
import android.content.*
import android.location.Geocoder
import android.net.Uri
import androidx.core.content.getSystemService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.ktx.api.net.awaitFindAutocompletePredictions
import com.mapcode.Mapcode
import com.mapcode.MapcodeCodec
import com.mapcode.UnknownMapcodeException
import com.mapcode.UnknownPrecisionFormatException
import com.mapcode.util.Location
import com.mapcode.util.NoAddressException
import com.mapcode.util.UnknownAddressException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ShowMapcodeUseCaseImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope
) : ShowMapcodeUseCase {

    private val geocoder: Geocoder = Geocoder(ctx)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(ctx)
    private val okHttpClient: OkHttpClient = OkHttpClient()

    /**
     * Save the last location because on Google Maps it can still show the blue my location dot
     * if you disable GPS while the app is running.
     */
    private var cachedLastLocation: Location? = null

    private val placesClient: PlacesClient by lazy { Places.createClient(ctx) }

    override fun getMapcodes(lat: Double, long: Double): List<Mapcode> {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = HttpUrl.Builder()
                    .scheme("https")
                    .host("api.mapcode.com")
                    .addPathSegment("mapcode")
                    .addPathSegment("codes")
                    .addPathSegment("$lat,$long")
                    .addQueryParameter("client", "android")
                    .build()

                val request: Request = Request.Builder()
                    .url(url)
                    .build()

                okHttpClient.newCall(request).execute()
            } catch (_: Exception) {
                Timber.e("Failed to call mapcode API.")
            }
        }

        return MapcodeCodec.encode(lat, long)
    }

    override fun copyToClipboard(text: String) {
        val clipboardManager: ClipboardManager? = ctx.getSystemService()

        val clipData = ClipData.newPlainText(text, text)
        clipboardManager?.setPrimaryClip(clipData)
    }

    override suspend fun geocode(address: String): Result<Location> {
        try {
            //sending an empty string to Geocoder API results in IOException even if you have internet access
            //so return the correct exception here.
            if (address.isEmpty()) {
                return failure(UnknownAddressException())
            }

            val matchingAddress = withContext(Dispatchers.Default) {
                geocoder.getFromLocationName(address, 10)?.firstOrNull()
            }

            if (matchingAddress == null) {
                return failure(UnknownAddressException())
            } else {
                return success(Location(matchingAddress.latitude, matchingAddress.longitude))
            }
        } catch (e: IOException) {
            return failure(e)
        }
    }

    override suspend fun reverseGeocode(lat: Double, long: Double): Result<String> {
        try {
            val addressList = withContext(Dispatchers.Default) {
                geocoder.getFromLocation(lat, long, 1) ?: emptyList()
            }

            if (addressList.isEmpty()) {
                return failure(NoAddressException())
            }

            val address = addressList.single()

            val addressString = buildString {
                for (i in 0..address.maxAddressLineIndex) {
                    append(address.getAddressLine(i))
                }
            }

            return success(addressString)
        } catch (e: IOException) {
            return failure(e)
        }
    }

    override fun decodeMapcode(mapcode: String): Result<Location> {
        try {
            val point = MapcodeCodec.decode(mapcode)

            return success(Location(point.latDeg, point.lonDeg))
        } catch (e: UnknownMapcodeException) {
            return failure(e)
        } catch (e: UnknownPrecisionFormatException) {
            return failure(UnknownMapcodeException("Unknown mapcode $mapcode"))
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastLocation(): Location? = suspendCoroutine { continuation ->
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                continuation.resume(cachedLastLocation)
            } else {
                val newLocation = Location(location.latitude, location.longitude)
                cachedLastLocation = newLocation
                continuation.resume(newLocation)
            }
        }
    }

    override fun launchDirectionsToLocation(location: Location, zoom: Float): Boolean {
        try {
            val gmmIntentUri: Uri =
                Uri.parse("google.navigation:q=${location.latitude},${location.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            ctx.startActivity(mapIntent)
            return true
        } catch (e: ActivityNotFoundException) {
            return false
        }
    }

    override fun shareText(text: String, description: String) {
        val sendIntent: Intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, description)
        }

        val shareIntent = Intent.createChooser(sendIntent, null).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        ctx.startActivity(shareIntent)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getMatchingAddresses(
        query: String,
        maxResults: Int,
        southwest: Location,
        northeast: Location
    ): Result<List<String>> {
        val locationBias = RectangularBounds.newInstance(
            LatLng(southwest.latitude, southwest.longitude),
            LatLng(northeast.latitude, northeast.longitude)
        )

        val responseResult = withContext(Dispatchers.Default) {
            try {
                val response = placesClient.awaitFindAutocompletePredictions {
                    this.query = query
                    this.locationBias = locationBias
                }

                success(response)
            } catch (e: ApiException) {
                Timber.e(e.status.toString())
                failure(e)
            }
        }

        return responseResult.map { response ->
            response.autocompletePredictions
                .map { it.getFullText(null) }
                .map { it.toString() }
        }
    }
}

/**
 * This handles getting mapcode information for the UI layer.
 */
interface ShowMapcodeUseCase {
    /**
     * Get mapcode for a position on the map.
     */
    fun getMapcodes(lat: Double, long: Double): List<Mapcode>

    /**
     * Convert a mapcode into a latitude and longitude.
     * @return success if the mapcode is known and could be converted.
     * @return failure with [UnknownMapcodeException] if the mapcode can't be converted into a location.
     */
    fun decodeMapcode(mapcode: String): Result<Location>

    /**
     * Copy text to a clipboard.
     *
     * @param text The text to clip.
     */
    fun copyToClipboard(text: String)

    /**
     * Convert an address to a latitude and longitude.
     * @return [IOException] failure if the address can't be geocoded due to a network failure.
     * @return [UnknownAddressException] failure if the address doesn't exist and can't be geocoded.
     */
    suspend fun geocode(address: String): Result<Location>

    /**
     * Converts a latitude and longitude to an address.
     * @return [IOException] failure if the address can't be found due to a network failure.
     * @return [NoAddressException] failure if no address exists for this location.
     */
    suspend fun reverseGeocode(lat: Double, long: Double): Result<String>

    /**
     * Get the last known GPS location of the device. Check that the location is available before calling this method.
     */
    suspend fun getLastLocation(): Location?

    /**
     * Open the directions to the [location] in an external maps app. Returns whether a map app was found and opened.
     */
    fun launchDirectionsToLocation(location: Location, zoom: Float): Boolean

    /**
     * Open the share sheet to share some text.
     */
    fun shareText(text: String, description: String)

    /**
     * Get a list of addresses within the [northeast] and [southwest] bounds that might correspond to the [query].
     */
    suspend fun getMatchingAddresses(
        query: String,
        maxResults: Int,
        southwest: Location,
        northeast: Location
    ): Result<List<String>>
}