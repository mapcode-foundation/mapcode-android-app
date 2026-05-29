# Latest Toolchain & Dependency Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the project to the latest stable toolchain and libraries with `compileSdk`/`targetSdk` 37, `minSdk` held at 26, in revertible green stages.

**Architecture:** Ordered stages (Approach B from the spec). Each stage edits Gradle/config (and code where an API breaks), ends with a green `assembleDebug` + `testDebugUnitTest` (and R8 at key stages), and a commit. Any stage can be reverted to the prior commit or to the `pre-latest-upgrade` tag.

**Tech Stack:** AGP 9.2.1, Gradle 8.14.5, JDK 17, Kotlin 2.3.21 (K2 + Compose compiler plugin), KSP2, Compose BOM 2026.05.01, Hilt 2.59.2 (KSP), compose-destinations 2.3.0.

**Spec:** `docs/superpowers/specs/2026-05-29-latest-dependency-upgrade-design.md`

> **Ordering note (refines spec):** `compileSdk`/`targetSdk` 37 is moved before the AndroidX library bumps, because lifecycle 2.10 / activity 1.13 / core-ktx 1.18 / material 1.14 require compileSdk ≥ 36.

> **RUNTIME CORRECTION (2026-05-29, discovered during Stage 2 execution):** AGP 9.2.1 requires Gradle ≥ 9.1, and Kotlin Gradle Plugin 1.9.24's kapt is incompatible with Gradle 9 (`NoSuchMethodError` in `Kapt3GradleSubplugin`). AGP 9 also pins/forces Kotlin 2.x (KGP BOM 2.2.10; downgrade floor 2.0.0). **Therefore the Kotlin 2.x + Compose-compiler-plugin migration MUST land before AGP 9.** Corrected stage order: **Stage 1** AGP 8.13.2+Gradle 8.14.5 (done) → **Stage 2** Kotlin 2.3.21 + Compose plugin + KSP2 + Hilt→KSP (on AGP 8.13.2/Gradle 8.14.5) → **Stage 3** AGP 9.2.1 + Gradle 9.5.1 → **Stage 4** compileSdk/targetSdk 37 → **Stage 5** Compose BOM + AndroidX latest + migrations → **Stage 6** remaining libs → **Stage 7** verify. (The "Stage N" headings below predate this swap; follow the corrected order: the old "Stage 4: Kotlin" runs as Stage 2, the old "Stage 2: AGP 9" runs as Stage 3.)
>
> AGP 9 specifics flagged for the AGP-9 stage: keep the `org.jetbrains.kotlin.android` plugin by setting `android.builtInKotlin=false` and `android.newDsl=false` in `gradle.properties` (the latter keeps the legacy `applicationVariants.all {}` KSP-srcDir block working); `kotlin-kapt` must be gone by then (Hilt already on KSP from Stage 2).

> **Version resolution:** Pinned versions below are the latest stable at planning time (2026-05-29). For trailing libraries marked "resolve", confirm the latest stable at execution with:
> `curl -s <metadata-url> | grep -oE '<release>[^<]+'` and prefer stable over `-alpha/-beta/-rc`.

> **Baseline to preserve at every stage:** debug + release build green, **88 unit tests passing**.

---

## Stage 0: Prep & revert anchor

**Files:**
- Create (local, not committed): `gradle.properties` override OR use `JAVA_HOME` per-invocation
- No source changes

- [ ] **Step 1: Confirm clean tree on the conservative-update commit**

Run: `git status -s && git rev-parse --short HEAD`
Expected: clean tree; HEAD at `53a0061` (spec) or `abbbcb5`.

- [ ] **Step 2: Tag the current working state for revert**

```bash
git tag pre-latest-upgrade abbbcb5
git tag -l pre-latest-upgrade
```
Expected: prints `pre-latest-upgrade`. (Revert anytime with `git checkout pre-latest-upgrade`.)

- [ ] **Step 3: Create the upgrade branch**

```bash
git checkout -b deps/latest-upgrade
```
Expected: `Switched to a new branch 'deps/latest-upgrade'`.

- [ ] **Step 4: Point Gradle at JDK 17**

Verify path: `/usr/libexec/java_home -v 17` (e.g. `.../jbr-17.0.14/Contents/Home`).
Use it for all builds in this plan by prefixing commands with:
`JAVA_HOME="$(/usr/libexec/java_home -v 17)"` — e.g. `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew ...`

