package com.mapcode.map

/**
 * Created by sds100 on 01/06/2022.
 */

/**
 * UI state for a helper message that isn't an error.
 */
sealed class AddressHelper {
    /**
     * Show no error message.
     */
    object None : AddressHelper()

    /**
     * Show that there is no internet connection.
     */
    object NoInternet : AddressHelper()

    /**
     * This location has no address.
     */
    object NoAddress : AddressHelper()

    /**
     * The last 2 parts of the address.
     */
    data class Location(val location: String) : AddressHelper()
}