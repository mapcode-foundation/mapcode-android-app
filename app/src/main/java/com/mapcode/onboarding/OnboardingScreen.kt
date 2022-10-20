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
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.mapcode.R
import com.mapcode.theme.*
import com.mapcode.util.animateScrollToNextPage
import com.mapcode.util.animateScrollToPreviousPage
import com.ramcosta.composedestinations.annotation.Destination
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Destination(start = true)
@Composable
fun OnboardingScreen(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState()
    val pageColors = pageColors(pagerState.currentPage)

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(pageColors.background)

    Surface(modifier = modifier, color = pageColors.background) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            val pageModifier = Modifier
                .padding(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 16.dp)
                .fillMaxSize()

            HorizontalPager(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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

            PageControls(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                pagerState = pagerState
            )
        }
    }
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen(modifier = Modifier.fillMaxSize())
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun PageControls(
    modifier: Modifier,
    pagerState: PagerState
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

        IconButton(
            modifier = Modifier.alpha(previousPageButtonAlpha),
            onClick = {
                scope.launch {
                    pagerState.animateScrollToPreviousPage()
                }
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.onboarding_previous_page_content_description),
                tint = Color.White
            )
        }

        HorizontalPagerIndicator(
            modifier = Modifier.align(Alignment.CenterVertically),
            pagerState = pagerState,
            inactiveColor = Color.White,
            activeColor = darkBackgroundColor
        )

        IconButton(onClick = {
            scope.launch {
                pagerState.animateScrollToNextPage()
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
                tint = Color.White
            )
        }
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
                    MaterialTheme.typography.body1
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