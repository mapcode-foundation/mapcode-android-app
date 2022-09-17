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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class FavouritesNameDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    var name: String by mutableStateOf("")
    var mapcode: String by mutableStateOf("")

    @Test
    fun clear_and_focus_text_when_clicking_clear() {
        name = "address"
        setContent()

        composeTestRule.onNodeWithContentDescription("Clear name").performClick()
        composeTestRule.onNodeWithText("Name").assertIsFocused()
        composeTestRule.onNodeWithText("").assertIsDisplayed()
    }

    @Test
    fun display_mapcode() {
        mapcode = "NLD AB.XY"
        setContent()

        composeTestRule.onNodeWithText("Mapcode: NLD AB.XY").assertIsDisplayed()
    }

    @Test
    fun disable_save_button_when_empty_text() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Clear name").performClick()
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun show_error_when_empty_text() {
        setContent()
        composeTestRule.onNodeWithContentDescription("Clear name").performClick()
        composeTestRule.onNodeWithText("Name must not be empty!").assertIsDisplayed()
    }

    private fun setContent() {
        composeTestRule.setContent {
            FavouritesNameDialog(
                name = name,
                mapcode = mapcode,
                onNameChange = {
                    name = it
                },
                onDismiss = { }) {
            }
        }
    }
}