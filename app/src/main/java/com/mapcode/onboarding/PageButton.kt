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

package com.mapcode.onboarding

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mapcode.R

@Composable
fun PageButton(modifier: Modifier, pageColors: PageColors, onClick: () -> Unit) {
    val contentColor = MaterialTheme.colors.contentColorFor(pageColors.backgroundDark)

    val buttonColors = ButtonDefaults.buttonColors(
        backgroundColor = pageColors.backgroundDark,
        contentColor = contentColor
    )

    Button(
        modifier = modifier,
        onClick = onClick,
        colors = buttonColors
    ) {
        Text(stringResource(R.string.onboarding_welcome_page_learn_more_button))
    }
}