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