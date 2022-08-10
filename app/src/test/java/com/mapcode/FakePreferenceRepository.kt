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

package com.mapcode

import androidx.datastore.preferences.core.Preferences
import com.mapcode.data.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 26/04/2021.
 */
class FakePreferenceRepository : PreferenceRepository {
    private val preferences: MutableStateFlow<Map<Preferences.Key<*>, Any?>> = MutableStateFlow(emptyMap())

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: Preferences.Key<T>): Flow<T?> {
        return preferences.map { it[key] as T? }
    }

    override fun <T> set(key: Preferences.Key<T>, value: T?) {
        preferences.value = preferences.value.toMutableMap().apply {
            this[key] = value
        }
    }
}