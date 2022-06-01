package com.mapcode.map

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Created by sds100 on 01/06/2022.
 */

@Module
@InstallIn(ViewModelComponent::class)
abstract class MapModule {
    @Binds
    abstract fun bindShowMapcodeUseCase(impl: ShowMapcodeUseCaseImpl): ShowMapcodeUseCase
}