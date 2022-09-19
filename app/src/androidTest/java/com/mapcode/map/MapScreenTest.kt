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

import android.Manifest
import android.os.Build
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.test.rule.GrantPermissionRule
import assertk.assertThat
import assertk.assertions.index
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.mapcode.Mapcode
import com.mapcode.Territory
import com.mapcode.favourites.Favourite
import com.mapcode.util.Location
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

class MapScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private lateinit var mockDestinationsNavigator: DestinationsNavigator

    private lateinit var useCase: FakeShowMapcodeUseCase
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        useCase = FakeShowMapcodeUseCase()
        mockDestinationsNavigator = mock()
        viewModel = MapViewModel(useCase)
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_header() {
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 0f)

        composeTestRule.onNodeWithText("Mapcode").performClick()

        assertThat(useCase.clipboard).isEqualTo("AB.XY")
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_mapcode_code() {
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 0f)

        composeTestRule.onNodeWithText("AB.XY").performClick()

        assertThat(useCase.clipboard).isEqualTo("AB.XY")
    }

    @Test
    fun show_snackbar_when_copying_mapcode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return
        }

        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        viewModel.onCameraMoved(0.0, 0.0, 0f)

        setMapScreenAsContent()

        composeTestRule.onNodeWithText("AB.XY").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Copied to clipboard.").assertIsDisplayed()
    }

    @Test
    fun show_warning_if_no_internet() {
        useCase.hasInternetConnection = false

        setMapScreenAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 0f)

        composeTestRule.onNodeWithText("No internet?").assertIsDisplayed()
    }

    @Test
    fun show_warning_if_no_address_exists_for_location() {
        setMapScreenAsContent()

        useCase.knownLocations.clear()
        viewModel.onCameraMoved(0.0, 0.0, 0f)

        composeTestRule.onNodeWithText("No address found").assertIsDisplayed()
    }

    @Test
    fun show_error_if_unknown_address_query() {
        useCase.knownLocations.clear()

        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Enter address or mapcode").apply {
            performTextClearance()
            performTextInput("ff")
        }

        composeTestRule.onNodeWithText("ff").performImeAction()

        composeTestRule.onNodeWithText("Cannot find: ff").assertIsDisplayed()
    }

    @Test
    fun update_camera_after_searching_known_address() {
        useCase.knownLocations.add(
            FakeLocation(
                3.0,
                2.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Enter address or mapcode").apply {
            performTextClearance()
            performTextInput("Street, City")
            performImeAction()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("3.0000000").assertExists()
        composeTestRule.onNodeWithText("2.0000000").assertExists()
    }

    @Test
    fun update_address_after_searching_known_address() {
        useCase.knownLocations.add(
            FakeLocation(
                3.0,
                2.0,
                addresses = listOf("Street, City, Country"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Enter address or mapcode").apply {
            performTextClearance()
            performTextInput("Street, City, Country")
            performImeAction()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Street, City, Country").assertIsDisplayed()
        composeTestRule.onNodeWithText("City, Country").assertIsDisplayed()
    }

    @Test
    fun clear_address_if_press_clear_button_in_text_field() {
        setMapScreenAsContent()

        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )
        viewModel.onCameraMoved(0.0, 0.0, 0f) //fill address field with something

        composeTestRule.onNodeWithContentDescription("Clear address").performClick()

        composeTestRule.onNodeWithText("Enter address or mapcode")
            .assert(SemanticsMatcher.expectValue(SemanticsPropertyKey("EditableText"), null))
    }

    @Test
    fun hide_clear_button_if_empty_address() {
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Enter address or mapcode").performTextClearance()

        composeTestRule.onNodeWithContentDescription("Clear address").assertDoesNotExist()
    }

    @Test
    fun focus_address_text_field_when_click_clear_button() {
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )
        viewModel.onCameraMoved(0.0, 0.0, 0f) //fill address field with something

        setMapScreenAsContent()

        composeTestRule.onNodeWithContentDescription("Clear address").performClick()

        composeTestRule.onNodeWithText("Enter address or mapcode").assertIsFocused()
    }

    @Test
    fun show_last_2_parts_of_address_if_address_exists() {
        setMapScreenAsContent()

        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = listOf("Street, City, Country"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, 0f)

        composeTestRule.onNodeWithText("City, Country").assertIsDisplayed()
    }

    @Test
    fun update_information_when_map_moves() {
        setMapScreenAsContent()

        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = listOf("Street, City, Country"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.NLD))
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, 0f)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("NLD AB.XY").assertIsDisplayed()

        composeTestRule.onNodeWithText("Netherlands").assertIsDisplayed()

        composeTestRule.onNodeWithText("Territory 1 of 1").assertIsDisplayed()

        composeTestRule.onNodeWithText("Street, City, Country").assertIsDisplayed()

        composeTestRule.onNodeWithText("City, Country").assertIsDisplayed()
    }

    @Test
    fun update_address_after_searching_latitude() {
        useCase.knownLocations.add(
            FakeLocation(
                3.0,
                0.0,
                addresses = listOf("Street, City, Country"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Latitude (Y)").apply {
            performTextClearance()
            performTextInput("3")
            performImeAction()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Street, City, Country").assertIsDisplayed()

        composeTestRule.onNodeWithText("City, Country").assertIsDisplayed()
    }

    @Test
    fun update_address_after_searching_longitude() {
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                2.0,
                addresses = listOf("Street, City, Country"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Longitude (X)").apply {
            performTextClearance()
            performTextInput("2.0")
            performImeAction()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Street, City, Country").assertIsDisplayed()

        composeTestRule.onNodeWithText("City, Country").assertIsDisplayed()
    }

    @Test
    fun update_camera_after_searching_latitude() {
        useCase.knownLocations.add(
            FakeLocation(
                3.0,
                2.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Latitude (Y)").apply {
            performTextClearance()
            performTextInput("3.0")
            performImeAction()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("3.0000000").assertExists()
        composeTestRule.onNodeWithText("0.0000000").assertExists()
    }

    @Test
    fun update_camera_after_searching_longitude() {
        useCase.knownLocations.add(
            FakeLocation(
                3.0,
                2.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Longitude (X)").apply {
            performTextClearance()
            performTextInput("2.0")
            performImeAction()
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("0.0000000").assertExists()
        composeTestRule.onNodeWithText("2.0000000").assertExists()
    }

    @Test
    fun show_snackbar_if_fail_to_get_current_location() {
        useCase.currentLocation = null
        setMapScreenAsContent()

        composeTestRule.onNodeWithContentDescription("Go to my location").performClick()

        composeTestRule.onNodeWithText("Can't find location. Is your GPS turned on?")
            .assertIsDisplayed()
    }

    @Test
    fun show_error_snackbar_when_no_external_app_to_view_map() {
        useCase.isMapsAppInstalled = false
        setMapScreenAsContent()

        composeTestRule.onNodeWithContentDescription("View location in maps app").performClick()

        composeTestRule.onNodeWithText("You have no map app installed to open this in.")
            .assertIsDisplayed()
    }

    @Test
    fun share_mapcode_when_clicking_share_button() {
        useCase.knownLocations.add(
            FakeLocation(
                3.0,
                2.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )
        setMapScreenAsContent()

        viewModel.onCameraMoved(3.0, 2.0, 1f)

        composeTestRule.onNodeWithContentDescription("Share mapcode").performClick()

        assertThat(useCase.sharedMapcode).isEqualTo(Mapcode("AB.XY", Territory.AAA))
    }

    @Test
    fun show_error_if_latitude_is_not_a_number() {
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Latitude (Y)").apply {
            performTextClearance()
            performTextInput("a")
        }

        composeTestRule.onNodeWithText("Must be a number!").assertIsDisplayed()
    }

    @Test
    fun show_error_if_longitude_is_not_a_number() {
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Longitude (X)").apply {
            performTextClearance()
            performTextInput("a")
        }

        composeTestRule.onNodeWithText("Must be a number!").assertIsDisplayed()
    }

    @Test
    fun do_not_show_error_if_latitude_is_empty() {
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Latitude (Y)").apply {
            performTextClearance()
        }

        composeTestRule.onNodeWithText("Must be a number!").assertDoesNotExist()
    }

    @Test
    fun do_not_show_error_if_longitude_is_empty() {
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Longitude (X)").apply {
            performTextClearance()
        }

        composeTestRule.onNodeWithText("Must be a number!").assertDoesNotExist()
    }

    @Test
    fun do_not_submit_latitude_if_invalid() {
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Latitude (Y)").apply {
            performTextClearance()
            performTextInput("a")
            performImeAction()
        }

        composeTestRule.onNodeWithText("Latitude (Y)").assertIsFocused()
        composeTestRule.onNodeWithText("Must be a number!").assertIsDisplayed()
    }

    @Test
    fun submit_latitude_if_empty() {
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Latitude (Y)").apply {
            performTextClearance()
            performImeAction()
        }

        composeTestRule.onNodeWithText("Latitude (Y)").assertIsNotFocused()
        composeTestRule.onNodeWithText("Must be a number!").assertDoesNotExist()
    }

    @Test
    fun do_not_submit_longitude_if_invalid() {
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Longitude (X)").apply {
            performTextClearance()
            performTextInput("a")
            performImeAction()
        }

        composeTestRule.onNodeWithText("Longitude (X)").assertIsFocused()
        composeTestRule.onNodeWithText("Must be a number!").assertIsDisplayed()
    }

    @Test
    fun submit_longitude_if_empty() {
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Longitude (X)").apply {
            performTextClearance()
            performImeAction()
        }

        composeTestRule.onNodeWithText("Longitude (X)").assertIsNotFocused()
        composeTestRule.onNodeWithText("Must be a number!").assertDoesNotExist()
    }

    @Test
    fun hide_dropdown_if_no_matching_addresses() {
        useCase.matchingAddresses["address"] = emptyList()

        setMapScreenAsContent()
        composeTestRule.onNodeWithText("Enter address or mapcode").apply {
            performTextClearance()
            performTextInput("address")
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("address_dropdown").assertDoesNotExist()
    }

    @Test
    fun show_matching_addresses_in_dropdown_when_typing() {
        useCase.matchingAddresses["address"] = listOf("Street 1", "Street 2")

        setMapScreenAsContent()
        composeTestRule.onNodeWithText("Enter address or mapcode").apply {
            performTextClearance()
            performTextInput("address")
        }

        composeTestRule.waitUntil(2000) {
            viewModel.uiState.value.addressUi.matchingAddresses.isNotEmpty()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("address_dropdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("Street 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Street 2").assertIsDisplayed()
    }

    @Test
    fun hide_dropdown_if_not_typing_address() {
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Enter address or mapcode")

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("address_dropdown").assertDoesNotExist()
    }

    @Test
    fun clear_address_focus_when_submitting_address_query() {
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = listOf("address"),
                mapcodes = emptyList()
            )
        )
        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Enter address or mapcode").apply {
            performTextClearance()
            performTextInput("address")
            performImeAction()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Enter address or mapcode").assertIsNotFocused()
    }

    @Test
    fun clear_address_focus_when_choosing_address_in_dropdown() {
        useCase.matchingAddresses["address"] = listOf("Street 1")

        setMapScreenAsContent()

        composeTestRule.onNodeWithText("Enter address or mapcode").apply {
            performTextClearance()
            performTextInput("address")
        }

        composeTestRule.waitUntil(2000) {
            viewModel.uiState.value.addressUi.matchingAddresses.isNotEmpty()
        }

        composeTestRule.onNodeWithText("Street 1").performClick()
        composeTestRule.onNodeWithText("Enter address or mapcode").assertIsNotFocused()
    }

    @Test
    fun do_not_focus_address_when_opening_app() {
        setMapScreenAsContent()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Enter address or mapcode").assertIsNotFocused()
    }

    @Test
    fun select_latitude_text_when_focussing() {
        useCase.knownLocations.add(
            FakeLocation(0.0, 0.0, emptyList(), mapcodes = emptyList())
        )
        setMapScreenAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 1f)

        composeTestRule.onNodeWithText("Latitude (Y)").apply {
            performClick()
            assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(0, 9)
                )
            )
        }
    }

    @Test
    fun select_longitude_text_when_focussing() {
        useCase.knownLocations.add(
            FakeLocation(0.0, 0.0, emptyList(), mapcodes = emptyList())
        )
        setMapScreenAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 1f)

        composeTestRule.onNodeWithText("Longitude (X)").apply {
            performClick()
            assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(0, 9)
                )
            )
        }
    }

    @Test
    fun copy_location_to_clipboard_when_tapping_latitude_copy_button() {
        setMapScreenAsContent()
        viewModel.onCameraMoved(1.0, 2.0, 1f)

        composeTestRule.onAllNodes(hasContentDescription("Copy location")).onFirst().performClick()
        assertThat(useCase.clipboard).isEqualTo("1,2")
    }

    @Test
    fun copy_location_to_clipboard_when_tapping_longitude_copy_button() {
        setMapScreenAsContent()
        viewModel.onCameraMoved(1.0, 2.0, 1f)

        composeTestRule.onAllNodes(hasContentDescription("Copy location")).onLast().performClick()
        assertThat(useCase.clipboard).isEqualTo("1,2")
    }

    @Test
    fun show_add_favourites_dialog_when_clicking_add_favourites() {
        setMapScreenAsContent()
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = listOf("address"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.NLD))
            )
        )

        viewModel.onCameraMoved(0.0, 0.0, 1f)

        composeTestRule.onNodeWithContentDescription("Save location").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Mapcode: NLD AB.XY").assertIsDisplayed()
        composeTestRule.onNodeWithText("Name").assertIsDisplayed()
    }

    @Test
    fun save_favourites_when_clicking_save_name() {
        setMapScreenAsContent()
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = listOf("address"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.NLD))
            )
        )

        viewModel.onCameraMoved(0.0, 0.0, 1f)

        composeTestRule.onNodeWithContentDescription("Save location").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Save").performClick()

        composeTestRule.waitUntil(2000) {
            useCase.favourites.value.isNotEmpty()
        }

        assertThat(useCase.favourites.value).index(0).prop(Favourite::name).isEqualTo("address")
        assertThat(useCase.favourites.value).index(0).prop(Favourite::location)
            .isEqualTo(Location(0.0, 0.0))

        composeTestRule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun favourite_button_deletes_favourite_when_location_is_saved() {
        setMapScreenAsContent()

        useCase.favourites.value =
            listOf(Favourite(id = "0", location = Location(0.0, 0.0), name = "fav"))
        viewModel.onCameraMoved(0.0, 0.0, 1f)

        composeTestRule.onNodeWithContentDescription("Delete saved location").performClick()
        assertThat(useCase.favourites.value).isEmpty()
    }

    private fun setMapScreenAsContent() {
        composeTestRule.setContent {
            MapScreen(
                viewModel = viewModel,
                renderGoogleMaps = false,
                navigator = mockDestinationsNavigator,
                resultRecipient = mock()
            )
        }
    }
}