- [ ] **Step 5: Verify current build is still green on JDK 17**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew assembleDebug testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 88 tests pass.
> If it fails only because Gradle 7.6.4 rejects JDK 17, proceed to Stage 1 (Gradle 8 supports JDK 17) and re-run this verification there.

- [ ] **Step 6: Commit (branch marker only — no changes expected)**

If Step 4 created a committed file, commit it; otherwise skip. Keep `gradle.properties` JDK overrides **out** of git (machine-specific).

---

## Stage 1: AGP 7.4.2 → 8.13.2 + Gradle 8.14.5

**Files:**
- Modify: `build.gradle:7-8` (AGP plugin versions)
- Modify: `gradle/wrapper/gradle-wrapper.properties:3` (distributionUrl)
- Modify: `app/build.gradle` (add `buildFeatures.buildConfig` if BuildConfig is used)

- [ ] **Step 1: Bump Gradle wrapper to 8.14.5**

In `gradle/wrapper/gradle-wrapper.properties`, set:
`distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.5-bin.zip`

- [ ] **Step 2: Bump AGP to 8.13.2**

In `build.gradle`:
```groovy
    id 'com.android.application' version '8.13.2' apply false
    id 'com.android.library' version '8.13.2' apply false
```

- [ ] **Step 3: Add explicit buildConfig flag (AGP 8 no longer generates it by default)**

In `app/build.gradle` `buildFeatures { ... }` add `buildConfig true`:
```groovy
    buildFeatures {
        compose true
        buildConfig true
    }
```

- [ ] **Step 4: Build & test**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew assembleDebug testDebugUnitTest --console=plain 2>&1 | tail -30`
Expected: `BUILD SUCCESSFUL`, 88 tests pass.
> Common AGP 8 fixes if it fails: remove obsolete `packagingOptions` spelling (already `packagingOptions { resources {...} }` — fine), ensure `namespace` set (already `com.mapcode`).

- [ ] **Step 5: Commit**

```bash
git add build.gradle gradle/wrapper/gradle-wrapper.properties app/build.gradle
git commit -m "Stage 1: AGP 8.13.2 + Gradle 8.14.5 (JDK 17)"
```

---

## Stage 2: AGP 8.13.2 → 9.2.1

**Files:**
- Modify: `build.gradle:7-8` (AGP versions)
- Modify: `gradle/wrapper/gradle-wrapper.properties` (only if AGP 9.2.1 requires a newer Gradle)
- Possibly: `app/build.gradle`, `gradle.properties` (AGP 9 removed-API fixes)

- [ ] **Step 1: Invoke the agp-9-upgrade skill**

Use the `agp-9-upgrade` skill to perform the AGP 8 → 9 migration. Apply its recommended edits (removed DSL, changed defaults, `gradle.properties` flags).

- [ ] **Step 2: Set AGP 9.2.1**

In `build.gradle`:
```groovy
    id 'com.android.application' version '9.2.1' apply false
    id 'com.android.library' version '9.2.1' apply false
```

- [ ] **Step 3: Bump Gradle only if required**

Run the build (Step 4). If AGP errors that it needs Gradle ≥ a version newer than 8.14.5, set `gradle-wrapper.properties` `distributionUrl` to that version (e.g. `gradle-9.5.1-bin.zip`) and re-run.

- [ ] **Step 4: Build, test, and R8**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew assembleDebug testDebugUnitTest assembleRelease --console=plain 2>&1 | tail -40`
Expected: `BUILD SUCCESSFUL`, 88 tests pass, release (R8) succeeds.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Stage 2: AGP 9.2.1"
```

---

## Stage 3: compileSdk / targetSdk → 37

**Files:**
- Modify: `app/build.gradle:14` (`compileSdk`), `app/build.gradle:19` (`targetSdk`)

- [ ] **Step 1: Set compileSdk and targetSdk to 37**

In `app/build.gradle`:
```groovy
    compileSdk 37
    ...
        targetSdk 37
```
(SDK platform `android-37.0` and build-tools `37.0.0` are already installed.)

- [ ] **Step 2: Build, test, R8, and lint-vital**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew assembleDebug testDebugUnitTest assembleRelease lintVitalRelease --console=plain 2>&1 | tail -40`
Expected: `BUILD SUCCESSFUL`, 88 tests pass.
> If `lintVitalRelease` reports new errors from targetSdk 37 behavior changes, fix the flagged code or, if a check is a false positive, add a scoped lint suppression. Do not blanket-disable lint.

- [ ] **Step 3: Commit**

```bash
git add app/build.gradle
git commit -m "Stage 3: compileSdk/targetSdk 37"
```

