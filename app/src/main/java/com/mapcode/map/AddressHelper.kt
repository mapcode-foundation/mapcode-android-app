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