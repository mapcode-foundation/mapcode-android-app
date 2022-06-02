package com.mapcode.map

/**
 * Created by sds100 on 01/06/2022.
 */
sealed class AddressError {
    /**
     * Show no error message.
     */
    object None : AddressError()

    /**
     * Show that there is no internet connection.
     */
    object NoInternet : AddressError()

    /**
     * Show that the address that the user searched can't be found.
     */
    data class UnknownAddress(val addressQuery: String) : AddressError()

    /**
     * This location has no address.
     */
    object NoAddress : AddressError()
}