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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowDown
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapcode.Mapcode
import com.mapcode.R
import com.mapcode.map.MapcodeButtons
import com.mapcode.map.MapcodeUi
import com.mapcode.theme.Cyan200
import com.mapcode.theme.Cyan500
import com.mapcode.theme.Cyan900
import com.mapcode.theme.MapcodeTheme
import com.mapcode.util.MapcodeUtils

@Composable
fun TerritoriesPageVertical(modifier: Modifier, pageColors: PageColors) {
    Column(modifier) {
        Icon(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
                .padding(32.dp)
                .weight(0.3f),
            imageVector = Icons.Outlined.PinDrop,
            contentDescription = null,
            tint = pageColors.foreground
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.1f),
            text = stringResource(R.string.onboarding_territories_page_title),
            style = MaterialTheme.typography.h4,
            textAlign = TextAlign.Center,
            color = pageColors.foreground
        )

        Column(
            modifier = Modifier
                .weight(0.4f)
                .verticalScroll(state = rememberScrollState())
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_territories_page_text),
                style = MaterialTheme.typography.body1
            )
        }

        Spacer(Modifier.height(16.dp))

        TerritoriesPageButtons(pageColors = pageColors)
    }
}

@Composable
fun TerritoriesPageHorizontal(modifier: Modifier, pageColors: PageColors) {
    Column(modifier) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.onboarding_territories_page_title),
            style = MaterialTheme.typography.h5,
            textAlign = TextAlign.Center,
            color = pageColors.foreground
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(state = rememberScrollState())
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.onboarding_territories_page_text),
                style = MaterialTheme.typography.body1
            )
        }

        Spacer(Modifier.height(8.dp))

        TerritoriesPageButtons(modifier = Modifier.wrapContentWidth(), pageColors = pageColors)
    }
}

@Composable
private fun TerritoriesPageButtons(modifier: Modifier = Modifier, pageColors: PageColors) {
    val mapcodes: List<Mapcode> = remember { MapcodeUtils.GoogleHqMapcodes() }
    var mapcodeIndex by rememberSaveable { mutableStateOf(0) }

    val mapcodeUi: MapcodeUi by remember {
        derivedStateOf {
            MapcodeUi.fromMapcode(mapcodes[mapcodeIndex], mapcodeIndex, mapcodes.size)
        }
    }

    Column(modifier) {
        Row {
            Spacer(Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.onboarding_territories_click_territory_tooltip),
                color = pageColors.foreground,
                style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold)
            )

            Icon(
                imageVector = Icons.Outlined.KeyboardDoubleArrowDown,
                contentDescription = null,
                tint = pageColors.foreground
            )
        }

        Spacer(Modifier.height(8.dp))

        MapcodeTheme(darkTheme = false) {
            MapcodeButtons(
                state = mapcodeUi,
                onTerritoryClick = {
                    mapcodeIndex = (mapcodeIndex + 1) % mapcodes.size
                },
                onMapcodeClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    val pageColors = PageColors(
        foreground = Cyan900,
        background = Cyan200,
        backgroundDark = Cyan500
    )

    MapcodeTheme {
        Surface(color = pageColors.background) {
            TerritoriesPageVertical(
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
        foreground = Cyan900,
        background = Cyan200,
        backgroundDark = Cyan500
    )

    MapcodeTheme {
        Surface(color = pageColors.background) {
            TerritoriesPageHorizontal(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                pageColors = pageColors
            )
        }
    }
}
