package com.mapcode.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.mapcode.Mapcode
import com.mapcode.Territory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Created by sds100 on 01/06/2022.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class MapViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockUseCase: ShowMapcodeUseCase
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockUseCase = mock()
        viewModel = MapViewModel(mockUseCase)
    }

    @Test
    fun `update mapcode when camera moves`() = runTest {
        val fakeMapcodes = listOf(Mapcode("1AB.XY", Territory.AAA), Mapcode("1CD.YZ", Territory.NLD))
        whenever(mockUseCase.getMapcodes(1.0, 1.0)).thenReturn(fakeMapcodes)

        viewModel.onCameraMoved(1.0, 1.0)

        val expectedUiState = MapcodeInfoState(code = "1AB.XY", territory = "AAA")
        advanceUntilIdle()
        assertThat(viewModel.mapcodeInfoState.value).isEqualTo(expectedUiState)
    }

    @Test
    fun `copy mapcode and territory to clipboard when mapcode is clicked`() {
        val fakeMapcodes = listOf(Mapcode("1AB.XY", Territory.AAA))
        whenever(mockUseCase.getMapcodes(1.0, 1.0)).thenReturn(fakeMapcodes)
        viewModel.onCameraMoved(1.0, 1.0)

        viewModel.copyMapcode()
        verify(mockUseCase).copyToClipboard("AAA 1AB.XY")
    }

    @Test
    fun `do not copy mapcode to clipboard when there is no mapcode`() {
        viewModel.copyMapcode()
        verify(mockUseCase, never()).copyToClipboard(any())
    }
}