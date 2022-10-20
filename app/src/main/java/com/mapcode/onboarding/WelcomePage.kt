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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapcode.R
import com.mapcode.theme.LightBlue200
import com.mapcode.theme.LightBlue500
import com.mapcode.theme.LightBlue900
import com.mapcode.theme.MapcodeTheme

@Composable
fun WelcomePageVertical(modifier: Modifier, pageColors: PageColors) {
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

        WelcomeText(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(state = rememberScrollState()),
            pageColors = pageColors
        )

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
fun WelcomePageHorizontal(modifier: Modifier, pageColors: PageColors) {
    Column(modifier) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.onboarding_welcome_page_title),
            style = MaterialTheme.typography.h5,
            textAlign = TextAlign.Center,
            color = pageColors.foreground
        )

        Spacer(Modifier.height(16.dp))

        WelcomeText(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(state = rememberScrollState()),
            pageColors = pageColors
        )

        Spacer(Modifier.height(8.dp))

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
private fun WelcomeText(modifier: Modifier, pageColors: PageColors) {
    Column(modifier = modifier) {
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

}

@Preview
@Composable
private fun Preview() {
    val pageColors = PageColors(
        foreground = LightBlue900,
        background = LightBlue200,
        backgroundDark = LightBlue500
    )

    MapcodeTheme {
        Surface(color = pageColors.background) {
            WelcomePageVertical(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                pageColors = pageColors
            )
        }
    }
}

@Preview(widthDp = 800, heightDp = 300)
@Composable
private fun LandscapePreview() {
    val pageColors = PageColors(
        foreground = LightBlue900,
        background = LightBlue200,
        backgroundDark = LightBlue500
    )

    MapcodeTheme {
        Surface(color = pageColors.background) {
            WelcomePageHorizontal(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                pageColors = pageColors
            )
        }
    }
}
