package com.mapcode

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.mapcode.map.MapScreen
import com.mapcode.map.MapViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by sds100 on 01/06/2022.
 */
class MapScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var useCase: FakeShowMapcodeUseCase
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        useCase = FakeShowMapcodeUseCase()
        viewModel = MapViewModel(useCase, FakePreferenceRepository())
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_header() {
        useCase.knownLocations.add(
            FakeLocation(
                0.0, 0.0, addresses = emptyList(), mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 0f)

        composeTestRule.onNodeWithText("Mapcode").performClick()

        assertThat(useCase.clipboard).isEqualTo("AAA AB.XY")
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_mapcode_code() {
        useCase.knownLocations.add(
            FakeLocation(
                0.0, 0.0, addresses = emptyList(), mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 0f)

        composeTestRule.onNodeWithText("AAA AB.XY").performClick()

        assertThat(useCase.clipboard).isEqualTo("AAA AB.XY")
    }

    @Test
    fun show_snack_bar_when_copying_mapcode() {
        useCase.knownLocations.add(
            FakeLocation(
                0.0, 0.0, addresses = emptyList(), mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        viewModel.onCameraMoved(0.0, 0.0, 0f)

        setMapScreenAsContent()

        composeTestRule.onNodeWithText("AAA AB.XY").performClick()

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
                3.0, 2.0, addresses = listOf("Street, City"), mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
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
                0.0, 0.0, addresses = listOf("Street, City"), mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
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
                0.0, 0.0, addresses = listOf("Street, City"), mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
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
                3.0, 2.0, addresses = listOf("Street, City"), mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
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
                3.0, 2.0, addresses = listOf("Street, City"), mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
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
    fun show_snack_bar_if_fail_to_get_current_location() {
        useCase.currentLocation = null
        setMapScreenAsContent()

        composeTestRule.onNodeWithContentDescription("Go to my location").performClick()

        composeTestRule.onNodeWithText("Can't find location. Is your GPS turned on?").assertIsDisplayed()
    }

    @Test
    fun show_error_snackbar_when_no_external_app_to_view_map() {
        useCase.isMapsAppInstalled = false
        setMapScreenAsContent()

        composeTestRule.onNodeWithContentDescription("View location in maps app").performClick()

        composeTestRule.onNodeWithText("You have no map app installed to open this in.").assertIsDisplayed()
    }

    @Test
    fun share_mapcode_when_clicking_share_button() {
        useCase.knownLocations.add(
            FakeLocation(
                3.0, 2.0, addresses = listOf("Street, City"), mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )
        setMapScreenAsContent()

        viewModel.onCameraMoved(3.0, 2.0, 1f)

        composeTestRule.onNodeWithContentDescription("Share mapcode").performClick()

        assertThat(useCase.sharedText).isEqualTo("AAA AB.XY")
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

    private fun setMapScreenAsContent() {
        composeTestRule.setContent {
            MapScreen(viewModel = viewModel, renderGoogleMaps = false)
        }
    }
}