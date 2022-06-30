package com.mapcode.map

import com.mapcode.util.DispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher

/**
 * Created by sds100 on 01/05/2021.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider(
    testDispatcher: TestDispatcher
) : DispatcherProvider {
    override val main = testDispatcher
    override val default = testDispatcher
    override val io = testDispatcher
    override val unconfined = testDispatcher
}