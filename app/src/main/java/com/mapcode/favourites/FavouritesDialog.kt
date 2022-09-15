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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mapcode.R
import com.mapcode.util.CustomDialog
import com.mapcode.util.Location

@Composable
private fun FavouritesDialog(
    favourites: List<Favourite>,
    onDismiss: () -> Unit
) {
    CustomDialog(
        title = stringResource(R.string.favourites_dialog_title),
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_dialog_button))
            }
        },
        onDismissRequest = onDismiss
    ) {
        LazyColumn {
            items(favourites) {
                FavouritesListItem(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview
@Composable
private fun FavouritesDialogPreview() {
    FavouritesDialog(
        favourites = listOf(
            Favourite("", Location(0.0, 0.0), "")
        ),
        onDismiss = {}
    )
}

@Composable
private fun FavouritesListItem(modifier: Modifier) {
    Row(modifier) {

    }
}