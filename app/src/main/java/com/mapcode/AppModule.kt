package com.mapcode

import com.mapcode.util.DefaultDispatcherProvider
import com.mapcode.util.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import javax.inject.Singleton

/**
 * Created by sds100 on 28/06/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppCoroutineScope(): CoroutineScope {
        return MainScope()
    }

    @Provides
    @Singleton
    fun provideDispatchers(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }
}