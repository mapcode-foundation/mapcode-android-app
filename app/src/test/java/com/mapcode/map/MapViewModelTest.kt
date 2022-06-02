package com.mapcode.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.mapcode.Mapcode
import com.mapcode.Territory
import com.mapcode.UnknownMapcodeException
import com.mapcode.util.Location
import com.mapcode.util.NoAddressException
import com.mapcode.util.UnknownAddressException
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
    fun `show no internet message and empty address if geocoding fails`() = runTest {
        returnUnknownWhenDecodeMapcode()
        whenever(mockUseCase.geocode(any())).thenReturn(failure(IOException()))

        viewModel.queryAddress("address")
        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.addressError).isEqualTo(AddressError.NoInternet)
        assertThat(uiState.address).isEmpty()
    }

    @Test
    fun `clear address and show error if moving map and no internet connection`() = runTest {
        whenever(mockUseCase.reverseGeocode(1.0, 1.0)).thenReturn(failure(IOException()))
        whenever(mockUseCase.getMapcodes(1.0, 1.0)).thenReturn(listOf(Mapcode("AB.CD", Territory.NLD)))

        viewModel.onCameraMoved(1.0, 1.0)
        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.address).isEmpty()
        assertThat(uiState.addressError).isEqualTo(AddressError.NoInternet)
    }

    @Test
    fun `show address not found message and clear address if unable to geocode address`() = runTest {
        returnUnknownWhenDecodeMapcode()
        whenever(mockUseCase.geocode(any())).thenReturn(failure(UnknownAddressException()))

        viewModel.queryAddress("bad address")
        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.addressError).isEqualTo(AddressError.UnknownAddress("bad address"))
        assertThat(uiState.address).isEmpty()
    }

    @Test
    fun `update info correctly if queried mapcode successfully`() = runTest {
        whenever(mockUseCase.decodeMapcode("NLD AB.CD")).thenReturn(success(Location(2.0, 3.0)))
        whenever(mockUseCase.reverseGeocode(2.0, 3.0)).thenReturn(success("Street, City, 1234AB"))
        whenever(mockUseCase.getMapcodes(2.0, 3.0)).thenReturn(listOf(Mapcode("AB.CD", Territory.NLD)))

        viewModel.queryAddress("NLD AB.CD")
        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        val expectedUiState = MapcodeInfoState(
            code = "AB.CD",
            territory = "NLD",
            latitude = "2.0",
            longitude = "3.0",
            addressError = AddressError.None,
            address = "Street, City, 1234AB"
        )

        assertThat(uiState).isEqualTo(expectedUiState)
    }

    @Test
    fun `update info correctly if geocoded address successfully`() = runTest {
        returnUnknownWhenDecodeMapcode()
        whenever(mockUseCase.geocode("street, city")).thenReturn(success(Location(2.0, 3.0)))
        whenever(mockUseCase.reverseGeocode(2.0, 3.0)).thenReturn(success("Street, City, 1234AB"))
        whenever(mockUseCase.getMapcodes(2.0, 3.0)).thenReturn(listOf(Mapcode("AB.CD", Territory.NLD)))

        viewModel.queryAddress("street, city")
        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        val expectedUiState = MapcodeInfoState(
            code = "AB.CD",
            territory = "NLD",
            latitude = "2.0",
            longitude = "3.0",
            addressError = AddressError.None,
            address = "Street, City, 1234AB"
        )

        assertThat(uiState).isEqualTo(expectedUiState)
    }

    @Test
    fun `clear mapcode query if can't decode mapcode`() = runTest {
        returnUnknownWhenDecodeMapcode()
        whenever(mockUseCase.geocode("bad mapcode")).thenReturn(failure(UnknownAddressException()))

        viewModel.queryAddress("bad mapcode")
        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.address).isEmpty()
        assertThat(uiState.addressError).isEqualTo(AddressError.UnknownAddress("bad mapcode"))
    }

    @Test
    fun `show no address found error and empty address if current location has no known address`() = runTest {
        whenever(mockUseCase.reverseGeocode(2.0, 3.0)).thenReturn(failure(NoAddressException()))

        viewModel.onCameraMoved(2.0, 3.0)
        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.addressError).isEqualTo(AddressError.NoAddress)
        assertThat(uiState.address).isEmpty()
    }

    @Test
    fun `show no error message and address if the current location has a known address`() = runTest {
        whenever(mockUseCase.reverseGeocode(2.0, 3.0)).thenReturn(success("Street, City"))

        viewModel.onCameraMoved(2.0, 3.0)
        advanceUntilIdle()

        val uiState = viewModel.mapcodeInfoState.value
        assertThat(uiState.addressError).isEqualTo(AddressError.None)
        assertThat(uiState.address).isEqualTo("Street, City")
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

    private fun returnUnknownWhenDecodeMapcode() {
        whenever(mockUseCase.decodeMapcode(any())).thenReturn(failure(UnknownMapcodeException("")))
    }
}