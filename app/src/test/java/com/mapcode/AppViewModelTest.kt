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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mapcode.data.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakePreferences: FakePreferenceRepository
    private lateinit var viewModel: AppViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        fakePreferences = FakePreferenceRepository()
        viewModel = AppViewModel(fakePreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `show onboarding if never opened the app 1`() = runTest {
        fakePreferences.set(Keys.finishedOnboarding, null)
        advanceUntilIdle()
        assertThat(viewModel.showOnboarding.first()).isTrue()
    }

    @Test
    fun `show onboarding if never opened the app 2`() = runTest {
        fakePreferences.set(Keys.finishedOnboarding, false)
        advanceUntilIdle()
        assertThat(viewModel.showOnboarding.first()).isTrue()
    }

    @Test
    fun `never show onboarding again if finished onboarding`() = runTest {
        viewModel.onFinishOnboarding()
        advanceUntilIdle()
        assertThat(viewModel.showOnboarding.first()).isFalse()
    }
}