package com.mapcode.map

import android.content.Context
import com.mapcode.util.DefaultDispatcherProvider
import com.mapcode.util.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Created by sds100 on 01/06/2022.
 */

@Module
@InstallIn(ViewModelComponent::class)
object MapModule {
    @Provides
    fun provideShowMapcodeUseCase(@ApplicationContext ctx: Context): ShowMapcodeUseCase {
        return ShowMapcodeUseCaseImpl(ctx)
    }

    @Provides
    fun provideDispatchers(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }
}