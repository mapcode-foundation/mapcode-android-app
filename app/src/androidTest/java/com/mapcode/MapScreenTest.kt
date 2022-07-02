package com.mapcode

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.mapcode.map.MapScreen
import com.mapcode.map.MapViewModel
import com.mapcode.util.Location
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
        viewModel.onCameraMoved(0.0, 0.0)

        composeTestRule
            .onNodeWithText("Mapcode")
            .performClick()

        assertThat(useCase.clipboard).isEqualTo("AAA AB.XY")
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
        viewModel.onCameraMoved(0.0, 0.0)

        composeTestRule
            .onNodeWithText("AB.XY")
            .performClick()

        assertThat(useCase.clipboard).isEqualTo("AAA AB.XY")
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_mapcode_territory() {
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()
        viewModel.onCameraMoved(0.0, 0.0)

        composeTestRule
            .onNodeWithText("AAA")
            .performClick()

        assertThat(useCase.clipboard).isEqualTo("AAA AB.XY")
    }

    @Test
    fun show_snack_bar_when_copying_mapcode() {
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        viewModel.onCameraMoved(0.0, 0.0)

        setMapScreenAsContent()

        composeTestRule
            .onNodeWithText("AAA")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Copied to clipboard.")
            .assertIsDisplayed()
    }

    @Test
    fun show_warning_if_no_internet() {
        useCase.hasInternetConnection = false

        setMapScreenAsContent()
        viewModel.onCameraMoved(0.0, 0.0)

        composeTestRule
            .onNodeWithText("No internet?")
            .assertIsDisplayed()
    }

    @Test
    fun show_warning_if_no_address_exists_for_location() {
        setMapScreenAsContent()

        useCase.knownLocations.clear()
        viewModel.onCameraMoved(0.0, 0.0)

        composeTestRule
            .onNodeWithText("No address found")
            .assertIsDisplayed()
    }

    @Test
    fun show_error_if_unknown_address_query() {
        useCase.knownLocations.clear()

        setMapScreenAsContent()

        composeTestRule
            .onNodeWithText("Enter address or mapcode").apply {
                performTextClearance()
                performTextInput("ff")
            }

        composeTestRule
            .onNodeWithText("ff")
            .performImeAction()

        composeTestRule
            .onNodeWithText("Cannot find: ff")
            .assertIsDisplayed()
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

        composeTestRule
            .onNodeWithText("Enter address or mapcode").apply {
                performTextClearance()
                performTextInput("Street, City")
                performImeAction()
            }

        composeTestRule.waitForIdle()

        assertThat(viewModel.location.value).isEqualTo(Location(3.0, 2.0))
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

        composeTestRule
            .onNodeWithText("Enter address or mapcode").apply {
                performTextClearance()
                performTextInput("Street, City, Country")
                performImeAction()
            }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Street, City, Country")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("City, Country")
            .assertIsDisplayed()
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
        viewModel.onCameraMoved(0.0, 0.0) //fill address field with something

        composeTestRule
            .onNodeWithContentDescription("Clear address")
            .performClick()

        composeTestRule
            .onNodeWithText("Enter address or mapcode")
            .assert(SemanticsMatcher.expectValue(SemanticsPropertyKey("EditableText"), null))
    }

    @Test
    fun hide_clear_button_if_empty_address() {
        setMapScreenAsContent()

        composeTestRule
            .onNodeWithText("Enter address or mapcode")
            .performTextClearance()

        composeTestRule
            .onNodeWithContentDescription("Clear address")
            .assertDoesNotExist()
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
        viewModel.onCameraMoved(0.0, 0.0) //fill address field with something

        setMapScreenAsContent()

        composeTestRule
            .onNodeWithContentDescription("Clear address")
            .performClick()

        composeTestRule
            .onNodeWithText("Enter address or mapcode")
            .assertIsFocused()
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

        viewModel.onCameraMoved(1.0, 1.0)

        composeTestRule
            .onNodeWithText("City, Country")
            .assertIsDisplayed()
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

        viewModel.onCameraMoved(1.0, 1.0)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("AB.XY")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("NLD")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Netherlands")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Territory 1 of 1")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Street, City, Country")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("City, Country")
            .assertIsDisplayed()
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

        composeTestRule
            .onNodeWithText("Latitude (Y)").apply {
                performTextClearance()
                performTextInput("3")
                performImeAction()
            }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Street, City, Country")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("City, Country")
            .assertIsDisplayed()
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

        composeTestRule
            .onNodeWithText("Longitude (X)").apply {
                performTextClearance()
                performTextInput("2.0")
                performImeAction()
            }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Street, City, Country")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("City, Country")
            .assertIsDisplayed()
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

        composeTestRule
            .onNodeWithText("Latitude (Y)").apply {
                performTextClearance()
                performTextInput("3.0")
                performImeAction()
            }

        composeTestRule.waitForIdle()

        assertThat(viewModel.location.value).isEqualTo(Location(3.0, 0.0))
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

        composeTestRule
            .onNodeWithText("Longitude (X)").apply {
                performTextClearance()
                performTextInput("2.0")
                performImeAction()
            }

        composeTestRule.waitForIdle()

        assertThat(viewModel.location.value).isEqualTo(Location(0.0, 2.0))
    }

    private fun setMapScreenAsContent() {
        composeTestRule.setContent {
            MapScreen(viewModel = viewModel, showMap = false)
        }
    }
}