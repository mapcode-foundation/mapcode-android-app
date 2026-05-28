# Territories Hint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Parse the `territories` hint from the `/mapcode/codes` API response and use it to sort and auto-select the most locally relevant mapcode.

**Architecture:** `ShowMapcodeUseCase.getMapcodes()` becomes a `Flow<List<Mapcode>>` that emits locally-computed mapcodes immediately (phase 1), then emits server-computed mapcodes sorted by the territories hint when the API responds (phase 2). `MapViewModel` collects the flow and applies visual suppression if the displayed mapcode hasn't changed.

**Tech Stack:** Kotlin Flows, `kotlinx.serialization` (already in `build.gradle`), OkHttp (already in `build.gradle`), assertk + JUnit4 for tests.

---

## File Map

| Action | File |
|--------|------|
| Create | `app/src/main/java/com/mapcode/map/ApiMapcodeResponse.kt` |
| Create | `app/src/main/java/com/mapcode/map/MapcodeSorter.kt` |
| Create | `app/src/test/java/com/mapcode/map/MapcodeSorterTest.kt` |
| Modify | `app/src/main/java/com/mapcode/map/ShowMapcodeUseCase.kt` |
| Modify | `app/src/test/java/com/mapcode/map/FakeShowMapcodeUseCase.kt` |
| Modify | `app/src/main/java/com/mapcode/map/MapViewModel.kt` |
| Modify | `app/src/test/java/com/mapcode/map/MapViewModelTest.kt` |

---

## Task 1: API Response Data Classes

**Files:**
- Create: `app/src/main/java/com/mapcode/map/ApiMapcodeResponse.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.mapcode.map

import kotlinx.serialization.Serializable

@Serializable
data class ApiMapcodeResponse(
    val mapcodes: List<ApiMapcode> = emptyList(),
    val territories: List<ApiTerritoryHint>? = null
)

@Serializable
data class ApiMapcode(
    val mapcode: String,
    val territory: String? = null
)

@Serializable
data class ApiTerritoryHint(val alphaCode: String)
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mapcode/map/ApiMapcodeResponse.kt
git commit -m "feat: add API response data classes for territories hint"
```

---

## Task 2: Sorting Function (TDD)

**Files:**
- Create: `app/src/main/java/com/mapcode/map/MapcodeSorter.kt`
- Create: `app/src/test/java/com/mapcode/map/MapcodeSorterTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/mapcode/map/MapcodeSorterTest.kt`:

```kotlin
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
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.mapcode.map.MapcodeSorterTest"
```
Expected: FAILED — `sortMapcodesByHint` is not defined yet.

- [ ] **Step 3: Implement the sorting function**

Create `app/src/main/java/com/mapcode/map/MapcodeSorter.kt`:

```kotlin
package com.mapcode.map

import com.mapcode.Mapcode

fun sortMapcodesByHint(mapcodes: List<Mapcode>, hints: List<String>): List<Mapcode> {
    val hintIndex = hints.withIndex().associate { (i, alphaCode) -> alphaCode to i }
    return mapcodes.sortedWith(
        compareBy(
            { hintIndex[it.territory.name] ?: Int.MAX_VALUE },
            { it.code.length }
        )
    )
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.mapcode.map.MapcodeSorterTest"
```
Expected: BUILD SUCCESSFUL, all 6 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mapcode/map/MapcodeSorter.kt \
        app/src/test/java/com/mapcode/map/MapcodeSorterTest.kt
git commit -m "feat: add mapcode sorting function with territory hint support"
```

---

## Task 3: Update Interface + Implementations

**Files:**
- Modify: `app/src/main/java/com/mapcode/map/ShowMapcodeUseCase.kt`
- Modify: `app/src/test/java/com/mapcode/map/FakeShowMapcodeUseCase.kt`

All three changes in this task must be done together — changing the interface signature breaks both the implementation and the fake simultaneously.

- [ ] **Step 1: Update the interface signature in `ShowMapcodeUseCase.kt`**

Change line 284:
```kotlin
// Before:
fun getMapcodes(lat: Double, long: Double): List<Mapcode>

// After:
fun getMapcodes(lat: Double, long: Double): Flow<List<Mapcode>>
```

- [ ] **Step 2: Replace `ShowMapcodeUseCaseImpl.getMapcodes()` (lines 78–101) with the Flow implementation**

Add a top-level private val above the class declaration (after the existing imports):

```kotlin
private val json = Json { ignoreUnknownKeys = true }
```

Add these imports at the top of the file (inside the existing import block):

```kotlin
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
```

Replace the `getMapcodes` function (lines 78–101):

```kotlin
override fun getMapcodes(lat: Double, long: Double): Flow<List<Mapcode>> = flow {
    emit(MapcodeCodec.encode(lat, long).distinctBy { it.territory })

    try {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("api.mapcode.com")
            .addPathSegment("mapcode")
            .addPathSegment("codes")
            .addPathSegment("$lat,$long")
            .addQueryParameter("client", "android")
            .build()

        val responseBody = withContext(Dispatchers.IO) {
            okHttpClient.newCall(Request.Builder().url(url).build())
                .execute()
                .use { it.body?.string() }
        } ?: return@flow

        val apiResponse = json.decodeFromString<ApiMapcodeResponse>(responseBody)
        val serverMapcodes = apiResponse.mapcodes
            .mapNotNull { it.toLibraryMapcode() }
            .distinctBy { it.territory }

        if (serverMapcodes.isEmpty()) return@flow

        val hints = apiResponse.territories?.map { it.alphaCode } ?: emptyList()
        emit(sortMapcodesByHint(serverMapcodes, hints))
    } catch (e: Exception) {
        Timber.e(e, "Failed to call mapcode API.")
    }
}

