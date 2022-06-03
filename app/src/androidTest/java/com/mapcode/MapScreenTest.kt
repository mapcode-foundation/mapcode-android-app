package com.mapcode

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
        viewModel = MapViewModel(useCase)
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_header() {
        useCase.locations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()

        composeTestRule
            .onNodeWithText("Mapcode (tap to copy)")
            .performClick()

        assertThat(useCase.clipboard).isEqualTo("AAA AB.XY")
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_mapcode_code() {
        useCase.locations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()

        composeTestRule
            .onNodeWithText("AB.XY")
            .performClick()

        assertThat(useCase.clipboard).isEqualTo("AAA AB.XY")
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_mapcode_territory() {
        useCase.locations.add(
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

        assertThat(useCase.clipboard).isEqualTo("AAA AB.XY")
    }

    @Test
    fun show_snack_bar_when_copying_mapcode() {
        useCase.locations.add(
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

        setMapScreenAsContent()

        composeTestRule
            .onNodeWithText("No internet?")
            .assertIsDisplayed()
    }

    @Test
    fun show_warning_if_no_address_exists_for_location() {
        useCase.locations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

        setMapScreenAsContent()

        composeTestRule
            .onNodeWithText("No address found")
            .assertIsDisplayed()
    }

    @Test
    fun show_error_if_unknown_address_query() {
        useCase.locations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.XY", Territory.AAA))
            )
        )

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

    private fun setMapScreenAsContent() {
        composeTestRule.setContent {
            MapScreen(viewModel = viewModel)
        }
    }
}