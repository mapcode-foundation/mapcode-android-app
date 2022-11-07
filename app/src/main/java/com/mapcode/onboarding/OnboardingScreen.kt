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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.*
import com.mapcode.AppViewModel
import com.mapcode.R
import com.mapcode.destinations.MapScreenDestination
import com.mapcode.theme.*
import com.mapcode.util.animateScrollToNextPage
import com.mapcode.util.animateScrollToPreviousPage
import com.mapcode.util.isLastPage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue


@Destination(start = true)
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: AppViewModel,
    navigator: DestinationsNavigator,
    layoutType: OnboardingScreenLayoutType = OnboardingScreenLayoutType.Vertical
) {
    OnboardingScreen(
        modifier = modifier,
        onFinishOnboarding = {
            viewModel.onFinishOnboarding()
            navigator.popBackStack()
            navigator.navigate(MapScreenDestination.route)
        },
        layoutType = layoutType
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onFinishOnboarding: () -> Unit,
    layoutType: OnboardingScreenLayoutType
) {
    val pagerState = rememberPagerState()
    val pageColors = pageColors(pagerState.currentPage)

    val pageColor = calculatePageColor(
        currentPage = pagerState.currentPage,
        pageOffset = pagerState.currentPageOffset
    )

    Surface(modifier = modifier, color = pageColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Pager(state = pagerState, layoutType = layoutType)

                TextButton(
                    modifier = Modifier
                        .align(alignment = Alignment.TopEnd)
                        .padding(8.dp),
                    onClick = onFinishOnboarding
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip_button),
                        color = pageColors.foreground,
                    )
                }
            }

            PageControls(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                pagerState = pagerState,
                onDoneClick = onFinishOnboarding
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun Pager(state: PagerState, layoutType: OnboardingScreenLayoutType) {
    val pageModifier = Modifier
        .padding(start = 32.dp, end = 32.dp, top = 16.dp)
        .fillMaxSize()

    val pageColors = pageColors(state.currentPage)

    HorizontalPager(
        modifier = Modifier.fillMaxWidth(),
        count = 2,
        userScrollEnabled = true,
        state = state
    ) { page ->
        when (layoutType) {
            OnboardingScreenLayoutType.Vertical ->
                when (page) {
                    0 -> WelcomePageVertical(
                        modifier = pageModifier,
                        pageColors = pageColors
                    )
                    1 -> TerritoriesPageVertical(
                        modifier = pageModifier,
                        pageColors = pageColors
                    )
                }

            OnboardingScreenLayoutType.Horizontal ->
                when (page) {
                    0 -> WelcomePageHorizontal(
                        modifier = pageModifier,
                        pageColors = pageColors
                    )
                    1 -> TerritoriesPageHorizontal(
                        modifier = pageModifier,
                        pageColors = pageColors
                    )
                }
        }
    }
}

@Composable
private fun calculatePageColor(currentPage: Int, pageOffset: Float): Color {
    val currentPageColor = pageColors(currentPage).background

    if (pageOffset < 0) {
        val previousPageColor = pageColors(currentPage - 1).background
        return lerp(currentPageColor, previousPageColor, fraction = pageOffset.absoluteValue)
    } else {
        val nextPageColor = pageColors(currentPage + 1).background
        return lerp(currentPageColor, nextPageColor, fraction = pageOffset.absoluteValue)
    }
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun OnboardingScreenPreview() {
    MapcodeTheme {
        OnboardingScreen(
            modifier = Modifier.fillMaxSize(),
            onFinishOnboarding = {},
            layoutType = OnboardingScreenLayoutType.Vertical
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PageControls(
    modifier: Modifier,
    pagerState: PagerState,
    onDoneClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val pageColors = pageColors(pagerState.currentPage)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val previousPageButtonAlpha = if (pagerState.currentPage == 0) {
            0.0f
        } else {
            1.0f
        }

        PreviousPageButton(
            modifier = Modifier.alpha(previousPageButtonAlpha),
            color = pageColors.foreground,
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPreviousPage()
                }
            }
        )

        HorizontalPagerIndicator(
            modifier = Modifier.align(Alignment.CenterVertically),
            pagerState = pagerState,
            inactiveColor = pageColors.foreground,
            activeColor = pageColors.backgroundDark
        )

        if (pagerState.isLastPage()) {
            DoneButton(color = pageColors.foreground, onClick = onDoneClick)
        } else {
            NextPageButton(color = pageColors.foreground) {
                scope.launch {
                    pagerState.animateScrollToNextPage()
                }
            }
        }
    }
}

@Composable
private fun DoneButton(modifier: Modifier = Modifier, color: Color, onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Done,
            contentDescription = stringResource(R.string.onboarding_done_content_description),
            tint = color
        )
    }
}

@Composable
private fun NextPageButton(modifier: Modifier = Modifier, color: Color, onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.ArrowForward,
            contentDescription = stringResource(R.string.onboarding_next_page_content_description),
            tint = color
        )
    }
}

@Composable
private fun PreviousPageButton(modifier: Modifier = Modifier, color: Color, onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.onboarding_previous_page_content_description),
            tint = color
        )
    }
}

private fun pageColors(page: Int): PageColors {
    return when (page) {
        0 -> PageColors(
            foreground = LightBlue900,
            background = LightBlue100,
            backgroundDark = LightBlue500
        )
        else -> PageColors(
            foreground = Cyan900,
            background = Cyan100,
            backgroundDark = Cyan500
        )
    }
}