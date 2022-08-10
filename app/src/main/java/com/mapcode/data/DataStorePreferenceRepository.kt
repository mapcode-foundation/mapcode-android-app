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

package com.mapcode.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class DataStorePreferenceRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope
) : PreferenceRepository {

    private val Context.dataStore by preferencesDataStore(name = "preferences")
    private val dataStore: DataStore<Preferences> = ctx.dataStore

    override fun <T> get(key: Preferences.Key<T>): Flow<T?> {
        return dataStore.data.map { it[key] }.distinctUntilChanged()
    }

    override fun <T> set(key: Preferences.Key<T>, value: T?) {
        coroutineScope.launch {
            dataStore.edit {
                if (value == null) {
                    it.remove(key)
                } else {
                    it[key] = value
                }
            }
        }
    }
}