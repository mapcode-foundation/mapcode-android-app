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

package com.mapcode.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightBlue100 = Color(0xFFB3E5FC)
val LightBlue200 = Color(0xFF81D4FA)
val LightBlue500 = Color(0xFF03A9F4)
val LightBlue700 = Color(0xFF0288D1)
val LightBlue900 = Color(0xFF01579B)
val Cyan100 = Color(0xFFB2EBF2)
val Cyan200 = Color(0xFF80DEEA)
val Cyan500 = Color(0xFF00BCD4)
val Cyan900 = Color(0xFF006064)
val Green600 = Color(0xFF43A047)
val Yellow300 = Color(0xFFFFF176)
val Red500 = Color(0xFFF44336)
val Red300 = Color(0xFFE57373)

object MapcodeColor {
    @Composable
    fun deleteFavouriteButton(): Color {
        return if (MaterialTheme.colors.isLight) {
            Red500
        } else {
            Red300
        }
    }
}