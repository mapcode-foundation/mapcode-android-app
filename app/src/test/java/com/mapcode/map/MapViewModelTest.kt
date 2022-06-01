package com.mapcode.map

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.mapcode.Mapcode
import com.mapcode.Territory
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Created by sds100 on 01/06/2022.
 */
internal class MapViewModelTest {

    private lateinit var mockShowMapcodeUseCase: ShowMapcodeUseCase
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        mockShowMapcodeUseCase = mock()
        viewModel = MapViewModel(mockShowMapcodeUseCase)
    }

    @Test
    fun `update mapcode when camera moves`() {
        val fakeMapcodes = listOf(Mapcode("1AB.XY", Territory.AAA), Mapcode("1CD.YZ", Territory.NLD))
        whenever(mockShowMapcodeUseCase.getMapcodes(1.0, 1.0)).thenReturn(fakeMapcodes)

        viewModel.onCameraMoved(1.0, 1.0)
        assertThat(viewModel.mapcodeInfoState.value.mapcode).isEqualTo("1AB.XY")
    }
}