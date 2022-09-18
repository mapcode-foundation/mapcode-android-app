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

import com.mapcode.Mapcode
import com.mapcode.MapcodeCodec
import com.mapcode.util.Location
import com.mapcode.util.ShareAdapter
import com.mapcode.util.codeWithNoInternationalTerritory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ViewFavouritesUseCaseImpl @Inject constructor(
    private val dataStore: FavouritesDataStore,
    private val shareAdapter: ShareAdapter
) : ViewFavouritesUseCase {

    override fun getMapcodes(location: Location): List<Mapcode> {
        return MapcodeCodec.encode(location.latitude, location.longitude)
    }

    override fun share(favouriteName: String, mapcode: Mapcode) {
        val mapcodeString = mapcode.codeWithNoInternationalTerritory()

        val text = "$favouriteName. Mapcode: $mapcodeString"

        shareAdapter.share(text = text, description = text)
    }

    override fun getFavourites(): Flow<List<Favourite>> {
        return dataStore.getAll()
            .map { entityList ->
                entityList.map { fromFavouriteEntity(it) }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun setFavouriteName(id: String, name: String) {
        val entity = dataStore.get(id)
        dataStore.update(entity.copy(name = name))
    }

    override fun deleteFavourite(id: String) {
        dataStore.delete(id)
    }

    private fun fromFavouriteEntity(entity: FavouriteEntity): Favourite {
        return Favourite(
            entity.id,
            entity.name,
            Location(entity.latitude, entity.longitude)
        )
    }
}

interface ViewFavouritesUseCase {
    fun getMapcodes(location: Location): List<Mapcode>
    fun share(favouriteName: String, mapcode: Mapcode)

    fun getFavourites(): Flow<List<Favourite>>

    /**
     * Set the name of a favourite.
     */
    suspend fun setFavouriteName(id: String, name: String)

    fun deleteFavourite(id: String)
}