private fun ApiMapcode.toLibraryMapcode(): Mapcode? {
    val territory = if (this.territory == null) {
        Territory.AAA
    } else {
        Territory.values().firstOrNull { it.name == this.territory }
            ?: run {
                Timber.w("Unknown territory from API: ${this.territory}")
                null
            }
    }
    return territory?.let { Mapcode(this.mapcode, it) }
}
```

- [ ] **Step 3: Update `FakeShowMapcodeUseCase.getMapcodes()` to return a Flow**

Add these imports at the top of `FakeShowMapcodeUseCase.kt`:

```kotlin
import kotlinx.coroutines.flow.flow
```

Add two new properties after `val favourites`:

```kotlin
val serverMapcodes: MutableMap<Pair<Double, Double>, List<Mapcode>> = mutableMapOf()
val territoryHints: MutableMap<Pair<Double, Double>, List<String>> = mutableMapOf()
```

Replace `getMapcodes` (lines 48–52):

```kotlin
override fun getMapcodes(lat: Double, long: Double): Flow<List<Mapcode>> = flow {
    val localMapcodes = knownLocations
        .find { it.latitude == lat && it.longitude == long }
        ?.mapcodes?.distinctBy { it.territory } ?: emptyList()

    emit(localMapcodes)

    val server = serverMapcodes[Pair(lat, long)] ?: return@flow
    val hints = territoryHints[Pair(lat, long)] ?: emptyList()
    emit(sortMapcodesByHint(server.distinctBy { it.territory }, hints))
}
```

- [ ] **Step 4: Verify everything compiles**

```bash
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin
```
Expected: BUILD SUCCESSFUL (MapViewModel will show a compile error — fix in Task 4).

- [ ] **Step 5: Run existing tests to establish baseline (some may fail — that's expected)**

```bash
./gradlew :app:testDebugUnitTest --tests "com.mapcode.map.MapViewModelTest"
```
Note failures caused by the ViewModel not yet updated. Fix in Task 4.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mapcode/map/ShowMapcodeUseCase.kt \
        app/src/test/java/com/mapcode/map/FakeShowMapcodeUseCase.kt
git commit -m "feat: getMapcodes returns Flow with two-phase local+server emission"
```

---

## Task 4: Update MapViewModel

**Files:**
- Modify: `app/src/main/java/com/mapcode/map/MapViewModel.kt`

- [ ] **Step 1: Add the job field**

After line 101 (`private var getMatchingAddressesJob: Job? = null`), add:

```kotlin
private var updateMapcodesJob: Job? = null
```

- [ ] **Step 2: Replace `updateMapcodes()` (lines 537–547)**

```kotlin
private fun updateMapcodes(lat: Double, long: Double) {
    updateMapcodesJob?.cancel()
    updateMapcodesJob = viewModelScope.launch {
        useCase.getMapcodes(lat, long).collect { newMapcodes ->
            val currentMapcode = mapcodes.value.getOrNull(mapcodeIndex.value)
            val newFirst = newMapcodes.getOrNull(0)

            if (newFirst != null &&
                currentMapcode != null &&
                newFirst.code == currentMapcode.code &&
                newFirst.territory == currentMapcode.territory) {
                return@collect
            }

            mapcodes.value = newMapcodes
            mapcodeIndex.value = if (newMapcodes.isEmpty()) -1 else 0
        }
    }
}
```

Note: the `distinctBy { it.territory }` is now applied inside the Flow in `ShowMapcodeUseCaseImpl` (and `FakeShowMapcodeUseCase`), so it is removed here.

