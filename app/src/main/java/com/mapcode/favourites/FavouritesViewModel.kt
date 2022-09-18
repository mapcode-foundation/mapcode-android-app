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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapcode.util.codeWithNoInternationalTerritory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val useCase: ViewFavouritesUseCase
) : ViewModel() {
    val favourites: StateFlow<List<FavouriteListItem>> =
        useCase.getFavourites()
            .map { list -> list.map { createListItem(it) } }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList()
            )

    fun onShareClick(id: String) {
        viewModelScope.launch {
            val favourite = useCase.getFavourites().first().find { it.id == id } ?: return@launch
            useCase.share(
                favouriteName = favourite.name,
                mapcode = useCase.getMapcodes(favourite.location).first()
            )
        }
    }


    fun onSubmitNameChange(id: String, name: String) {
        viewModelScope.launch {
            useCase.setFavouriteName(id, name)
        }
    }

    fun onDeleteClick(id: String) {
        useCase.deleteFavourite(id)
    }

    private fun createListItem(favourite: Favourite): FavouriteListItem {
        val mapcode = useCase.getMapcodes(favourite.location).first()

        return FavouriteListItem(
            id = favourite.id,
            name = favourite.name,
            mapcode = mapcode.codeWithNoInternationalTerritory()
        )
    }
}