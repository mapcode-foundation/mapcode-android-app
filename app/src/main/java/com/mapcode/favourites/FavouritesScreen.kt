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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.mapcode.R
import com.mapcode.theme.MapcodeColor
import com.mapcode.util.Location
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator

@Destination
@Composable
fun FavouritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavouritesViewModel,
    resultBackNavigator: ResultBackNavigator<Location>
) {
    val favourites by viewModel.favourites.collectAsState()

    FavouritesScreen(
        modifier = modifier,
        navigateBack = resultBackNavigator::navigateBack,
        onFavouriteClick = { id ->
            viewModel.getFavouriteLocation(id)?.also {
                resultBackNavigator.navigateBack(it)
            }
        },
        favourites = favourites,
        onShareFavourite = viewModel::onShareClick,
        onDeleteFavourite = viewModel::onDeleteClick,
        onChangeFavouriteName = viewModel::onSubmitNameChange
    )
}

@Composable
private fun FavouritesScreen(
    modifier: Modifier = Modifier,
    navigateBack: () -> Unit = {},
    onFavouriteClick: (String) -> Unit = {},
    favourites: List<FavouriteListItem>,
    onShareFavourite: (String) -> Unit = {},
    onDeleteFavourite: (String) -> Unit = {},
    onChangeFavouriteName: (String, String) -> Unit = { _, _ -> },
) {
    val systemUiController = rememberSystemUiController()
    val systemBarColor = MaterialTheme.colors.primary

    DisposableEffect(systemUiController, systemBarColor) {
        systemUiController.setNavigationBarColor(color = systemBarColor)
        onDispose {}
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
            onShareFavourite = onShareFavourite,
            onDeleteFavourite = onDeleteFavourite,
            onChangeFavouriteName = onChangeFavouriteName,
            onFavouriteClick = onFavouriteClick
        )
    }

    BackHandler(onBack = navigateBack)
}

@Preview
@Composable
private fun CreateFavouriteDialogPreview() {
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
    onChangeFavouriteName: (String, String) -> Unit,
    onShareFavourite: (String) -> Unit,
    onDeleteFavourite: (String) -> Unit,
    onFavouriteClick: (String) -> Unit = {}
) {
    var showDeleteFavouriteDialog: Boolean by rememberSaveable { mutableStateOf(false) }
    var showFavouriteNameDialog: Boolean by rememberSaveable { mutableStateOf(false) }
    var dialogFavourite: FavouriteListItem? by rememberSaveable { mutableStateOf(null) }

    if (showFavouriteNameDialog) {
        RenameFavouriteDialog(
            name = dialogFavourite!!.name,
            onNameChange = { dialogFavourite = dialogFavourite!!.copy(name = it) },
            onDismiss = { showFavouriteNameDialog = false },
            onSubmitClick = {
                onChangeFavouriteName(dialogFavourite!!.id, dialogFavourite!!.name)
                showFavouriteNameDialog = false
            })
    }

    if (showDeleteFavouriteDialog) {
        DeleteFavouriteDialog(
            favouriteName = dialogFavourite!!.name,
            onConfirm = {
                onDeleteFavourite(dialogFavourite!!.id)
                showDeleteFavouriteDialog = false
            },
            onDismiss = { showDeleteFavouriteDialog = false }
        )
    }

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
                            onFavouriteClick(item.id)
                        },
                    state = item,
                    onShareClick = {
                        onShareFavourite(item.id)
                    },
                    onEditClick = {
                        dialogFavourite = item
                        showFavouriteNameDialog = true
                    },
                    onDeleteClick = {
                        dialogFavourite = item
                        showDeleteFavouriteDialog = true
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