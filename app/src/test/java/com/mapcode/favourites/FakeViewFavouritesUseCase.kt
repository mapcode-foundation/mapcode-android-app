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

package com.mapcode.favourites

import com.mapcode.FakeLocation
import com.mapcode.Mapcode
import com.mapcode.util.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeViewFavouritesUseCase : ViewFavouritesUseCase {
    val favourites: MutableStateFlow<List<Favourite>> = MutableStateFlow(emptyList())
    val knownLocations: MutableList<FakeLocation> = mutableListOf()

    override fun getMapcodes(location: Location): List<Mapcode> {
        return knownLocations
            .find { it.latitude == location.latitude && it.longitude == location.longitude }
            ?.mapcodes ?: emptyList()
    }

    override fun share(favouriteName: String, mapcode: Mapcode) {
    }

    override fun getFavourites(): Flow<List<Favourite>> {
        return favourites
    }

    override suspend fun setFavouriteName(id: String, name: String) {
    }

    override fun deleteFavourite(id: String) {
    }
}