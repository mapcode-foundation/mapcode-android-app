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
import com.mapcode.Territory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

//    fun onShareClick(favourite: Favourite) {
//        
//    }
//
//    fun onSubmitNameChange(id: String, name: String) {
//        viewModelScope.launch {
//            useCase.setFavouriteName(id, name)
//        }
//    }
//
//    fun onDeleteClick(favourite: Favourite) {
//        useCase.deleteFavourite(favourite.id)
//    }

    private fun createListItem(favourite: Favourite): FavouriteListItem {
        val mapcode = useCase.getMapcodes(favourite.location).first()

        val mapcodeString = if (mapcode.territory == Territory.AAA) {
            mapcode.code
        } else {
            mapcode.codeWithTerritory
        }

        return FavouriteListItem(
            id = favourite.id,
            name = favourite.name,
            mapcode = mapcodeString
        )
    }
}