---

## Stage 4: Kotlin 1.9.24 → 2.3.21 (K2) + Compose compiler plugin + KSP2 + Hilt→KSP

**Files:**
- Modify: `build.gradle` (Kotlin, KSP, serialization, add Compose compiler plugin; bump Hilt gradle plugin)
- Modify: `app/build.gradle` (plugins block: add compose plugin, swap `kapt`→`ksp` for Hilt, drop `kotlin-kapt`; remove `composeOptions`)

- [ ] **Step 1: Update root plugin versions in `build.gradle`**

```groovy
plugins {
    id 'com.android.application' version '9.2.1' apply false
    id 'com.android.library' version '9.2.1' apply false
    id 'org.jetbrains.kotlin.android' version '2.3.21' apply false
    id 'org.jetbrains.kotlin.plugin.compose' version '2.3.21' apply false
    id 'com.google.android.libraries.mapsplatform.secrets-gradle-plugin' version '2.0.1' apply false
    id 'com.google.devtools.ksp' version '2.3.9' // verify matches Kotlin 2.3.21 at build time
    id 'org.jetbrains.kotlin.plugin.serialization' version '2.3.21'
}
```
And bump the Hilt classpath in the `buildscript` block:
```groovy
        classpath 'com.google.dagger:hilt-android-gradle-plugin:2.59.2'
```

- [ ] **Step 2: Update `app/build.gradle` plugins block**

Add the Compose compiler plugin, remove `kotlin-kapt`, keep `kotlin-parcelize`:
```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.plugin.compose'
    id 'dagger.hilt.android.plugin'
    id 'com.google.android.libraries.mapsplatform.secrets-gradle-plugin'
    id 'com.google.devtools.ksp'
    id 'org.jetbrains.kotlin.plugin.serialization'
    id 'kotlin-parcelize'
}
```

- [ ] **Step 3: Remove `composeOptions` (compiler version now comes from the plugin)**

In `app/build.gradle`, delete:
```groovy
    composeOptions {
        kotlinCompilerExtensionVersion '1.5.14'
    }
```

- [ ] **Step 4: Switch Hilt compiler from kapt to KSP**

In `app/build.gradle` dependencies, replace:
```groovy
    kapt "com.google.dagger:hilt-compiler:2.48"
```
with:
```groovy
    ksp "com.google.dagger:hilt-compiler:2.59.2"
```
Also bump `implementation "com.google.dagger:hilt-android:2.59.2"`.

- [ ] **Step 5: Raise JVM target to 17**

In `app/build.gradle`:
```groovy
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
        freeCompilerArgs += [
                "-opt-in=kotlin.RequiresOptIn"
        ]
    }
```

- [ ] **Step 6: Build & test**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew assembleDebug testDebugUnitTest --console=plain 2>&1 | tail -40`
Expected: `BUILD SUCCESSFUL`, 88 tests pass.
> If KSP errors that `2.3.9` is incompatible with Kotlin 2.3.21, set the KSP version to the one whose metadata lists Kotlin 2.3.21 support and re-run.
> If Hilt KSP codegen fails, fall back: re-add `id 'kotlin-kapt'` and use `kapt "com.google.dagger:hilt-compiler:2.59.2"` (still K2-compatible). Record the fallback in the commit message.

- [ ] **Step 7: Commit**

```bash
git add build.gradle app/build.gradle
git commit -m "Stage 4: Kotlin 2.3.21 (K2) + Compose plugin + KSP2 + Hilt on KSP"
```

---

## Stage 5: Compose BOM + AndroidX latest + accompanist + compose-destinations 2.x

**Files:**
- Modify: `app/build.gradle` (Compose BOM, AndroidX libs, accompanist, compose-destinations, Hilt nav)
- Modify: `app/src/main/java/com/mapcode/MapcodeNavHost.kt` (systemuicontroller migration + destinations API)
- Modify: `app/src/main/java/com/mapcode/map/MapScreen.kt`, `favourites/FavouritesScreen.kt`, `onboarding/OnboardingScreen.kt` (destinations annotations)
- Modify: `app/src/androidTest/java/com/mapcode/map/MapScreenTest.kt`, `favourites/FavouritesScreenTest.kt` (destinations test API)

- [ ] **Step 1: Adopt the Compose BOM and unpin Compose artifacts**

In `app/build.gradle` replace the individually-pinned Compose lines with the BOM + versionless artifacts:
```groovy
    implementation platform('androidx.compose:compose-bom:2026.05.01')
    androidTestImplementation platform('androidx.compose:compose-bom:2026.05.01')
    implementation "androidx.compose.ui:ui"
    implementation "androidx.compose.ui:ui-tooling-preview"
    implementation 'androidx.compose.material:material'
    implementation "androidx.compose.foundation:foundation"
    implementation "androidx.compose.material3:material3-window-size-class"
    implementation "androidx.compose.material:material-icons-extended"
    debugImplementation "androidx.compose.ui:ui-tooling"
    debugImplementation "androidx.compose.ui:ui-test-manifest"
    androidTestImplementation "androidx.compose.ui:ui-test-junit4"
