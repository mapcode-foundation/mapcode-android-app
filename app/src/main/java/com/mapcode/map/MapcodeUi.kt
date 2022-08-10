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

data class MapcodeUi(
    val code: String,
    /**
     * The short name of the territory. E.g NLD.
     */
    val territoryShortName: String?,

    /**
     * The full name of the territory. E.g Netherlands.
     */
    val territoryFullName: String,

    /**
     * The number of the territory in the list. E.g 1 out of 3.
     */
    val number: Int,

    /**
     * How many different territories have mapcodes for this location.
     */
    val count: Int
)