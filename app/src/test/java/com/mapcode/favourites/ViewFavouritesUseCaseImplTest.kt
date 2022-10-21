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
import com.mapcode.Territory
import com.mapcode.util.ShareAdapter
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class ViewFavouritesUseCaseImplTest {

    private lateinit var mockShareAdapter: ShareAdapter
    private lateinit var useCase: ViewFavouritesUseCaseImpl

    @Before
    fun setUp() {
        mockShareAdapter = mock()
        useCase = ViewFavouritesUseCaseImpl(
            dataStore = mock(),
            shareAdapter = mockShareAdapter
        )
    }

    @Test
    fun `do not share territory for international mapcode`() {
        useCase.share("Home", Mapcode("AB.XY", Territory.AAA))
        verify(mockShareAdapter).share(
            text = "Home. Mapcode: AB.XY",
            description = "Home. Mapcode: AB.XY"
        )
    }

    @Test
    fun `share territory with non-international mapcode`() {
        useCase.share("Home", Mapcode("AB.XY", Territory.NLD))
        verify(mockShareAdapter).share(
            text = "Home. Mapcode: NLD AB.XY",
            description = "Home. Mapcode: NLD AB.XY"
        )
    }
}