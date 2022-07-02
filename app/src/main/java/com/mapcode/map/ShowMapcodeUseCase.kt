package com.mapcode.map

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.location.Geocoder
import androidx.core.content.getSystemService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapcode.Mapcode
import com.mapcode.MapcodeCodec
import com.mapcode.UnknownMapcodeException
import com.mapcode.UnknownPrecisionFormatException
import com.mapcode.util.Location
import com.mapcode.util.NoAddressException
import com.mapcode.util.UnknownAddressException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by sds100 on 01/06/2022.
 */

class ShowMapcodeUseCaseImpl @Inject constructor(@ApplicationContext private val ctx: Context) : ShowMapcodeUseCase {

    private val geocoder: Geocoder = Geocoder(ctx)
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(ctx)

    override fun getMapcodes(lat: Double, long: Double): List<Mapcode> {
        return MapcodeCodec.encode(lat, long)
    }

    override fun copyToClipboard(text: String) {
        val clipboardManager: ClipboardManager? = ctx.getSystemService()

        val clipData = ClipData.newPlainText(text, text)
        clipboardManager?.setPrimaryClip(clipData)
    }

    override fun geocode(address: String): Result<Location> {
        try {
            //sending an empty string to Geocoder API results in IOException even if you have internet access
            //so return the correct exception here.
            if (address.isEmpty()) {
                return failure(UnknownAddressException())
            }

            val matchingAddress = geocoder.getFromLocationName(address, 1).firstOrNull()

            if (matchingAddress == null) {
                return failure(UnknownAddressException())
            } else {
                return success(Location(matchingAddress.latitude, matchingAddress.longitude))
            }
        } catch (e: IOException) {
            return failure(e)
        }
    }

    override fun reverseGeocode(lat: Double, long: Double): Result<String> {
        try {
            val addressList = geocoder.getFromLocation(lat, long, 1)

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
                continuation.resume(null)
            } else {
                continuation.resume(Location(location.latitude, location.longitude))
            }
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
    fun geocode(address: String): Result<Location>

    /**
     * Converts a latitude and longitude to an address.
     * @return [IOException] failure if the address can't be found due to a network failure.
     * @return [NoAddressException] failure if no address exists for this location.
     */
    fun reverseGeocode(lat: Double, long: Double): Result<String>

    /**
     * Get the last known GPS location of the device. Check that the location is available before calling this method.
     */
    suspend fun getLastLocation(): Location?
}