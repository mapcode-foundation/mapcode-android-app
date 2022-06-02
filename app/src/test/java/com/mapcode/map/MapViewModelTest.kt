package com.mapcode.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.mapcode.Mapcode
import com.mapcode.Territory
import com.mapcode.util.Location
import com.mapcode.util.UnknownAddressException
import junit.framework.Assert.fail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.io.IOException
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

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

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `update mapcode when camera moves`() = runTest {
        val fakeMapcodes = listOf(Mapcode("1AB.XY", Territory.NLD), Mapcode("1CD.YZ", Territory.AAA))
        whenever(mockUseCase.getMapcodes(1.0, 1.0)).thenReturn(fakeMapcodes)
        whenever(mockUseCase.reverseGeocode(1.0, 1.0)).thenReturn(success(""))

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
        viewModel.queryAddress("address")

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.addressError).isEqualTo(AddressError.NoInternet)
        assertThat(uiState.address).isEmpty()
    }

    @Test
    fun `clear address if no internet connection`() {
        whenever(mockUseCase.geocode(any())).thenReturn(failure(IOException()))
        viewModel.onCameraMoved(1.0, 1.0)

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.address).isEmpty()
    }

    @Test
    fun `show address not found message if unable to geocode address`() {
        whenever(mockUseCase.geocode(any())).thenReturn(failure(UnknownAddressException()))
        viewModel.queryAddress("not an address")

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.addressError).isEqualTo(AddressError.CantFindAddress)
    }

    @Test
    fun `do not clear address if address not found`() {
        whenever(mockUseCase.geocode(any())).thenReturn(failure(UnknownAddressException()))
        viewModel.queryAddress("not an address")

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.address).isEqualTo("not an address")
    }

    @Test
    fun `update location if geocoded address successfully`() = runTest {
        whenever(mockUseCase.geocode("street, city")).thenReturn(success(Location(2.0, 3.0)))
        viewModel.queryAddress("street, city")

        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.latitude).isEqualTo("2.0")
        assertThat(uiState.longitude).isEqualTo("3.0")
    }

    @Test
    fun `update address if queried mapcode successfully`() = runTest {
        whenever(mockUseCase.getMapcodeLocation("NLD AB.CD")).thenReturn(success(Location(2.0, 3.0)))
        whenever(mockUseCase.reverseGeocode(2.0, 3.0)).thenReturn(success("street, city"))

        viewModel.queryAddress("NLD AB.CD")
        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.address).isEqualTo("street, city")
    }

    @Test
    fun `update mapcode if queried address successfully`() = runTest {
        fail()
    }

    @Test
    fun `show correct address if geocoded address successfully`() = runTest {
        whenever(mockUseCase.geocode("street, city")).thenReturn(success(Location(2.0, 3.0)))
        viewModel.queryAddress("street, city")

        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.latitude).isEqualTo("2.0")
        assertThat(uiState.longitude).isEqualTo("3.0")
    }

    @Test
    fun `update location if queried mapcode successfully`() = runTest {
        fail()
    }

    @Test
    fun `show correctly formatted mapcode if queried mapcode successfully`() = runTest {
        fail()
    }

    @Test
    fun `show unknown mapcode error if can't decode mapcode`() {

    }

    @Test
    fun `do not clear mapcode query if can't decode mapcode`() {
    }

    @Test
    fun `first interpret search as mapcode and if fails then as address`() {
        fail()
    }

    @Test
    fun `show no error if address can be geocoded`() {
        fail()
    }

    @Test
    fun `update address after moving map`() = runTest {
        whenever(mockUseCase.reverseGeocode(1.0, 1.0)).thenReturn(success("10 street, city"))
        viewModel.onCameraMoved(1.0, 1.0)
        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.address).isEqualTo("10 street, city")
        assertThat(uiState.addressError).isEqualTo(AddressError.None)
    }
}