- [ ] **Step 3: Verify it compiles**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run the full test suite**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all existing tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mapcode/map/MapViewModel.kt
git commit -m "feat: collect getMapcodes flow with job cancellation and visual suppression"
```

---

## Task 5: Add ViewModel Tests for Territory Hint Behaviour

**Files:**
- Modify: `app/src/test/java/com/mapcode/map/MapViewModelTest.kt`

Add the following tests to the bottom of `MapViewModelTest` (before the closing `}`):

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test
fun `server mapcodes replace local mapcodes when API responds`() = runTest {
    val localMapcode = Mapcode("AB.CD", Territory.NLD)
    val serverMapcode = Mapcode("XY.ZW", Territory.BEL)

    useCase.knownLocations.add(
        FakeLocation(1.0, 1.0, emptyList(), listOf(localMapcode))
    )
    useCase.serverMapcodes[Pair(1.0, 1.0)] = listOf(serverMapcode)

    viewModel.onCameraMoved(1.0, 1.0, 0f)
    advanceUntilIdle()

    assertThat(viewModel.uiState.value.mapcodeUi.code).isEqualTo("XY.ZW")
    assertThat(viewModel.uiState.value.mapcodeUi.territoryShortName).isEqualTo("BEL")
}

@Test
fun `mapcodes sorted by territory hint when server responds`() = runTest {
    val nld = Mapcode("AB.CD", Territory.NLD)
    val bel = Mapcode("XY.ZW", Territory.BEL)

    useCase.knownLocations.add(
        FakeLocation(1.0, 1.0, emptyList(), listOf(nld))
    )
    useCase.serverMapcodes[Pair(1.0, 1.0)] = listOf(nld, bel)
    useCase.territoryHints[Pair(1.0, 1.0)] = listOf("BEL")

    viewModel.onCameraMoved(1.0, 1.0, 0f)
    advanceUntilIdle()

    assertThat(viewModel.uiState.value.mapcodeUi.territoryShortName).isEqualTo("BEL")
    assertThat(viewModel.uiState.value.mapcodeUi.count).isEqualTo(2)
}

@Test
fun `display not updated when server first mapcode matches current display`() = runTest {
    val mapcode = Mapcode("AB.CD", Territory.NLD)
    val extra = Mapcode("XY.ZW", Territory.BEL)

    useCase.knownLocations.add(
        FakeLocation(1.0, 1.0, emptyList(), listOf(mapcode))
    )
    // Server returns NLD first (hint=[NLD]) — same as local[0], suppression applies
    useCase.serverMapcodes[Pair(1.0, 1.0)] = listOf(mapcode, extra)
    useCase.territoryHints[Pair(1.0, 1.0)] = listOf("NLD")

    viewModel.onCameraMoved(1.0, 1.0, 0f)
    advanceUntilIdle()

    // Suppression: local list was kept; only 1 mapcode visible (local), not 2 (server)
    assertThat(viewModel.uiState.value.mapcodeUi.count).isEqualTo(1)
    assertThat(viewModel.uiState.value.mapcodeUi.code).isEqualTo("AB.CD")
}

@Test
fun `index resets to 0 on server arrival showing hint-sorted first mapcode`() = runTest {
    val nld = Mapcode("AB.CD", Territory.NLD)
    val bel = Mapcode("XY.ZW", Territory.BEL)

    useCase.knownLocations.add(
        FakeLocation(1.0, 1.0, emptyList(), listOf(nld))
    )
    // Server returns both; hint puts BEL first
    useCase.serverMapcodes[Pair(1.0, 1.0)] = listOf(nld, bel)
    useCase.territoryHints[Pair(1.0, 1.0)] = listOf("BEL")

    viewModel.onCameraMoved(1.0, 1.0, 0f)
    advanceUntilIdle()

    // index reset to 0 after server arrives; BEL is at index 0 per hint
    assertThat(viewModel.uiState.value.mapcodeUi.territoryShortName).isEqualTo("BEL")
    assertThat(viewModel.uiState.value.mapcodeUi.number).isEqualTo(1)
}

@Test
fun `latest camera position shown when camera moves before server responds`() = runTest {
    val mapcode1 = Mapcode("AB.CD", Territory.NLD)
    val mapcode2 = Mapcode("XY.ZW", Territory.BEL)

    useCase.knownLocations.add(FakeLocation(1.0, 1.0, emptyList(), listOf(mapcode1)))
    useCase.knownLocations.add(FakeLocation(2.0, 2.0, emptyList(), listOf(mapcode2)))

    viewModel.onCameraMoved(1.0, 1.0, 0f)
    viewModel.onCameraMoved(2.0, 2.0, 0f)
    advanceUntilIdle()

    assertThat(viewModel.uiState.value.mapcodeUi.code).isEqualTo("XY.ZW")
    assertThat(viewModel.uiState.value.mapcodeUi.territoryShortName).isEqualTo("BEL")
}
```

- [ ] **Step 2: Run the new tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.mapcode.map.MapViewModelTest.server mapcodes replace*" \
  --tests "com.mapcode.map.MapViewModelTest.mapcodes sorted*" \
  --tests "com.mapcode.map.MapViewModelTest.display not updated*" \
  --tests "com.mapcode.map.MapViewModelTest.index reset*" \
  --tests "com.mapcode.map.MapViewModelTest.latest camera*"
```
Expected: FAILED (tests not yet added)

- [ ] **Step 3: Add the tests to MapViewModelTest.kt**

Append the five test functions from Step 1 inside the `MapViewModelTest` class, before the closing `}`.

Also add this import if not already present:
```kotlin
import com.mapcode.map.FakeShowMapcodeUseCase
```

- [ ] **Step 4: Run all tests**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/mapcode/map/MapViewModelTest.kt
git commit -m "test: add territory hint sorting and server mapcode replacement tests"
```
