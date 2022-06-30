package com.mapcode.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNullOrEmpty
import com.mapcode.Mapcode
import com.mapcode.Territory
import com.mapcode.util.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by sds100 on 01/06/2022.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class MapViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var useCase: FakeShowMapcodeUseCase
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        useCase = FakeShowMapcodeUseCase()
        viewModel = MapViewModel(useCase, dispatchers = TestDispatcherProvider(testDispatcher))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `update mapcode when camera moves`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("1AB.XY", Territory.NLD), Mapcode("1CD.YZ", Territory.AAA))
            )
        )
        viewModel.onCameraMoved(1.0, 1.0, 1f)

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.code).isEqualTo("1AB.XY")
        assertThat(uiState.territoryUi.shortName).isEqualTo("NLD")
    }

    @Test
    fun `copy mapcode and territory to clipboard when mapcode is clicked`() {
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("1AB.XY", Territory.AAA))
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, 2f)
        viewModel.copyMapcode()

        assertThat(useCase.clipboard).isEqualTo("AAA 1AB.XY")
    }

    @Test
    fun `do not copy mapcode to clipboard when there is no mapcode`() {
        viewModel.copyMapcode()
        assertThat(useCase.clipboard).isNullOrEmpty()
    }

    @Test
    fun `show no internet message and empty address if geocoding fails`() = runTest {
        useCase.hasInternetConnection = false
        useCase.knownLocations.clear()

        viewModel.queryAddress("address")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.helper).isEqualTo(AddressHelper.NoInternet)
        assertThat(uiState.addressUi.address).isEmpty()
    }

    @Test
    fun `clear address and show error if moving map and no internet connection`() = runTest {
        useCase.hasInternetConnection = false
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.CD", Territory.NLD))
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, 1f)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.address).isEmpty()
        assertThat(uiState.addressUi.helper).isEqualTo(AddressHelper.NoInternet)
    }

    @Test
    fun `show address not found message and clear address if unable to geocode address`() = runTest {
        useCase.knownLocations.clear()

        viewModel.queryAddress("bad address")
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.error).isEqualTo(AddressError.UnknownAddress("bad address"))
        assertThat(uiState.addressUi.address).isEmpty()
    }

    @Test
    fun `update info correctly if queried mapcode successfully`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                2.0,
                3.0,
                addresses = listOf("Street, City, 1234AB"),
                mapcodes = listOf(Mapcode("AB.CD", Territory.NLD))
            )
        )

        viewModel.queryAddress("NLD AB.CD")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        val expectedUiState = UiState(
            code = "AB.CD",
            territoryUi = TerritoryUi("NLD", "Netherlands", 1, 1),
            latitude = "2.0",
            longitude = "3.0",
            addressUi = AddressUi(
                "Street, City, 1234AB",
                helper = AddressHelper.Location("City, 1234AB"),
                error = AddressError.None,
            )
        )

        assertThat(uiState).isEqualTo(expectedUiState)
    }

    @Test
    fun `update info correctly if geocoded address successfully`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                2.0,
                3.0,
                addresses = listOf("Street, City, 1234AB", "street, city"),
                mapcodes = listOf(Mapcode("AB.CD", Territory.NLD))
            )
        )

        viewModel.queryAddress("street, city")
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        val expectedUiState = UiState(
            code = "AB.CD",
            territoryUi = TerritoryUi("NLD", "Netherlands", 1, 1),
            latitude = "2.0",
            longitude = "3.0",
            addressUi = AddressUi(
                "Street, City, 1234AB",
                helper = AddressHelper.Location("City, 1234AB"),
                error = AddressError.None,
            )
        )

        assertThat(uiState).isEqualTo(expectedUiState)
    }

    @Test
    fun `clear mapcode query if can't decode mapcode`() = runTest {
        useCase.knownLocations.clear()

        viewModel.queryAddress("bad mapcode")
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.address).isEmpty()
        assertThat(uiState.addressUi.error).isEqualTo(AddressError.UnknownAddress("bad mapcode"))
    }

    @Test
    fun `show no address found error and empty address if current location has no known address`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = listOf("Street, City"),
                mapcodes = emptyList()
            )
        )

        viewModel.onCameraMoved(0.0, 0.0, 1f) //first get the address set to something non empty
        viewModel.onCameraMoved(2.0, 3.0, 1f)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.helper).isEqualTo(AddressHelper.NoAddress)
        assertThat(uiState.addressUi.address).isEmpty()
    }

    @Test
    fun `show no error message and address if the current location has a known address`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                2.0,
                3.0,
                addresses = listOf("Street, City, Country"),
                mapcodes = emptyList()
            )
        )

        viewModel.onCameraMoved(2.0, 3.0, 1f)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.helper).isEqualTo(AddressHelper.Location("City, Country"))
        assertThat(uiState.addressUi.address).isEqualTo("Street, City, Country")
    }

    @Test
    fun `update address after moving map`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = listOf("10 street, city, country"),
                mapcodes = listOf(Mapcode("AB.CD", Territory.NLD))
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, 1f)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.address).isEqualTo("10 street, city, country")
        assertThat(uiState.addressUi.helper).isEqualTo(AddressHelper.Location("city, country"))
    }

    @Test
    fun `update zoom state when moving camera`() = runTest {
        viewModel.onCameraMoved(1.0, 1.0, 2f)

        assertThat(viewModel.zoom.value).isEqualTo(2f)
    }

    @Test
    fun `update location state when moving camera`() = runTest {
        viewModel.onCameraMoved(3.0, 3.0, 2f)

        assertThat(viewModel.location.value).isEqualTo(Location(3.0, 3.0))
    }

    @Test
    fun `clear unknown address error after searching for known address`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                2.0,
                3.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.CD", Territory.NLD))
            )
        )

        viewModel.queryAddress("Street, City")

        advanceUntilIdle()

        assertThat(viewModel.uiState.value.addressUi.error).isEqualTo(AddressError.None)
    }

    @Test
    fun `show unknown address error temporarily`() = runTest {
        useCase.knownLocations.clear()
        viewModel.queryAddress("bad address")

        runCurrent() //let the coroutine to update the address run
        assertThat(viewModel.uiState.value.addressUi.error).isEqualTo(AddressError.UnknownAddress("bad address"))

        advanceTimeBy(3000) //the error should clear after 3 seconds
        testScheduler.runCurrent()
        assertThat(viewModel.uiState.value.addressUi.error).isEqualTo(AddressError.None)
    }

    /**
     * Test that inputting a bad address and then another resets the timer
     * to clear the "unknown address" error.
     */
    @Test
    fun `unknown address error should always clear after same amount of time`() = runTest {
        useCase.knownLocations.clear()
        viewModel.queryAddress("bad address")

        advanceTimeBy(500) //wait 500ms before doing another query
        viewModel.queryAddress("bad address 2")

        runCurrent() //let the coroutine to update the address run
        //the 2nd query should be showing
        assertThat(viewModel.uiState.value.addressUi.error).isEqualTo(AddressError.UnknownAddress("bad address 2"))

        advanceTimeBy(2500) //the 2nd query should still be running even though it has been 3 secs since the first query
        runCurrent()
        assertThat(viewModel.uiState.value.addressUi.error).isEqualTo(AddressError.UnknownAddress("bad address 2"))
    }

    @Test
    fun `show error if doing an address search and no internet`() = runTest {
        useCase.hasInternetConnection = false
        viewModel.queryAddress("Street, City")

        runCurrent()
        assertThat(viewModel.uiState.value.addressUi.helper).isEqualTo(AddressHelper.NoInternet)
    }

    @Test
    fun `searching an empty address should should dismiss the query and put the old address back`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.CD", Territory.NLD))
            )
        )

        viewModel.onCameraMoved(0.0, 0.0, 1f) //fill the address with something
        viewModel.queryAddress("")

        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.address).isEqualTo("Street, City")
        assertThat(uiState.addressUi.error).isEqualTo(AddressError.None)
    }

    @Test
    fun `do not show last 2 parts of address if only has 2 parts`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                2.0,
                3.0,
                addresses = listOf("City, Country"),
                mapcodes = emptyList()
            )
        )

        viewModel.onCameraMoved(2.0, 3.0, 1f)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.helper).isEqualTo(AddressHelper.None)
    }

    @Test
    fun `do not show last 2 parts of address if only has 1 part`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                2.0,
                3.0,
                addresses = listOf("Country"),
                mapcodes = emptyList()
            )
        )

        viewModel.onCameraMoved(2.0, 3.0, 1f)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.helper).isEqualTo(AddressHelper.None)
    }
}