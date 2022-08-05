package com.mapcode.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import assertk.assertThat
import assertk.assertions.*
import com.mapcode.*
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
        viewModel = MapViewModel(
            useCase,
            dispatchers = TestDispatcherProvider(testDispatcher),
            preferences = FakePreferenceRepository()
        )
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
        viewModel.onCameraMoved(1.0, 1.0, 0f)

        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.mapcodeUi.code).isEqualTo("1AB.XY")
        assertThat(uiState.mapcodeUi.territoryShortName).isEqualTo("NLD")
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

        viewModel.onCameraMoved(1.0, 1.0, 0f)
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

        viewModel.onCameraMoved(1.0, 1.0, 0f)
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
            mapcodeUi = MapcodeUi("AB.CD", "NLD", "Netherlands", 1, 1),
            latitude = "2.0000000",
            longitude = "3.0000000",
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
            mapcodeUi = MapcodeUi("AB.CD", "NLD", "Netherlands", 1, 1),
            latitude = "2.0000000",
            longitude = "3.0000000",
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

        viewModel.onCameraMoved(0.0, 0.0, 0f) //first get the address set to something non empty
        viewModel.onCameraMoved(2.0, 3.0, 0f)
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

        viewModel.onCameraMoved(2.0, 3.0, 0f)
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

        viewModel.onCameraMoved(1.0, 1.0, 0f)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.address).isEqualTo("10 street, city, country")
        assertThat(uiState.addressUi.helper).isEqualTo(AddressHelper.Location("city, country"))
    }

    @Test
    fun `update location state when moving camera`() = runTest {
        viewModel.onCameraMoved(3.0, 2.0, 0f)
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("3.0000000")
        assertThat(uiState.longitude).isEqualTo("2.0000000")
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
                1.0,
                1.0,
                addresses = listOf("Street, City"),
                mapcodes = listOf(Mapcode("AB.CD", Territory.NLD))
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, 0f) //fill the address with something
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

        viewModel.onCameraMoved(2.0, 3.0, 0f)
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

        viewModel.onCameraMoved(2.0, 3.0, 0f)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.helper).isEqualTo(AddressHelper.None)
    }

    @Test
    fun `clicking territory should cycle through mapcodes`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = listOf("10 street, city, country"),
                mapcodes = listOf(
                    Mapcode("AB.CD", Territory.NLD),
                    Mapcode("HHH.HHH", Territory.AAA),
                    Mapcode("GGG.GGG", Territory.DEU)
                )
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, 0f)
        advanceUntilIdle()

        val uiState1 = viewModel.uiState.value
        assertThat(uiState1.mapcodeUi).isEqualTo(MapcodeUi("AB.CD", "NLD", "Netherlands", 1, 3))

        viewModel.onTerritoryClick()
        runCurrent()

        val uiState2 = viewModel.uiState.value
        assertThat(uiState2.mapcodeUi).isEqualTo(MapcodeUi("HHH.HHH", "AAA", "International", 2, 3))

        viewModel.onTerritoryClick()
        runCurrent()

        val uiState3 = viewModel.uiState.value
        assertThat(uiState3.mapcodeUi).isEqualTo(MapcodeUi("GGG.GGG", "DEU", "Germany", 3, 3))

        viewModel.onTerritoryClick()
        runCurrent()

        val uiState4 = viewModel.uiState.value
        assertThat(uiState4.mapcodeUi).isEqualTo(MapcodeUi("AB.CD", "NLD", "Netherlands", 1, 3))
    }

    @Test
    fun `clicking territory should do nothing if no mapcodes`() = runTest {
        viewModel.onTerritoryClick()
        assertThat(viewModel.uiState.value.mapcodeUi).isEqualTo(MapcodeUi("", "", "", 0, 0))
    }

    @Test
    fun `only show the first mapcode for a territory`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = listOf("10 street, city, country"),
                mapcodes = listOf(
                    Mapcode("AB.CD", Territory.NLD),
                    Mapcode("VX.YZ", Territory.NLD)
                )
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, 0f)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState.mapcodeUi).isEqualTo(MapcodeUi("AB.CD", "NLD", "Netherlands", 1, 1))
    }

    @Test
    fun `searching latitude should update location`() = runTest {
        viewModel.queryLatitude("1.0")
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("1.0000000")
        assertThat(uiState.longitude).isEqualTo("0.0000000")
    }

    @Test
    fun `searching too large latitude should go to 90`() = runTest {
        viewModel.queryLatitude("91.0")
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("90.0000000")
        assertThat(uiState.longitude).isEqualTo("0.0000000")
    }

    @Test
    fun `searching too small latitude should go to -90`() = runTest {
        viewModel.queryLatitude("-91.0")
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("-90.0000000")
        assertThat(uiState.longitude).isEqualTo("0.0000000")
    }

    @Test
    fun `searching empty latitude should keep current location`() = runTest {
        viewModel.onCameraMoved(1.0, 1.0, 0f)

        viewModel.queryLatitude("")
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("1.0000000")
        assertThat(uiState.longitude).isEqualTo("1.0000000")
    }

    @Test
    fun `searching longitude should update location`() = runTest {
        viewModel.queryLongitude("1.0")
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("0.0000000")
        assertThat(uiState.longitude).isEqualTo("1.0000000")
    }

    @Test
    fun `searching too large longitude should go to 180`() = runTest {
        viewModel.queryLongitude("181.0")
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("0.0000000")
        assertThat(uiState.longitude).isEqualTo("180.0000000")
    }

    @Test
    fun `searching too small longitude should go to -180`() = runTest {
        viewModel.queryLongitude("-180.0")
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("0.0000000")
        assertThat(uiState.longitude).isEqualTo("-180.0000000")
    }

    @Test
    fun `searching empty longitude should keep current location`() = runTest {
        viewModel.onCameraMoved(1.0, 1.0, 0f)

        viewModel.queryLongitude("")
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("1.0000000")
        assertThat(uiState.longitude).isEqualTo("1.0000000")
    }

    @Test
    fun `only show latitude and longitude to 7 decimal places`() = runTest {
        viewModel.onCameraMoved(1.123456789, 0.123456789, 0f)
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("1.1234568")
        assertThat(uiState.longitude).isEqualTo("0.1234568")
    }

    @Test
    fun `show snack bar if fail to get current location`() = runTest {
        useCase.currentLocation = null

        viewModel.goToMyLocation()
        runCurrent()

        assertThat(viewModel.showCantFindLocationSnackBar).isTrue()
    }

    @Test
    fun `update location when clicking my location button`() = runTest {
        useCase.currentLocation = Location(1.0, 2.0)

        viewModel.goToMyLocation()
        runCurrent()

        assertThat(viewModel.showCantFindLocationSnackBar).isFalse()

        val uiState = viewModel.uiState.value
        assertThat(uiState.latitude).isEqualTo("1.0000000")
        assertThat(uiState.longitude).isEqualTo("2.0000000")
    }

    @Test
    fun `clicking my location should dismiss snack bar if location is found`() = runTest {
        useCase.currentLocation = null
        viewModel.goToMyLocation()
        runCurrent()

        useCase.currentLocation = Location(1.0, 2.0)
        viewModel.goToMyLocation()
        runCurrent()

        assertThat(viewModel.showCantFindLocationSnackBar).isFalse()
    }

    @Test
    fun `zoom into street level after searching an address`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = listOf("Street, City, Country"),
                mapcodes = listOf(
                    Mapcode("AB.CD", Territory.NLD),
                )
            )
        )

        viewModel.queryAddress("Street, City, Country")
        runCurrent()

        assertThat(viewModel.zoom.value).isEqualTo(17f)
    }

    @Test
    fun `zoom into street level after searching a latitude`() = runTest {
        viewModel.queryLatitude("1.0")
        runCurrent()

        assertThat(viewModel.zoom.value).isEqualTo(17f)
    }

    @Test
    fun `zoom into street level after searching a longitude`() = runTest {
        viewModel.queryLongitude("1.0")
        runCurrent()

        assertThat(viewModel.zoom.value).isEqualTo(17f)
    }

    @Test
    fun `use current territory if no mapcode territory specified when searching for mapcode`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = listOf("1 Street, City, Country"),
                mapcodes = listOf(
                    Mapcode("XY.ZA", Territory.NLD),
                    Mapcode("XYZ.ABC", Territory.AAA),
                )
            )
        )
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = listOf("2 Street, City, Country"),
                mapcodes = listOf(
                    Mapcode("AB.CD", Territory.NLD),
                    Mapcode("FGH.JKL", Territory.AAA),
                )
            )
        )

        viewModel.onCameraMoved(0.0, 0.0, 0f)
        runCurrent()

        viewModel.onTerritoryClick() // check that it uses the user's chosen territory
        runCurrent()

        viewModel.queryAddress("FGH.JKL")
        runCurrent()

        assertThat(viewModel.uiState.value.latitude).isEqualTo("1.0000000")
        assertThat(viewModel.uiState.value.longitude).isEqualTo("1.0000000")
    }
}