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

import com.mapcode.data.Keys
import com.mapcode.data.PreferenceRepository
import com.mapcode.util.DefaultDispatcherProvider
import com.mapcode.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavouritesDataStoreImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    private val repository: PreferenceRepository,
) : FavouritesDataStore {
    private val favourites: StateFlow<List<FavouriteEntity>> =
        repository.get(Keys.favourites)
            .map { it ?: emptySet() }
            .map { jsonSet ->
                jsonSet.map { Json.decodeFromString<FavouriteEntity>(it) }
            }
            .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    override fun getAll(): Flow<List<FavouriteEntity>> {
        return favourites
    }

    override fun update(entity: FavouriteEntity) {
        coroutineScope.launch(dispatchers.io) {
            favourites.value
                .filter { it.id != entity.id }
                .plus(entity)
                .map { Json.encodeToString(it) }
                .toSet()
                .also { repository.set(Keys.favourites, it) }
        }
    }

    override suspend fun create(name: String, latitude: Double, longitude: Double): String {
        val id = UUID.randomUUID().toString()
        val entity = FavouriteEntity(
            id = id,
            name = name,
            latitude = latitude,
            longitude = longitude
        )

        withContext(dispatchers.io) {
            favourites
                .first()
                .plus(entity)
                .map { Json.encodeToString(it) }
                .toSet()
                .also { repository.set(Keys.favourites, it) }
        }

        return id
    }

    override fun delete(id: String) {
        coroutineScope.launch(dispatchers.io) {
            favourites.value
                .filter { it.id != id }
                .map { Json.encodeToString(it) }
                .toSet()
                .also { repository.set(Keys.favourites, it) }
        }
    }

    override suspend fun get(id: String): FavouriteEntity {
        return favourites.value.single { it.id == id }
    }
}

interface FavouritesDataStore {
    fun getAll(): Flow<List<FavouriteEntity>>
    suspend fun get(id: String): FavouriteEntity
    fun update(entity: FavouriteEntity)

    /**
     * @return the id of the new favourite.
     */
    suspend fun create(name: String, latitude: Double, longitude: Double): String
    fun delete(id: String)
}