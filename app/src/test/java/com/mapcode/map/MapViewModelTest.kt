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

package com.mapcode.map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import assertk.assertThat
import assertk.assertions.*
import com.mapcode.FakeLocation
import com.mapcode.Mapcode
import com.mapcode.Territory
import com.mapcode.TestDispatcherProvider
import com.mapcode.favourites.Favourite
import com.mapcode.util.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*

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
        Locale.setDefault(Locale.US) // set it to US formatting by default. tests can override it individually
        Dispatchers.setMain(testDispatcher)

        useCase = FakeShowMapcodeUseCase()
        viewModel = MapViewModel(
            useCase,
            dispatchers = TestDispatcherProvider(testDispatcher)
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
                mapcodes = listOf(
                    Mapcode("1AB.XY", Territory.NLD),
                    Mapcode("1CD.YZ", Territory.AAA)
                )
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
                mapcodes = listOf(Mapcode("1AB.XY", Territory.NLD))
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, 0f)
        viewModel.copyMapcode()

        assertThat(useCase.clipboard).isEqualTo("NLD 1AB.XY")
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

        viewModel.onAddressTextChange("address")
        viewModel.onSubmitAddress()
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
    fun `show address not found message and clear address if unable to geocode address`() =
        runTest {
            useCase.knownLocations.clear()

            viewModel.onAddressTextChange("bad address")
            viewModel.onSubmitAddress()
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

        viewModel.onAddressTextChange("NLD AB.CD")
        viewModel.onSubmitAddress()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        val expectedUiState = UiState(
            mapcodeUi = MapcodeUi("AB.CD", "NLD", "Netherlands", 1, 1),
            locationUi = LocationUi(
                latitudeText = "2.0000000",
                latitudePlaceholder = "2.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "3.0000000",
                longitudePlaceholder = "3.0000000",
                showLongitudeInvalidError = false,
            ),
            addressUi = AddressUi(
                "Street, City, 1234AB",
                helper = AddressHelper.Location("City, 1234AB"),
                error = AddressError.None,
                matchingAddresses = emptyList()
            ),
            favouriteLocations = emptyList()
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

        viewModel.onAddressTextChange("street, city")
        viewModel.onSubmitAddress()
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        val expectedUiState = UiState(
            mapcodeUi = MapcodeUi("AB.CD", "NLD", "Netherlands", 1, 1),
            locationUi = LocationUi(
                latitudeText = "2.0000000",
                latitudePlaceholder = "2.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "3.0000000",
                longitudePlaceholder = "3.0000000",
                showLongitudeInvalidError = false,
            ),
            addressUi = AddressUi(
                "Street, City, 1234AB",
                helper = AddressHelper.Location("City, 1234AB"),
                error = AddressError.None,
                matchingAddresses = emptyList()
            ),
            favouriteLocations = emptyList()
        )

        assertThat(uiState).isEqualTo(expectedUiState)
    }

    @Test
    fun `clear mapcode query if can't decode mapcode`() = runTest {
        useCase.knownLocations.clear()

        viewModel.onAddressTextChange("bad mapcode")
        viewModel.onSubmitAddress()
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.addressUi.address).isEmpty()
        assertThat(uiState.addressUi.error).isEqualTo(AddressError.UnknownAddress("bad mapcode"))
    }

    @Test
    fun `show no address found error and empty address if current location has no known address`() =
        runTest {
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
    fun `show no error message and address if the current location has a known address`() =
        runTest {
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
        viewModel.onCameraMoved(2.0, 3.0, 0f)
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "2.0000000",
                latitudePlaceholder = "2.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "3.0000000",
                longitudePlaceholder = "3.0000000",
                showLongitudeInvalidError = false,
            )
        )
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

        viewModel.onAddressTextChange("Street, City")
        viewModel.onSubmitAddress()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.addressUi.error).isEqualTo(AddressError.None)
    }

    @Test
    fun `show unknown address error temporarily`() = runTest {
        useCase.knownLocations.clear()
        viewModel.onAddressTextChange("bad address")
        viewModel.onSubmitAddress()

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
        viewModel.onAddressTextChange("bad address")
        viewModel.onSubmitAddress()

        advanceTimeBy(500) //wait 500ms before doing another query
        viewModel.onAddressTextChange("bad address 2")
        viewModel.onSubmitAddress()

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
        viewModel.onAddressTextChange("Street, City")
        viewModel.onSubmitAddress()

        runCurrent()
        assertThat(viewModel.uiState.value.addressUi.helper).isEqualTo(AddressHelper.NoInternet)
    }

    @Test
    fun `searching an empty address should should dismiss the query and put the old address back`() =
        runTest {
            useCase.knownLocations.add(
                FakeLocation(
                    1.0,
                    1.0,
                    addresses = listOf("Street, City"),
                    mapcodes = listOf(Mapcode("AB.CD", Territory.NLD))
                )
            )

            viewModel.onCameraMoved(1.0, 1.0, 0f) //fill the address with something
            viewModel.onAddressTextChange("")
            viewModel.onSubmitAddress()

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
        assertThat(uiState2.mapcodeUi).isEqualTo(MapcodeUi("HHH.HHH", null, "International", 2, 3))

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
        viewModel.onLatitudeTextChanged("1.0")
        viewModel.onSubmitLatitude()
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "1.0000000",
                latitudePlaceholder = "1.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "0.0000000",
                longitudePlaceholder = "0.0000000",
                showLongitudeInvalidError = false,
            )
        )
    }

    @Test
    fun `searching too large latitude should go to 90`() = runTest {
        viewModel.onLatitudeTextChanged("91.0")
        viewModel.onSubmitLatitude()
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "90.0000000",
                latitudePlaceholder = "90.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "0.0000000",
                longitudePlaceholder = "0.0000000",
                showLongitudeInvalidError = false,
            )
        )
    }

    @Test
    fun `searching too small latitude should go to -90`() = runTest {
        viewModel.onLatitudeTextChanged("-91.0")
        viewModel.onSubmitLatitude()
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "-90.0000000",
                latitudePlaceholder = "-90.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "0.0000000",
                longitudePlaceholder = "0.0000000",
                showLongitudeInvalidError = false,
            )
        )
    }

    @Test
    fun `searching empty latitude should keep current location`() = runTest {
        viewModel.onCameraMoved(1.0, 1.0, 0f)

        viewModel.onLatitudeTextChanged("")
        viewModel.onSubmitLatitude()
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "1.0000000",
                latitudePlaceholder = "1.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "1.0000000",
                longitudePlaceholder = "1.0000000",
                showLongitudeInvalidError = false,
            )
        )
    }

    @Test
    fun `searching longitude should update location`() = runTest {
        viewModel.onCameraMoved(0.0, 0.0, 0f)

        viewModel.onLongitudeTextChanged("1.0")
        viewModel.onSubmitLongitude()
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "0.0000000",
                latitudePlaceholder = "0.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "1.0000000",
                longitudePlaceholder = "1.0000000",
                showLongitudeInvalidError = false,
            )
        )
    }

    @Test
    fun `searching too large longitude should go to 180`() = runTest {
        viewModel.onLongitudeTextChanged("181.0")
        viewModel.onSubmitLongitude()
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "0.0000000",
                latitudePlaceholder = "0.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "180.0000000",
                longitudePlaceholder = "180.0000000",
                showLongitudeInvalidError = false,
            )
        )
    }

    @Test
    fun `searching too small longitude should go to -180`() = runTest {
        viewModel.onLongitudeTextChanged("-180.0")
        viewModel.onSubmitLongitude()
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "0.0000000",
                latitudePlaceholder = "0.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "-180.0000000",
                longitudePlaceholder = "-180.0000000",
                showLongitudeInvalidError = false,
            )
        )
    }

    @Test
    fun `searching empty longitude should keep current location`() = runTest {
        viewModel.onCameraMoved(1.0, 1.0, 0f)

        viewModel.onLongitudeTextChanged("")
        viewModel.onSubmitLongitude()
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "1.0000000",
                latitudePlaceholder = "1.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "1.0000000",
                longitudePlaceholder = "1.0000000",
                showLongitudeInvalidError = false,
            )
        )
    }

    @Test
    fun `only show latitude and longitude to 7 decimal places`() = runTest {
        viewModel.onCameraMoved(1.123456789, 0.123456789, 0f)
        runCurrent()

        val uiState = viewModel.uiState.value
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "1.1234568",
                latitudePlaceholder = "1.1234568",
                showLatitudeInvalidError = false,
                longitudeText = "0.1234568",
                longitudePlaceholder = "0.1234568",
                showLongitudeInvalidError = false,
            )
        )
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
        assertThat(uiState.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "1.0000000",
                latitudePlaceholder = "1.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "2.0000000",
                longitudePlaceholder = "2.0000000",
                showLongitudeInvalidError = false,
            )
        )
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

        viewModel.onAddressTextChange("Street, City, Country")
        viewModel.onSubmitAddress()
        runCurrent()

        assertThat(viewModel.zoom.value).isEqualTo(17f)
    }

    @Test
    fun `zoom into street level after searching a latitude`() = runTest {
        viewModel.onLatitudeTextChanged("1.0")
        viewModel.onSubmitLatitude()
        runCurrent()

        assertThat(viewModel.zoom.value).isEqualTo(17f)
    }

    @Test
    fun `zoom into street level after searching a longitude`() = runTest {
        viewModel.onLongitudeTextChanged("1.0")
        viewModel.onSubmitLongitude()
        runCurrent()

        assertThat(viewModel.zoom.value).isEqualTo(17f)
    }

    @Test
    fun `use current territory if no mapcode territory specified when searching for mapcode`() =
        runTest {
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

            viewModel.onAddressTextChange("FGH.JKL")
            viewModel.onSubmitAddress()
            runCurrent()

            assertThat(viewModel.uiState.value.locationUi).isEqualTo(
                LocationUi(
                    latitudeText = "1.0000000",
                    latitudePlaceholder = "1.0000000",
                    showLatitudeInvalidError = false,
                    longitudeText = "1.0000000",
                    longitudePlaceholder = "1.0000000",
                    showLongitudeInvalidError = false,
                )
            )
        }

    @Test
    fun `do nothing if submitting invalid latitude`() = runTest {
        viewModel.onCameraMoved(0.0, 0.0, 0f)

        viewModel.onLatitudeTextChanged("a")
        viewModel.onSubmitLatitude()
        runCurrent()

        assertThat(viewModel.uiState.value.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "a",
                latitudePlaceholder = "0.0000000",
                showLatitudeInvalidError = true,
                longitudeText = "0.0000000",
                longitudePlaceholder = "0.0000000",
                showLongitudeInvalidError = false,
            )
        )
    }

    @Test
    fun `do nothing if submitting invalid longitude`() = runTest {
        viewModel.onCameraMoved(0.0, 0.0, 0f)

        viewModel.onLongitudeTextChanged("a")
        viewModel.onSubmitLongitude()
        runCurrent()

        assertThat(viewModel.uiState.value.locationUi).isEqualTo(
            LocationUi(
                latitudeText = "0.0000000",
                latitudePlaceholder = "0.0000000",
                showLatitudeInvalidError = false,
                longitudeText = "a",
                longitudePlaceholder = "0.0000000",
                showLongitudeInvalidError = true,
            )
        )
    }

    @Test
    fun `show error if latitude is not a number`() = runTest {
        viewModel.onLatitudeTextChanged("a")
        runCurrent()

        assertThat(viewModel.uiState.value.locationUi.showLatitudeInvalidError).isTrue()
    }

    @Test
    fun `show error if longitude is not a number`() = runTest {
        viewModel.onLongitudeTextChanged("a")
        runCurrent()

        assertThat(viewModel.uiState.value.locationUi.showLongitudeInvalidError).isTrue()
    }

    @Test
    fun `do not show error if latitude is empty`() = runTest {
        viewModel.onLatitudeTextChanged("")
        runCurrent()

        assertThat(viewModel.uiState.value.locationUi.showLatitudeInvalidError).isFalse()
    }

    @Test
    fun `do not show error if longitude is empty`() = runTest {
        viewModel.onLongitudeTextChanged("")
        runCurrent()

        assertThat(viewModel.uiState.value.locationUi.showLongitudeInvalidError).isFalse()
    }

    @Test
    fun `latitude placeholder should show previous value while editing`() = runTest {
        viewModel.onCameraMoved(1.0, 1.0, 0f)
        viewModel.onLatitudeTextChanged("2")
        runCurrent()

        assertThat(viewModel.uiState.value.locationUi.latitudePlaceholder).isEqualTo("1.0000000")
    }

    @Test
    fun `longitude placeholder should show previous value while editing`() = runTest {
        viewModel.onCameraMoved(1.0, 1.0, 0f)
        viewModel.onLongitudeTextChanged("2")
        runCurrent()

        assertThat(viewModel.uiState.value.locationUi.longitudePlaceholder).isEqualTo("1.0000000")
    }

    @Test
    fun `show address dropdown when typing address`() = runTest {
        useCase.matchingAddresses["street"] = listOf("Street 1", "Street 2")

        viewModel.onAddressTextChange("street")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.addressUi.matchingAddresses).containsExactly(
            "Street 1",
            "Street 2"
        )
    }

    @Test
    fun `do not show AAA for international mapcode`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.CD", Territory.AAA))
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, zoom = 1f)
        runCurrent()

        assertThat(viewModel.uiState.value.mapcodeUi.territoryShortName).isNull()
    }

    @Test
    fun `do not copy AAA when copying international mapcodes`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                1.0,
                1.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.CD", Territory.AAA))
            )
        )

        viewModel.onCameraMoved(1.0, 1.0, zoom = 1f)
        runCurrent()
        viewModel.copyMapcode()

        assertThat(useCase.clipboard).isEqualTo("AB.CD")
    }

    @Test
    fun `copy latitude and longitude to clipboard`() = runTest {
        viewModel.onCameraMoved(1.0, 2.0, 1f)
        runCurrent()
        viewModel.copyLocation()

        assertThat(useCase.clipboard).isEqualTo("1,2")
    }

    @Test
    fun `only copy 7 decimal places of latitude and longitude to clipboard`() = runTest {
        viewModel.onCameraMoved(0.123456789, 1.0, 1f)
        runCurrent()
        viewModel.copyLocation()

        assertThat(useCase.clipboard).isEqualTo("0.1234568,1")
    }

    @Test
    fun `do not show error for comma decimal point in latitude`() = runTest {
        Locale.setDefault(Locale.GERMAN)
        viewModel.onLatitudeTextChanged("1,1")
        runCurrent()

        assertThat(viewModel.uiState.value.locationUi.showLatitudeInvalidError).isFalse()
    }

    @Test
    fun `do not show error for comma decimal point in longitude`() = runTest {
        Locale.setDefault(Locale.GERMAN)
        viewModel.onLongitudeTextChanged("1,1")
        runCurrent()

        assertThat(viewModel.uiState.value.locationUi.showLongitudeInvalidError).isFalse()
    }

    @Test
    fun `accept comma decimal points for latitude`() = runTest {
        Locale.setDefault(Locale.GERMAN)
        viewModel.onLatitudeTextChanged("1,1")
        viewModel.onSubmitLatitude()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.locationUi.latitudeText).isEqualTo("1,1000000")
    }

    @Test
    fun `accept comma decimal points for longitude`() = runTest {
        Locale.setDefault(Locale.GERMAN)
        viewModel.onLongitudeTextChanged("1,1")
        viewModel.onSubmitLongitude()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.locationUi.longitudeText).isEqualTo("1,1000000")
    }

    @Test
    fun `make address autocomplete request 1,5 seconds after typing stops`() = runTest {
        useCase.matchingAddresses["address"] = listOf("Street 1")
        viewModel.onAddressTextChange("address")
        advanceTimeBy(1499)
        assertThat(viewModel.uiState.value.addressUi.matchingAddresses).isEmpty()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.addressUi.matchingAddresses).containsExactly("Street 1")
    }

    @Test
    fun `clear matching addresses when clearing address text`() = runTest {
        useCase.matchingAddresses["address"] = listOf("Street 1")
        viewModel.onAddressTextChange("address")
        advanceUntilIdle()
        viewModel.onAddressTextChange("")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.addressUi.matchingAddresses).isEmpty()
    }

    @Test
    fun `do not clear matching addresses if the address query is being continued`() = runTest {
        useCase.matchingAddresses["add"] = listOf("Street 1")
        viewModel.onAddressTextChange("add")
        advanceUntilIdle()
        viewModel.onAddressTextChange("addr")
        assertThat(viewModel.uiState.value.addressUi.matchingAddresses).containsExactly("Street 1")
    }

    @Test
    fun `save favourite when clicking save favourite`() = runTest {
        useCase.knownLocations.add(
            FakeLocation(
                0.0,
                0.0,
                addresses = emptyList(),
                mapcodes = listOf(Mapcode("AB.XY", Territory.NLD))
            )
        )
        viewModel.onCameraMoved(0.0, 0.0, 1.0f)

        viewModel.onSaveFavouriteClick("name")
        advanceUntilIdle()

        assertThat(useCase.favourites).index(0)
            .prop(Favourite::name)
            .isEqualTo("name")

        assertThat(useCase.favourites).index(0)
            .prop(Favourite::location)
            .isEqualTo(Location(0.0, 0.0))
    }
}