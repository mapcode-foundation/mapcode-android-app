package com.mapcode

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.mapcode.map.MapScreen
import com.mapcode.map.MapViewModel
import com.mapcode.map.MapcodeInfoBox
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

        setMapcodeInfoBoxAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 1f)

        composeTestRule
            .onNodeWithText("Mapcode (tap to copy)")
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

        setMapcodeInfoBoxAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 1f)

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

        setMapcodeInfoBoxAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 1f)

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

        setMapScreenAsContent()

        composeTestRule
            .onNodeWithText("AAA")
            .performClick()

        composeTestRule
            .onNodeWithText("Copied to clipboard.")
            .assertIsDisplayed()
    }

    @Test
    fun show_warning_if_no_internet() {
        useCase.hasInternetConnection = false

        setMapcodeInfoBoxAsContent()
        viewModel.onCameraMoved(0.0, 0.0, 1f)

        composeTestRule
            .onNodeWithText("No internet?")
            .assertIsDisplayed()
    }

    @Test
    fun show_warning_if_no_address_exists_for_location() {
        setMapcodeInfoBoxAsContent()

        useCase.knownLocations.clear()
        viewModel.onCameraMoved(0.0, 0.0, 1f)

        composeTestRule
            .onNodeWithText("No address found")
            .assertIsDisplayed()
    }

    @Test
    fun show_error_if_unknown_address_query() {
        useCase.knownLocations.clear()

        setMapcodeInfoBoxAsContent()

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

        setMapcodeInfoBoxAsContent()

        composeTestRule
            .onNodeWithText("Enter address or mapcode").apply {
                performTextClearance()
                performTextInput("Street, City")
            }

        composeTestRule
            .onNodeWithText("Street, City")
            .performImeAction()

        composeTestRule.waitForIdle()

        assertThat(viewModel.location.value).isEqualTo(Location(3.0, 2.0))
    }

    @Test
    fun update_address_after_searching_known_address() {
        useCase.knownLocations.add(
            FakeLocation(
                3.0,
                2.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapcodeInfoBoxAsContent()

        composeTestRule
            .onNodeWithText("Enter address or mapcode").apply {
                performTextClearance()
                performTextInput("Street, City")
            }

        composeTestRule
            .onNodeWithText("Street, City")
            .performImeAction()

        composeTestRule.waitForIdle()

        assertThat(viewModel.mapcodeInfoState.value.address).isEqualTo("Street, City")
    }

    @Test
    fun clear_address_if_press_clear_button_in_text_field() {
        setMapcodeInfoBoxAsContent()

        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )
        viewModel.onCameraMoved(0.0, 0.0, 1f) //fill address field with something

        composeTestRule
            .onNodeWithContentDescription("Clear address")
            .performClick()

        composeTestRule
            .onNodeWithText("Enter address or mapcode")
            .assert(SemanticsMatcher.expectValue(SemanticsPropertyKey("EditableText"), null))
    }

    @Test
    fun hide_clear_button_if_empty_address() {
        setMapcodeInfoBoxAsContent()

        composeTestRule
            .onNodeWithText("Enter address or mapcode")
            .performTextClearance()

        composeTestRule
            .onNodeWithContentDescription("Clear address")
            .assertDoesNotExist()
    }

    @Test
    fun focus_address_text_field_when_click_clear_button() {
        setMapcodeInfoBoxAsContent()

        composeTestRule
            .onNodeWithContentDescription("Clear address")
            .performClick()

        composeTestRule
            .onNodeWithText("Enter address or mapcode")
            .assertIsFocused()
    }

    private fun setMapcodeInfoBoxAsContent() {
        composeTestRule.setContent {
            MapcodeInfoBox(viewModel = viewModel)
        }
    }

    private fun setMapScreenAsContent() {
        composeTestRule.setContent {
            MapScreen(viewModel = viewModel)
        }
    }
}