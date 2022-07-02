package com.mapcode.data

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 02/07/2022.
 */
interface PreferenceRepository {
    fun <T> get(key: Preferences.Key<T>): Flow<T?>
    fun <T> set(key: Preferences.Key<T>, value: T?)
}