```

- [ ] **Step 2: Bump AndroidX runtime libs to latest stable**

In `app/build.gradle`:
```groovy
    implementation 'androidx.core:core-ktx:1.18.0'
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0"
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.10.0'
    implementation 'androidx.activity:activity-compose:1.13.0'
    implementation "androidx.navigation:navigation-compose:2.9.8"
    implementation "com.google.android.material:material:1.14.0"
    implementation "androidx.core:core-splashscreen:1.0.1"
    implementation 'androidx.hilt:hilt-navigation-compose:1.3.0'
```

- [ ] **Step 3: Bump accompanist-permissions, remove accompanist-systemuicontroller**

In `app/build.gradle`:
```groovy
    implementation 'com.google.accompanist:accompanist-permissions:0.37.3' // resolve latest stable
```
Delete the `accompanist-systemuicontroller` line.

- [ ] **Step 4: Migrate systemuicontroller usage to androidx (`MapcodeNavHost.kt`)**

Replace `rememberSystemUiController()` usage. Remove:
```kotlin
import com.google.accompanist.systemuicontroller.rememberSystemUiController
```
Read `MapcodeNavHost.kt` to see how the controller is used (status-bar color / icons). Replace with the platform API: obtain the window via `LocalView.current` + `(LocalView.current.context as Activity).window`, and use `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = ...`. For status-bar color under edge-to-edge, prefer drawing behind the system bar. Implement the equivalent of the previous behavior; verify by compile + manual check in Stage 7.

- [ ] **Step 5: Migrate compose-destinations 1.9.63 → 2.3.0**

In `app/build.gradle`:
```groovy
    implementation 'io.github.raamcosta.compose-destinations:core:2.3.0'
    ksp 'io.github.raamcosta.compose-destinations:ksp:2.3.0'
```
Apply the 2.x API changes:
- `@Destination` → `@Destination<RootGraph>` (typed graph). For the onboarding start screen, `@Destination<RootGraph>(start = true)`.
- Update `MapcodeNavHost.kt`: `DestinationsNavHost(navGraph = NavGraphs.root)` and any `NavGraphs`/`startRoute` references to the 2.x generated API.
- Update the two instrumented tests that reference destinations (`MapScreenTest.kt`, `FavouritesScreenTest.kt`) to the 2.x navigation/test API.
Read each of the 6 files and adapt to compiler/KSP errors; the public destination function signatures stay the same, only annotations and NavHost wiring change.

- [ ] **Step 6: Build & test**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew assembleDebug testDebugUnitTest --console=plain 2>&1 | tail -40`
Expected: `BUILD SUCCESSFUL`, 88 tests pass.
> If a versionless Compose artifact isn't covered by the BOM (e.g. material-icons-extended), pin it explicitly to the version the BOM aligns to (check the build error's suggested version).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Stage 5: Compose BOM 2026.05.01 + AndroidX latest + accompanist/systemuicontroller migration + compose-destinations 2.3.0"
```

---

## Stage 6: Remaining libraries to latest

**Files:**
- Modify: `app/build.gradle` (maps-compose, places, places-ktx, play-services, serialization-json, datastore, test libs)
- Possibly: `app/src/main/java/com/mapcode/map/MapScreen.kt` and map-related files (maps-compose/places API changes)

- [ ] **Step 1: Bump non-map libs (low risk)**

In `app/build.gradle`:
```groovy
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0"
    implementation "androidx.datastore:datastore-preferences:1.2.1"
    testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0'
```
Bump play-services to latest stable (resolve):
```groovy
    implementation 'com.google.android.gms:play-services-maps:LATEST'   // resolve
    implementation 'com.google.android.gms:play-services-location:LATEST' // resolve
```
Keep okhttp at `4.12.0` (locked — do not move to 5.x).

- [ ] **Step 2: Build & test (checkpoint before the risky map bump)**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew assembleDebug testDebugUnitTest --console=plain 2>&1 | tail -30`
Expected: `BUILD SUCCESSFUL`, 88 tests pass.

