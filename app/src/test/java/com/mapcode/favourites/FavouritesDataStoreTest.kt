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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isFailure
import com.mapcode.FakePreferenceRepository
import com.mapcode.TestDispatcherProvider
import com.mapcode.data.Keys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class FavouritesDataStoreTest {

    private val testDispatcher = StandardTestDispatcher()
    private val coroutineScope = TestScope(testDispatcher)

    private lateinit var fakeRepository: FakePreferenceRepository
    private lateinit var dataStore: FavouritesDataStoreImpl

    @Before
    fun setUp() {
        fakeRepository = FakePreferenceRepository()

        dataStore = FavouritesDataStoreImpl(
            dispatchers = TestDispatcherProvider(testDispatcher),
            repository = fakeRepository,
            coroutineScope = coroutineScope
        )
    }

    @Test
    fun `save favourite if favourites does not exist in repository`() = runTest(testDispatcher) {
        fakeRepository.set(Keys.favourites, null)
        val idResult = dataStore.create(name = "name", latitude = 0.0, longitude = 0.0)
        advanceUntilIdle()

        assertThat(dataStore.getAll().first()).containsExactly(
            FavouriteEntity(
                id = idResult.getOrThrow(),
                name = "name",
                latitude = 0.0,
                longitude = 0.0
            )
        )
    }

    @Test
    fun `replace favourite when saving favourite with id that exists`() = runTest(testDispatcher) {
        val favourite = FavouriteEntity(
            id = "0",
            name = "name",
            latitude = 0.0, longitude = 0.0
        )

        fakeRepository.set(Keys.favourites, setOf(Json.encodeToString(favourite)))

        dataStore.update(
            FavouriteEntity(
                id = "0",
                name = "new name",
                latitude = 0.0, longitude = 0.0
            )
        )

        advanceUntilIdle()

        assertThat(dataStore.getAll().first()).containsExactly(
            FavouriteEntity(
                id = "0",
                name = "new name",
                latitude = 0.0, longitude = 0.0
            )
        )
    }

    @Test
    fun `add favourite when saving new favourite`() = runTest(testDispatcher) {
        val favourite = FavouriteEntity(
            id = "0",
            name = "name1",
            latitude = 0.0, longitude = 0.0
        )

        fakeRepository.set(Keys.favourites, setOf(Json.encodeToString(favourite)))
        advanceUntilIdle()

        val newFavIdResult = dataStore.create(name = "name2", latitude = 1.0, longitude = 1.0)
        advanceUntilIdle()

        assertThat(dataStore.getAll().first()).containsExactly(
            FavouriteEntity(
                id = "0",
                name = "name1",
                latitude = 0.0,
                longitude = 0.0
            ),
            FavouriteEntity(
                id = newFavIdResult.getOrThrow(),
                name = "name2",
                latitude = 1.0,
                longitude = 1.0
            )
        )
    }

    @Test
    fun `delete favourite when deleting`() = runTest(testDispatcher) {
        val favourite = FavouriteEntity(
            id = "0",
            name = "name1",
            latitude = 0.0,
            longitude = 0.0
        )

        fakeRepository.set(Keys.favourites, setOf(Json.encodeToString(favourite)))

        dataStore.delete(favourite.id)

        assertThat(dataStore.getAll().first()).isEmpty()
    }

    @Test
    fun `return error if creating a favourite for a location that already has one`() =
        runTest(testDispatcher) {
            val favourite1 = FavouriteEntity(
                id = "0",
                name = "fav1",
                latitude = 0.0,
                longitude = 0.0
            )

            fakeRepository.set(Keys.favourites, setOf(Json.encodeToString(favourite1)))
            advanceUntilIdle()

            val result = dataStore.create(name = "id2", latitude = 0.0, longitude = 0.0)

            assertThat(result).isFailure()
        }
}