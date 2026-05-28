package com.mapcode.map

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.mapcode.Mapcode
import com.mapcode.Territory
import org.junit.Test

class MapcodeSorterTest {

    @Test
    fun `hint-matching territory comes before non-matching`() {
        val nld = Mapcode("AB.CD", Territory.NLD)     // len 5
        val intl = Mapcode("ABCDE.FG", Territory.AAA) // len 8

        val result = sortMapcodesByHint(listOf(intl, nld), listOf("NLD"))

        assertThat(result).containsExactly(nld, intl)
    }

    @Test
    fun `hint order is preserved for multiple matching territories`() {
        val nld = Mapcode("AB.CD", Territory.NLD)
        val bel = Mapcode("XY.ZW", Territory.BEL)
        val intl = Mapcode("ABCDE.FG", Territory.AAA)

        val result = sortMapcodesByHint(listOf(nld, intl, bel), listOf("BEL", "NLD"))

        assertThat(result).containsExactly(bel, nld, intl)
    }

    @Test
    fun `non-matching territories sorted by code length ascending`() {
        val deu = Mapcode("AB.CD", Territory.DEU)       // len 5
        val bel = Mapcode("XYZ.WV", Territory.BEL)     // len 6
        val intl = Mapcode("ABCDE.FG", Territory.AAA)  // len 8

        val result = sortMapcodesByHint(listOf(intl, bel, deu), emptyList())

        assertThat(result).containsExactly(deu, bel, intl)
    }

    @Test
    fun `hint territory absent from mapcode list has no effect`() {
        val nld = Mapcode("AB.CD", Territory.NLD)
        val bel = Mapcode("XY.ZW", Territory.BEL)

        // DEU is in hints but not in list; NLD should still come first
        val result = sortMapcodesByHint(listOf(bel, nld), listOf("DEU", "NLD"))

        assertThat(result).containsExactly(nld, bel)
    }

    @Test
    fun `empty input returns empty list`() {
        val result = sortMapcodesByHint(emptyList(), listOf("NLD"))
        assertThat(result).isEmpty()
    }

    @Test
    fun `empty hint sorts all by code length`() {
        val long = Mapcode("ABCDE.FG", Territory.NLD)  // len 8
        val short = Mapcode("AB.CD", Territory.BEL)    // len 5

        val result = sortMapcodesByHint(listOf(long, short), emptyList())

        assertThat(result).containsExactly(short, long)
    }

    @Test
    fun `subdivision territory hint matched correctly with dash format`() {
        val usCa = Mapcode("AB.CD", Territory.US_CA) // California — toString() returns "US-CA"
        val intl = Mapcode("ABCDE.FG", Territory.AAA)

        // The API returns territory strings with dashes (e.g. "US-CA"), matching Territory.toString()
        val hint = usCa.territory.toString()
        val result = sortMapcodesByHint(listOf(intl, usCa), listOf(hint))

        assertThat(result).containsExactly(usCa, intl)
    }
}
