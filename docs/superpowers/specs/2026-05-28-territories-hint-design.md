# Territories Hint: API-Driven Mapcode Sorting & Selection

**Date:** 2026-05-28
**Status:** Approved

## Overview

The REST API endpoint `/mapcode/codes/{lat},{lon}` may return a `territories` element — a ranked list of territories the coordinates are most likely in. This spec describes how the app uses that hint to sort the mapcode list and auto-select the most relevant ("local") mapcode.

## Background

The app currently calls the API but discards the response body. All mapcode data comes from the local `MapcodeCodec.encode()` library. This feature adds full API response parsing, replaces local mapcodes with server mapcodes when available, and uses the `territories` hint to determine sort order and initial selection.

## API Response Model

The endpoint returns a JSON body. Relevant fields:

```json
{
  "mapcodes": [
    { "mapcode": "WH6.3V", "territory": "NLD" },
    { "mapcode": "VHXGB.5VBH" }
  ],
  "territories": [
    { "alphaCode": "NLD" }
  ]
}
```

- `mapcodes`: list of mapcode objects. `territory` is absent for international mapcodes.
- `territories`: optional. List of territories the coordinates are most likely in, in priority order.

New Kotlin data classes (using `kotlinx.serialization`, already a project dependency):

```kotlin
@Serializable
data class ApiMapcodeResponse(
    val mapcodes: List<ApiMapcode>,
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

## Sorting Contract

Applied after `distinctBy { it.territory }` (which keeps the highest-priority mapcode per territory, same as current behaviour):

1. **Hint-matching territories** — sorted by their index in the `territories` list (ascending).
2. **Non-matching territories** — sorted by `mapcode` string length (ascending), appended after group 1.
3. If `territories` is absent or empty, all mapcodes fall into rule 2 (sorted by length).

Example:
- Hint: `[NLD]`
- Input after dedup: `[AAA international (len 10), NLD short (len 6), BEL (len 7)]`
- Output: `[NLD short, BEL, AAA international]`

## Two-Phase Flow

`ShowMapcodeUseCase.getMapcodes()` changes from returning `List<Mapcode>` to returning `Flow<List<Mapcode>>`.

| Phase | Trigger | Mapcodes source | Index |
|---|---|---|---|
| 1 — Immediate | On call | Local `MapcodeCodec.encode()`, unsorted | 0 |
| 2 — Server | API response arrives | Server mapcodes, sorted by hint | 0 |
| 2 — Server failure | API call throws | No emission; phase 1 list stays | unchanged |

`MapViewModel.updateMapcodes()` collects this flow. On each emission it replaces `mapcodes` and resets `mapcodeIndex` to 0.

## Visual Suppression

Before applying the phase-2 update, compare `newServerList[0].code` and `newServerList[0].territory` with the currently displayed mapcode's code and territory. If both fields match, skip the StateFlow update — the display is already correct and an unnecessary recomposition is avoided.

## Index Reset Behaviour

`mapcodeIndex` always resets to 0 when server mapcodes arrive, even if the user has already tapped to navigate to a different territory. The server-determined local territory is always the starting selection.

## Error & Edge Cases

| Case | Behaviour |
|---|---|
| API unavailable / timeout | Phase 1 (local) mapcodes remain; no reorder |
| `territories` absent in response | Server mapcodes used; sorted by length only |
| Territory in hint not present in mapcode list | Hint entry ignored; no effect on sort |
| Server mapcode at index 0 equals current display | No StateFlow update; no visual change |
| Empty mapcode list from server | Fall back to phase-1 local list |

## Files Affected

| File | Change |
|---|---|
| `map/ShowMapcodeUseCase.kt` | Parse API response; return `Flow<List<Mapcode>>` |
| `map/MapViewModel.kt` | Collect flow; apply visual suppression check; reset index |
| `map/` (new file) | `ApiMapcodeResponse`, `ApiMapcode`, `ApiTerritoryHint` data classes |
| `map/` (new file or inline) | Sorting function |
