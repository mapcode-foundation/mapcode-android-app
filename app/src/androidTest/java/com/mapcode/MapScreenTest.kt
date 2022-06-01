package com.mapcode

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

        whenever(mockUseCase.getMapcodes(any(), any())).thenReturn(listOf(Mapcode("AB.XY", Territory.AAA)))

        composeTestRule.setContent {
            MapScreen(viewModel = viewModel)
        }
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_header() {
        composeTestRule
            .onNodeWithText("Mapcode (tap to copy)")
            .performClick()

        verify(mockUseCase).copyToClipboard("AAA AB.XY")
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_mapcode_code() {
        composeTestRule
            .onNodeWithText("AB.XY")
            .performClick()

        verify(mockUseCase).copyToClipboard("AAA AB.XY")
    }

    @Test
    fun copy_mapcode_to_clipboard_when_click_mapcode_territory() {
        composeTestRule
            .onNodeWithText("AAA")
            .performClick()

        verify(mockUseCase).copyToClipboard("AAA AB.XY")
    }

    @Test
    fun show_snack_bar_when_copying_mapcode() {
        composeTestRule
            .onNodeWithText("AAA")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Copied to clipboard.")
            .assertExists(errorMessageOnFail = "Can't find snackbar saying the mapcode is copied")
    }
}