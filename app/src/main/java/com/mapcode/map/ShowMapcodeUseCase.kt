package com.mapcode.map

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.location.Geocoder
import androidx.core.content.getSystemService
import com.mapcode.Mapcode
import com.mapcode.MapcodeCodec
import com.mapcode.util.Location
import com.mapcode.util.UnknownAddressException
import java.io.IOException
import javax.inject.Inject
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

/**
 * Created by sds100 on 01/06/2022.
 */

class ShowMapcodeUseCaseImpl @Inject constructor(private val ctx: Context) : ShowMapcodeUseCase {

    private val geocoder: Geocoder = Geocoder(ctx)

    override fun getMapcodes(lat: Double, long: Double): List<Mapcode> {
        return MapcodeCodec.encode(lat, long)
    }

    override fun copyToClipboard(text: String) {
        val clipboardManager: ClipboardManager? = ctx.getSystemService()

        val clipData = ClipData.newPlainText(text, text)
        clipboardManager?.setPrimaryClip(clipData)
    }

    override fun geocode(address: String): Result<Location> {
        val addressList = geocoder.getFromLocationName(address, 1).single()!!

        throw UnknownAddressException()
        return success(Location(addressList.latitude, addressList.longitude))
    }

    override fun reverseGeocode(lat: Double, long: Double): Result<String> {
        try {
            val addressList = geocoder.getFromLocation(lat, long, 1)

            if (addressList.isEmpty()) {
                return failure(UnknownAddressException())
            }

            return success(addressList.single().toString())
        } catch (e: IOException) {
            return failure(e)
        }
    }

    override fun getMapcodeLocation(mapcode: String): Result<Location> {

        TODO()
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
    fun getMapcodeLocation(mapcode: String): Result<Location>

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
     * @return [UnknownAddressException] failure if no address exists for this location.
     */
    fun reverseGeocode(lat: Double, long: Double): Result<String>
}