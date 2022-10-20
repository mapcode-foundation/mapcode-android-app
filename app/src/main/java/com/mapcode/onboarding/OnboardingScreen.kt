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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    navigator: DestinationsNavigator
) {
    OnboardingScreen(
        modifier = modifier,
        onFinishOnboarding = {
            viewModel.onFinishOnboarding()
            navigator.navigate(MapScreenDestination.route)
            navigator.popBackStack()
        }
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun OnboardingScreen(modifier: Modifier = Modifier, onFinishOnboarding: () -> Unit) {
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
            val pageModifier = Modifier
                .padding(start = 32.dp, end = 32.dp, top = 16.dp)
                .fillMaxSize()

            Box(modifier = Modifier.weight(1f)) {
                HorizontalPager(
                    modifier = Modifier.fillMaxWidth(),
                    count = 3,
                    userScrollEnabled = true,
                    state = pagerState
                ) { page ->
                    when (page) {
                        0 -> WelcomePage(modifier = pageModifier, pageColors = pageColors)
                        1 -> TerritoriesPage(modifier = pageModifier)
                        2 -> LocationPermissionPage(modifier = pageModifier)
                    }
                }

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
    OnboardingScreen(modifier = Modifier.fillMaxSize(), onFinishOnboarding = {})
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PageControls(
    modifier: Modifier,
    pagerState: PagerState,
    onDoneClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val darkBackgroundColor = pageColors(pagerState.currentPage).backgroundDark

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
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPreviousPage()
                }
            }
        )

        HorizontalPagerIndicator(
            modifier = Modifier.align(Alignment.CenterVertically),
            pagerState = pagerState,
            inactiveColor = Color.White,
            activeColor = darkBackgroundColor
        )

        if (pagerState.isLastPage()) {
            DoneButton(onClick = onDoneClick)
        } else {
            NextPageButton {
                scope.launch {
                    pagerState.animateScrollToNextPage()
                }
            }
        }
    }
}

@Composable
private fun DoneButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.Done,
            contentDescription = stringResource(R.string.onboarding_done_content_description),
            tint = Color.White
        )
    }
}

@Composable
private fun NextPageButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.ArrowForward,
            contentDescription = stringResource(R.string.onboarding_next_page_content_description),
            tint = Color.White
        )
    }
}

@Composable
private fun PreviousPageButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = Icons.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.onboarding_previous_page_content_description),
            tint = Color.White
        )
    }
}

@Composable
private fun WelcomePage(modifier: Modifier, pageColors: PageColors) {
    Column(modifier) {
        Image(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .weight(0.3f),
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = null,
            contentScale = ContentScale.FillHeight
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f),
            text = stringResource(R.string.onboarding_welcome_page_title),
            style = MaterialTheme.typography.h4,
            textAlign = TextAlign.Center,
            color = pageColors.foreground
        )

        Column(
            modifier = Modifier
                .weight(0.4f)
                .verticalScroll(state = rememberScrollState())
        ) {
            val text = buildAnnotatedString {
                pushStyle(
                    MaterialTheme.typography.subtitle1
                        .copy(fontWeight = FontWeight.W600, color = pageColors.foreground)
                        .toSpanStyle()
                )
                append(stringResource(R.string.onboarding_welcome_page_text_1))
                pop()

                append("\n\n")

                val normalTextColor = MaterialTheme.colors.contentColorFor(pageColors.background)
                val normalTextStyle =
                    MaterialTheme.typography.body1.copy(color = normalTextColor).toSpanStyle()

                pushStyle(normalTextStyle)
                append(stringResource(R.string.onboarding_welcome_page_text_2))
                pop()
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                style = MaterialTheme.typography.body1
            )
        }

        Spacer(Modifier.height(16.dp))

        val learnMoreUrl = stringResource(R.string.onboarding_welcome_page_learn_more_url)
        val uriHandler = LocalUriHandler.current

        PageButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            pageColors = pageColors,
            onClick = { uriHandler.openUri(learnMoreUrl) }
        )
    }
}

@Composable
private fun PageButton(modifier: Modifier, pageColors: PageColors, onClick: () -> Unit) {
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

@Composable
private fun TerritoriesPage(modifier: Modifier) {
    Column(modifier) {

    }
}

@Composable
private fun LocationPermissionPage(modifier: Modifier) {
    Column(modifier) {

    }
}

private fun pageColors(page: Int): PageColors {
    return when (page) {
        0 -> PageColors(
            foreground = LightBlue900,
            background = LightBlue200,
            backgroundDark = LightBlue500
        )
        1 -> PageColors(
            foreground = Cyan900,
            background = Cyan500,
            backgroundDark = Cyan700
        )
        else -> PageColors(
            foreground = LightGreen900,
            background = LightGreen500,
            backgroundDark = LightGreen700
        )
    }
}