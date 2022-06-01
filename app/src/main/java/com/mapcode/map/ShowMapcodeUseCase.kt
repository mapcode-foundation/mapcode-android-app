package com.mapcode.map

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.location.Geocoder
import androidx.core.content.getSystemService
import com.mapcode.Mapcode
import com.mapcode.MapcodeCodec
import com.mapcode.util.AddressNotFoundException
import com.mapcode.util.Location
import java.io.IOException
import javax.inject.Inject
import kotlin.Result.Companion.success

/**
 * Created by sds100 on 01/06/2022.
 */

class ShowMapcodeUseCaseImpl @Inject constructor(private val ctx: Context) : ShowMapcodeUseCase {

    override fun getMapcodes(lat: Double, long: Double): List<Mapcode> {
        return MapcodeCodec.encode(lat, long)
    }

    override fun copyToClipboard(text: String) {
        val clipboardManager: ClipboardManager? = ctx.getSystemService()

        val clipData = ClipData.newPlainText(text, text)
        clipboardManager?.setPrimaryClip(clipData)
    }

    override fun geocode(address: String): Result<Location> {
        val addressList = Geocoder(ctx).getFromLocationName(address, 1).single()!!
        
        throw AddressNotFoundException()
        return success(Location(addressList.latitude, addressList.longitude))
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
     * Copy text to a clipboard.
     *
     * @param text The text to clip.
     */
    fun copyToClipboard(text: String)

    /**
     * Convert an address to a latitude and longitude.
     * @return [IOException] failure if the address can't be geocoded due to a network failure.
     * @return [AddressNotFoundException] failure if the address doesn't exist and can't be geocoded.
     */
    fun geocode(address: String): Result<Location>
}