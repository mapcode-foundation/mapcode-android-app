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

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.mapcode.R
import com.mapcode.theme.Cyan500
import com.mapcode.theme.LightBlue500
import com.mapcode.theme.LightGreen500
import com.mapcode.util.scrollToNextPage
import com.mapcode.util.scrollToPreviousPage
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Destination(start = true)
@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    val backgroundColor = when (pagerState.currentPage) {
        0 -> LightBlue500
        1 -> Cyan500
        else -> LightGreen500
    }

    Surface(modifier = modifier, color = backgroundColor) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HorizontalPager(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                count = 3,
                userScrollEnabled = true,
                state = pagerState
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> TerritoriesPage()
                    2 -> LocationPermissionPage()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val contentColor = MaterialTheme.colors.contentColorFor(backgroundColor)

                val previousPageButtonAlpha = if (pagerState.currentPage == 0) {
                    0.0f
                } else {
                    1.0f
                }

                IconButton(
                    modifier = Modifier.alpha(previousPageButtonAlpha),
                    onClick = {
                        scope.launch {
                            pagerState.scrollToPreviousPage()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.onboarding_previous_page_content_description),
                        tint = contentColor
                    )
                }

                HorizontalPagerIndicator(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    pagerState = pagerState,
                    inactiveColor = Color.White,
                    activeColor = contentColor
                )

                IconButton(onClick = {
                    scope.launch {
                        pagerState.scrollToNextPage()
                    }
                }) {
                    val icon = if (pagerState.currentPage == pagerState.pageCount - 1) {
                        Icons.Outlined.Done
                    } else {
                        Icons.Outlined.ArrowForward
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(R.string.onboarding_next_page_content_description),
                        tint = contentColor
                    )
                }
            }
        }
    }
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen(modifier = Modifier.fillMaxSize())
}

@Composable
private fun WelcomePage() {

}

@Composable
private fun TerritoriesPage() {

}

@Composable
private fun LocationPermissionPage() {

}