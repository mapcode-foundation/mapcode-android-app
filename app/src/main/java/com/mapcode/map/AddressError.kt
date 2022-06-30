package com.mapcode.map

/**
 * Created by sds100 on 03/06/2022.
 */
sealed class AddressError {
    object None : AddressError()

    /**
     * Show that the address that the user searched can't be found.
     */
    data class UnknownAddress(val addressQuery: String) : AddressError()
}