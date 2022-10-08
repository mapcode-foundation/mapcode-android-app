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

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.mapcode.FakePreferenceRepository
import com.mapcode.TestDispatcherProvider
import com.mapcode.data.Keys
import com.mapcode.util.Location
import com.mapcode.util.ShareAdapter
import com.ramcosta.composedestinations.result.ResultBackNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakePreferenceRepository: FakePreferenceRepository
    private lateinit var mockShareAdapter: ShareAdapter

    private lateinit var viewModel: FavouritesViewModel
    private lateinit var mockBackNavigator: ResultBackNavigator<Location>

    private val testDispatcher = StandardTestDispatcher()
    private val coroutineScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        fakePreferenceRepository = FakePreferenceRepository()

        val dataStore = FavouritesDataStoreImpl(
            coroutineScope,
            TestDispatcherProvider(testDispatcher),
            fakePreferenceRepository
        )

        mockShareAdapter = mock()
        mockBackNavigator = mock()

        viewModel =
            FavouritesViewModel(useCase = ViewFavouritesUseCaseImpl(dataStore, mockShareAdapter))
    }

    @Test
    fun list_favourites() = runTest(testDispatcher) {
        setFavourites(
            FavouriteEntity("0", "Favourite 1", 0.0, 0.0),
            FavouriteEntity("1", "Favourite 2", 0.0, 0.0)
        )
        advanceUntilIdle()
        setContent()

        composeTestRule.onNodeWithText("Favourite 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Favourite 2").assertIsDisplayed()
    }

    @Test
    fun show_dialog_to_rename_when_clicking_edit() = runTest(testDispatcher) {
        setFavourites(FavouriteEntity("0", "Favourite 1", 0.0, 0.0))
        advanceUntilIdle()
        setContent()

        composeTestRule.onNodeWithContentDescription("Rename location").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rename location").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Favourite 1").onLast().assertIsDisplayed()
    }

    @Test
    fun rename_favourite() = runTest(testDispatcher) {
        setFavourites(FavouriteEntity("0", "Favourite 1", 0.0, 0.0))
        advanceUntilIdle()
        setContent()

        composeTestRule.onNodeWithContentDescription("Rename location").performClick()

        composeTestRule.onNodeWithText("Name").apply {
            performTextClearance()
            performTextInput("New favourite")
        }

        composeTestRule.onNodeWithText("Save").performClick()

        advanceUntilIdle()

        getFavourites().also {
            assertThat(it).containsExactly(FavouriteEntity("0", "New favourite", 0.0, 0.0))
        }
    }

    @Test
    fun show_confirmation_dialog_when_clicking_delete() = runTest(testDispatcher) {
        setFavourites(FavouriteEntity("0", "Favourite 1", 0.0, 0.0))
        advanceUntilIdle()
        setContent()

        composeTestRule.onNodeWithContentDescription("Delete location").performClick()

        composeTestRule.onNodeWithText("Delete location").assertIsDisplayed()
        composeTestRule.onNodeWithText("Are you sure you want to delete Favourite 1?")
            .assertIsDisplayed()
    }

    @Test
    fun delete_favourite_when_confirming_dialog() = runTest(testDispatcher) {
        setFavourites(FavouriteEntity("0", "Favourite 1", 0.0, 0.0))
        advanceUntilIdle()
        setContent()

        composeTestRule.onNodeWithContentDescription("Delete location").performClick()
        composeTestRule.onNodeWithText("Yes").performClick()

        advanceUntilIdle()
        assertThat(getFavourites()).isEmpty()
    }

    @Test
    fun dismiss_dialog_when_renaming_favourite() = runTest(testDispatcher) {
        setFavourites(FavouriteEntity("0", "Favourite 1", 0.0, 0.0))
        advanceUntilIdle()
        setContent()

        composeTestRule.onNodeWithContentDescription("Rename location").performClick()
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rename location").assertDoesNotExist()
    }

    @Test
    fun dismiss_dialog_when_deleting_favourite() = runTest(testDispatcher) {
        setFavourites(FavouriteEntity("0", "Favourite 1", 0.0, 0.0))
        advanceUntilIdle()
        setContent()

        composeTestRule.onNodeWithContentDescription("Delete location").performClick()
        composeTestRule.onNodeWithText("Yes").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Delete location").assertDoesNotExist()
    }

    @Test
    fun share_favourite_when_clicking_share() = runTest(testDispatcher) {
        setFavourites(FavouriteEntity("0", "Favourite 1", 0.0, 0.0))
        advanceUntilIdle()
        setContent()

        composeTestRule.onNodeWithContentDescription("Share location").performClick()
        composeTestRule.waitForIdle()

        val text = "Favourite 1. Mapcode: HHHHC.X0KG"
        verify(mockShareAdapter).share(text, text)
    }

    @Test
    fun go_back_when_clicking_back() {
        setContent()

        composeTestRule.onNodeWithContentDescription("Go back").performClick()

        verify(mockBackNavigator).navigateBack()
    }

    @Test
    fun return_location_result_when_clicking_favourite() = runTest(testDispatcher) {
        setFavourites(FavouriteEntity("0", "Favourite 1", 0.0, 0.0))
        advanceUntilIdle()
        setContent()

        composeTestRule.onNodeWithText("Favourite 1").performClick()
        verify(mockBackNavigator).navigateBack(Location(0.0, 0.0))
    }

    private fun setFavourites(vararg favourite: FavouriteEntity) {
        favourite
            .map { Json.encodeToString(it) }
            .toSet()
            .also { fakePreferenceRepository.set(Keys.favourites, it) }
    }

    private suspend fun getFavourites(): List<FavouriteEntity> {
        return fakePreferenceRepository
            .get(Keys.favourites)
            .first()
            ?.toList()
            ?.map { Json.decodeFromString(it) }
            ?: emptyList()
    }

    private fun setContent() {
        composeTestRule.setContent {
            FavouritesScreen(viewModel = viewModel, resultBackNavigator = mockBackNavigator)
        }
    }
}