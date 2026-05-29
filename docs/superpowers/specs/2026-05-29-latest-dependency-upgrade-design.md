# Design: Upgrade to latest toolchain & dependencies (targetSdk 37)

**Date:** 2026-05-29
**Branch base:** `deps/conservative-update` @ `abbbcb5`
**Status:** Approved approach (Staged with green checkpoints)

## Goal

Move the whole project to the latest **stable** toolchain and libraries, set
`targetSdk`/`compileSdk` to **37**, keep `minSdk` as low as the chosen
dependencies allow, and keep every step revertible to the current state.

## Decisions (locked)

- **minSdk stays 26.** Latest mapcode (`2.4.19`) pulls log4j2, which hard-requires
  API 26+. User chose to keep mapcode latest rather than exclude log4j2 or pin an
  older mapcode. 26 is therefore the floor.
- **Latest = latest stable**, not RC/alpha (e.g. Kotlin `2.3.21`, not `2.4.0-RC2`;
  AGP `9.2.1`, not `9.3.0-alphaXX`).
- **Build JDK = 17** (JDK 17 and 21 both installed; 17 chosen for broad
  compatibility). JVM target bytecode level raised to 17.
- **Strategy = Approach B**: ordered stages, each ending green (build + unit
  tests) and committed for fine-grained rollback.
- **No `git push` without explicit user consent.**

## Target version matrix

| Component | Current | Target (latest stable) |
|---|---|---|
| AGP | 7.4.2 | 9.2.1 |
| Gradle | 7.6.4 | 8.x (latest compatible with AGP 9.2.1) |
| Build JDK | 11 | 17 |
| JVM target | 1.8 | 17 |
| Kotlin | 1.9.24 | 2.3.21 |
| Compose compiler | `kotlinCompilerExtensionVersion 1.5.14` | `org.jetbrains.kotlin.plugin.compose` 2.3.21 |
| KSP | 1.9.24-1.0.20 | 2.3.x (matching Kotlin 2.3.21 / KSP2) |
| serialization plugin | 1.9.24 | 2.3.21 |
| Compose libraries | pinned 1.5.4 | Compose BOM 2026.05.01 (unpin individual versions) |
| compose-destinations | 1.9.63 | 2.3.0 (breaking API) |
| Hilt | 2.48 | 2.59.2 |
| hilt-navigation-compose | 1.1.0 | latest |
| accompanist (permissions, systemuicontroller) | 0.32.0 | latest |
| lifecycle / activity / navigation / core-ktx | 2.7.0 / 1.8.2 / 2.7.7 / 1.12.0 | latest stable |
| material (com.google.android.material) | 1.11.0 | latest stable |
| maps-compose / places / places-ktx | 2.11.4 / 3.5.0 / 3.1.1 | latest stable |
| play-services-maps / -location | 18.2.0 / 21.3.0 | latest stable |
| okhttp | 4.12.0 | latest stable (4.12.0 or 5.x — decide at stage 7) |
| kotlinx-serialization-json | 1.6.3 | latest stable |
| datastore-preferences | 1.0.0 | latest stable |
| mapcode | 2.4.19 | 2.4.19 (already latest) |
| Test libs (mockito, espresso, coroutines-test, ext-junit, rules) | mixed | latest stable |
| compileSdk / targetSdk | 34 / 34 | 37 / 37 |
| minSdk | 26 | 26 (unchanged) |

> Exact patch versions for "latest stable" are resolved at implementation time
> from Maven/Google Maven metadata, preferring stable over pre-release.

## Staged plan (each stage = green build + commit)

**Stage 0 — Prep & revert anchor**
- Tag current commit: `git tag pre-latest-upgrade abbbcb5`.
- Create working branch `deps/latest-upgrade` off `abbbcb5`.
- Point Gradle at JDK 17 (via `org.gradle.java.home` in a local, non-committed
  `gradle.properties` override, or `JAVA_HOME` for the build invocation).
- Acceptance: `assembleDebug` still green on JDK 17 with current versions.

**Stage 1 — AGP 7.4 → 8.latest + Gradle 8**
- Bump AGP to latest 8.x and Gradle wrapper to its required 8.x.
- Apply AGP 8 required changes: explicit `buildFeatures.buildConfig` if BuildConfig
  is used; `packaging {}` already present; verify `namespace` (already set).
