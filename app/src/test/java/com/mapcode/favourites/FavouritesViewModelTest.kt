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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import assertk.assertThat
import assertk.assertions.containsExactly
import com.mapcode.FakeLocation
import com.mapcode.Mapcode
import com.mapcode.Territory
import com.mapcode.util.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeUseCase: FakeViewFavouritesUseCase
    private lateinit var viewModel: FavouritesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakeUseCase = FakeViewFavouritesUseCase()
        viewModel = FavouritesViewModel(fakeUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `do not show territory for international mapcode`() = runTest {
        fakeUseCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        fakeUseCase.favourites.value = listOf(
            Favourite(id = "0", name = "name", location = Location(0.0, 0.0))
        )

        advanceUntilIdle()

        assertThat(viewModel.favourites.value).containsExactly(
            FavouriteListItem(
                id = "0",
                name = "name",
                mapcode = "AB.XY"
            )
        )
    }

    @Test
    fun `show territory for non-international mapcode`() = runTest {
        fakeUseCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.NLD))
            )
        )

        fakeUseCase.favourites.value = listOf(
            Favourite(id = "0", name = "name", location = Location(0.0, 0.0))
        )

        advanceUntilIdle()

        assertThat(viewModel.favourites.value).containsExactly(
            FavouriteListItem(
                id = "0",
                name = "name",
                mapcode = "NLD AB.XY"
            )
        )
    }
}