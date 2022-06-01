package com.mapcode.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.mapcode.Mapcode
import com.mapcode.Territory
import junit.framework.Assert.fail
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
import java.io.IOException
import kotlin.Result.Companion.failure

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
        val fakeMapcodes = listOf(Mapcode("1AB.XY", Territory.NLD), Mapcode("1CD.YZ", Territory.AAA))
        whenever(mockUseCase.getMapcodes(1.0, 1.0)).thenReturn(fakeMapcodes)

        viewModel.onCameraMoved(1.0, 1.0)

        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.code).isEqualTo("1AB.XY")
        assertThat(uiState.territory).isEqualTo("NLD")
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

    @Test
    fun `show no internet message and empty address if geocoding fails`() {
        whenever(mockUseCase.geocode(any())).thenReturn(failure(IOException()))
        viewModel.findAddress("address")

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.addressError).isEqualTo(AddressError.NoInternet)
        assertThat(uiState.address).isEmpty()
    }

    @Test
    fun `clear address if no internet connection`() {
        fail()
    }

    @Test
    fun `show address not found message if unable to geocode address`() {
        fail()
    }

    @Test
    fun `do not clear address if address not found`() {

        fail()
    }

    @Test
    fun `update location and mapcode if geocoded address successfully`() {
        fail()
    }

    @Test
    fun `update location and mapcode if found mapcode successfully`() {
        fail()
    }

    @Test
    fun `first interpret search as mapcode and if fails then as address`() {
        fail()
    }

    @Test
    fun `update address after moving map`() {
        fail()
    }
}