- [ ] **Step 3: Commit the safe bumps**

```bash
git add app/build.gradle
git commit -m "Stage 6a: serialization-json, datastore, coroutines-test, play-services latest"
```

- [ ] **Step 4: Bump maps-compose / places / places-ktx (HIGH RISK — major versions)**

In `app/build.gradle`:
```groovy
    implementation 'com.google.maps.android:maps-compose:8.3.0'
    implementation 'com.google.android.libraries.places:places:5.2.0'
    implementation 'com.google.maps.android:places-ktx:5.0.0'
```
These cross major versions (maps-compose 2→8, places 3→5). Read `MapScreen.kt` and adapt to compile errors (Places API request/response classes and maps-compose composable signatures changed across majors).

- [ ] **Step 5: Build & test**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew assembleDebug testDebugUnitTest --console=plain 2>&1 | tail -40`
Expected: `BUILD SUCCESSFUL`, 88 tests pass.
> **Fallback if the map API migration is too invasive:** step back to the highest maps-compose/places major that compiles with only minor code changes (e.g. maps-compose 4.x/6.x), record the chosen versions and the reason in the commit message, and flag for follow-up. Getting these to absolute-latest is explicitly allowed to stop short here.

- [ ] **Step 6: Bump test libs to latest stable (resolve each)**

In `app/build.gradle`, bump: `mockito-core`, `mockito-inline` (or migrate to `mockito-core` inline-by-default), `mockito-kotlin`, `mockito-android`, `androidx.test:core-ktx`, `androidx.test.ext:junit`, `androidx.test.espresso:espresso-core`, `androidx.test:rules`, `assertk-jvm`. Resolve each latest stable before setting.

- [ ] **Step 7: Build, test, R8**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew assembleDebug testDebugUnitTest assembleRelease --console=plain 2>&1 | tail -40`
Expected: `BUILD SUCCESSFUL`, 88 tests pass, R8 succeeds.

- [ ] **Step 8: Commit**

```bash
git add app/build.gradle app/src
git commit -m "Stage 6b: maps-compose/places latest + test libs latest"
```

---

## Stage 7: Final verification

**Files:** none (verification only)

- [ ] **Step 1: Clean full build + tests**

Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew clean assembleDebug assembleRelease testDebugUnitTest --console=plain 2>&1 | tail -40`
Expected: `BUILD SUCCESSFUL`; release (R8) succeeds.

- [ ] **Step 2: Confirm test count**

Run: `find app/build/test-results -name "*.xml" | xargs grep -h "testsuite " | grep -oE 'tests="[0-9]+" skipped="[0-9]+" failures="[0-9]+" errors="[0-9]+"' | awk -F'"' '{t+=$2;s+=$4;f+=$6;e+=$8} END {print "tests",t,"skipped",s,"failures",f,"errors",e}'`
Expected: `failures 0 errors 0`, tests ≈ 88.

- [ ] **Step 3: Manual smoke test (user-driven, highest runtime risk)**

Install and check on a device/emulator (API 26+): onboarding pager swipe + dot indicator (foundation Pager), navigation between screens (compose-destinations 2.x), status-bar appearance (systemuicontroller migration), and map + places search (maps-compose/places majors).
Run: `JAVA_HOME="$(/usr/libexec/java_home -v 17)" ./gradlew installDebug` then launch the app.

- [ ] **Step 4: Re-index GitNexus (code changed)**

Run: `npx gitnexus analyze`

- [ ] **Step 5: Summarize & report**

Report the final version matrix actually achieved (note any fallbacks from Stage 6), test results, and any deferred follow-ups. **Do not `git push`** — wait for explicit user consent.

---

## Self-review notes
- minSdk remains 26 throughout (no stage changes it). ✔
- compileSdk/targetSdk 37 lands in Stage 3, before AndroidX libs needing it. ✔
- Compose compiler plugin replaces `kotlinCompilerExtensionVersion` in Stage 4. ✔
- Hilt → KSP in Stage 4 with documented kapt fallback. ✔
- compose-destinations 2.x + systemuicontroller migrations have explicit file lists. ✔
- okhttp held at 4.12.0 (locked). ✔
- Revert: `pre-latest-upgrade` tag (Stage 0) + per-stage commits. ✔
- "resolve" markers flag trailing libs whose exact latest must be confirmed at execution (play-services, accompanist-permissions, test libs).
