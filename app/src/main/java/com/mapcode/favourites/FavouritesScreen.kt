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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.mapcode.R
import com.mapcode.theme.MapcodeColor
import com.ramcosta.composedestinations.annotation.Destination

@Destination
@Composable
fun FavouritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavouritesViewModel,
    navigateBack: () -> Unit
) {
    val favourites by viewModel.favourites.collectAsState()

    FavouritesScreen(
        modifier = modifier,
        navigateBack = navigateBack,
        favourites = favourites,
        onDeleteFavourite = viewModel::onDeleteClick
    )
}

@Composable
private fun FavouritesScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit = {},
    favourites: List<FavouriteListItem>,
    onDeleteFavourite: (String) -> Unit = {}
) {
    val systemUiController = rememberSystemUiController()
    val systemBarColor = MaterialTheme.colors.primary

    SideEffect {
        systemUiController.setNavigationBarColor(color = systemBarColor)
    }

    Scaffold(
        modifier = modifier,
        scaffoldState = rememberScaffoldState(),
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = navigateBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.favourites_navigate_back)
                    )
                }
            }
        }
    ) { padding ->
        Content(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            favourites = favourites,
            onDeleteFavourite = onDeleteFavourite
        )
    }

    BackHandler(onBack = navigateBack)
}

@Preview
@Composable
private fun Preview() {
    FavouritesScreen(
        favourites = listOf(
            FavouriteListItem("id0", "Bla", "NLD AB.XY")
        )
    )
}

@Composable
private fun Content(
    modifier: Modifier = Modifier,
    favourites: List<FavouriteListItem>,
    onDeleteFavourite: (String) -> Unit
) {
    Column(modifier) {
        Text(
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp),
            text = stringResource(R.string.favourites_screen_caption)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(favourites) { item ->
                FavouritesListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clickable {

                        },
                    state = item,
                    onShareClick = {},
                    onEditClick = {},
                    onDeleteClick = {
                        onDeleteFavourite(item.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun FavouritesListItem(
    modifier: Modifier, state: FavouriteListItem,
    onShareClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(modifier) {
        Row {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.body1
                )

                Text(
                    text = state.mapcode,
                    style = MaterialTheme.typography.body2
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.rename_favourite_content_description)
                )
            }

            IconButton(onClick = onShareClick) {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = stringResource(R.string.share_favourite_content_description)
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_favourite_content_description),
                    tint = MapcodeColor.deleteFavouriteButton()
                )
            }
        }
    }
}