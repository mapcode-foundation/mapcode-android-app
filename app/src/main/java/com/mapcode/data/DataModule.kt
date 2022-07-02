package com.mapcode.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Created by sds100 on 01/06/2022.
 */

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindPreferenceRepository(impl: DataStorePreferenceRepository): PreferenceRepository
}