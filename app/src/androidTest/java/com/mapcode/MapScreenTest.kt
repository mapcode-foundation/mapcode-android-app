package com.mapcode

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mapcode.map.MapScreen
import com.mapcode.map.MapViewModel
import com.mapcode.map.ShowMapcodeUseCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException
import kotlin.Result.Companion.failure

/**
 * Created by sds100 on 01/06/2022.
 */
class MapScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var mockUseCase: ShowMapcodeUseCase
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        mockUseCase = mock()
        viewModel = MapViewModel(mockUseCase)
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_header() {
        mockGetMapcodes(Mapcode("AB.XY", Territory.AAA))

        composeTestRule
            .onNodeWithText("Mapcode (tap to copy)")
            .performClick()

        verify(mockUseCase).copyToClipboard("AAA AB.XY")
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_mapcode_code() {
        mockGetMapcodes(Mapcode("AB.XY", Territory.AAA))

        composeTestRule
            .onNodeWithText("AB.XY")
            .performClick()

        verify(mockUseCase).copyToClipboard("AAA AB.XY")
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_mapcode_territory() {
        mockGetMapcodes(Mapcode("AB.XY", Territory.AAA))

        composeTestRule
            .onNodeWithText("AAA")
            .performClick()

        verify(mockUseCase).copyToClipboard("AAA AB.XY")
    }

    @Test
    fun show_snack_bar_when_copying_mapcode() {
        mockGetMapcodes(Mapcode("AB.XY", Territory.AAA))

        composeTestRule
            .onNodeWithText("AAA")
            .performClick()

        composeTestRule
            .onNodeWithText("Copied to clipboard.")
            .assertIsDisplayed()
    }

    @Test
    fun show_error_if_no_internet() {
        whenever(mockUseCase.reverseGeocode(any(), any())).thenReturn(failure(IOException()))
        setMapScreenContent()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("No internet?")
            .assertIsDisplayed()
    }

    @Test
    fun show_error_if_no_address_exists_for_location() {

    }

    private fun mockGetMapcodes(vararg mapcode: Mapcode) {
        whenever(mockUseCase.getMapcodes(any(), any())).thenReturn(mapcode.toList())
    }

    private fun setMapScreenContent() {
        composeTestRule.setContent {
            MapScreen(viewModel = viewModel)
        }
    }
}