- Acceptance: `assembleDebug` + `testDebugUnitTest` green.

**Stage 2 — AGP 8 → 9.2.1**
- Use the `agp-9-upgrade` skill to drive AGP 9 migration (removed DSL, defaults).
- Acceptance: `assembleDebug` + `testDebugUnitTest` green; `assembleRelease` (R8) green.

**Stage 3 — Kotlin 1.9.24 → 2.3.21 (K2)**
- Add `org.jetbrains.kotlin.plugin.compose` (version 2.3.21); remove
  `composeOptions.kotlinCompilerExtensionVersion`.
- Bump `org.jetbrains.kotlin.android` and `plugin.serialization` to 2.3.21.
- Bump KSP to the matching 2.3.x.
- Decide Hilt processor: keep `kapt`, or migrate Hilt to KSP (preferred for K2).
  Default: migrate Hilt `kapt` → `ksp` (drop `kotlin-kapt` if no other kapt users).
- Acceptance: `assembleDebug` + `testDebugUnitTest` green.

**Stage 4 — Compose BOM + AndroidX libs + compose-destinations 2.x**
- Replace pinned Compose versions with Compose BOM `2026.05.01` (versionless
  Compose artifacts).
- Bump accompanist, lifecycle, activity-compose, navigation-compose, core-ktx,
  material to latest stable.
- Migrate compose-destinations 1.9.63 → 2.3.0 across ~6 files
  (`@Destination` → typed `@Destination<RootGraph>`, `NavGraphs`/`DestinationsNavHost`
  API, plus 2 instrumented tests).
- Re-verify the foundation Pager onboarding code against the BOM's Compose version.
- Bump Hilt to 2.59.2 + hilt-navigation-compose latest.
- Acceptance: `assembleDebug` + `testDebugUnitTest` green.

**Stage 5 — compileSdk / targetSdk → 37**
- Set `compileSdk 37`, `targetSdk 37` (SDK 37 platform + build-tools 37.0.0 are
  installed locally).
- Address any new API-level lint/behavior changes surfaced by targetSdk 37.
- Acceptance: `assembleDebug` + `assembleRelease` + `testDebugUnitTest` green;
  `lintVitalRelease` reviewed.

**Stage 6 — Remaining libraries to latest**
- okhttp (decide 4.12.0 vs 5.x), maps-compose, places, places-ktx,
  play-services-*, kotlinx-serialization-json, datastore, all test libs.
- Acceptance: full `assembleDebug assembleRelease testDebugUnitTest` green.

**Stage 7 — Final verification**
- Full clean build (debug + release), all unit tests, summarize results.
- Manual smoke check of onboarding pager + navigation (compose-destinations) on
  device/emulator (user-driven; flagged as the highest-risk runtime areas).

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| AGP 7→9 is two majors; removed DSL/APIs | Stage through AGP 8 first; use `agp-9-upgrade` skill for the 8→9 step |
| Kotlin 2 Compose compiler plugin migration | Dedicated stage 3; add compose plugin, remove `kotlinCompilerExtensionVersion` |
| KSP2 behavior differences | Verify codegen (Hilt, compose-destinations) builds at stage 3/4 |
| compose-destinations 2.x breaking API | Isolated stage 4; only ~6 files; covered by instrumented tests |
| targetSdk 37 runtime behavior changes | Stage 5 + manual smoke test; review `lintVitalRelease` |
| Hilt kapt vs KSP under K2 | If KSP migration fails, fall back to keeping `kapt` (still K2-compatible) |
| A stage breaks irrecoverably | Previous stage commit + `pre-latest-upgrade` tag allow exact revert |

## Verification (every stage unless noted)
- `./gradlew assembleDebug testDebugUnitTest` green.
- Release/R8 (`assembleRelease`) at stages 2, 5, 6, 7.
- Baseline to beat: 88 unit tests passing, debug + release build green.

## Out of scope
- No feature changes, no unrelated refactors.
- No `okhttp` 5.x unless trivially clean (decided at stage 6).
- No CI/publishing changes.
