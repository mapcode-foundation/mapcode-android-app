package com.mapcode.map

/**
 * Created by sds100 on 01/06/2022.
 */
enum class AddressError {
    /**
     * Show no error message.
     */
    None,

    /**
     * Show that there is no internet connection.
     */
    NoInternet,

    /**
     * Show that the address that the user searched can't be found.
     */
    CantFindAddress,

    /**
     * This location has no address.
     */
    NoAddress
}