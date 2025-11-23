### AutomaticClockWidget — Developer Guidelines

This document captures project-specific knowledge to help advanced contributors build, test, and extend the app with confidence.

#### Project Overview
- Android app (minSdk 31, target/compileSdk 36) using Kotlin, Jetpack Compose, and Glance for an App Widget.
- DI: Koin BOM with compose integration.
- Time/calendars: `kotlinx-datetime` and `biweekly` (iCalendar parsing).
- Networking: OkHttp (BOM + logging-interceptor); Kotlinx Serialization JSON.
- Version catalog: Dependencies centralized in `gradle/libs.versions.toml`.
- Entry points:
  - App: `com.julian.automaticclockwidget.MainActivity`
  - Widget: `com.julian.automaticclockwidget.widgets.AutomaticClockWidget` via `AutomaticClockWidgetReceiver` and `res/xml/automatic_clock_widget_info.xml`

#### Build & Configuration
- Toolchain
  - Android Gradle Plugin: `8.13.0` (`plugins.android-application` via version catalog)
  - Kotlin: `2.2.21` (Compose compiler plugin enabled)
  - JVM target: 11 (Gradle config enforces Java 11 for source/target)
- SDKs
  - compileSdk/targetSdk: 36
  - minSdk: 31 (required by Glance + modern APIs)
- How to build from CLI
  - Assemble debug APK: `./gradlew :app:assembleDebug`
  - Full build (all variants): `./gradlew build`
  - Clean: `./gradlew clean`
- IDE setup
  - Use Android Studio Koala+ (or newer) with JDK 17 installed; Gradle itself targets JVM 11 for compilation. Let the IDE use the Gradle JDK to avoid mismatch.
  - Ensure Android SDK 36 Platform + Build Tools are installed.
- Signing/ProGuard
  - `release` is not minified by default (`isMinifyEnabled = false`). ProGuard rules placeholder at `app/proguard-rules.pro`. Adjust if enabling R8.

#### Modules & Key Code Paths
- Single module `:app`. Key packages:
  - `widgets/` — Glance widget entry (`AutomaticClockWidget`, `AutomaticClockWidgetReceiver`).
  - `calendars/` — Calendar domain, repository, and iCalendar parsing (`biweekly`).
  - `airports/` — Airport/timezone lookup and use case.
  - `ui/theme/` — Koin module (`AppModule.kt`), theming.
- Widget specifics
  - Glance `SizeMode.Exact`; layout adapts row/column counts based on `LocalSize`. If you add interactive elements or background work, prefer `ActionCallback` with `actionRunCallback`. Be mindful that Glance runs on the app process and has RemoteViews constraints.
  - Update behavior comes from standard `APPWIDGET_UPDATE` broadcast (see `AndroidManifest.xml`) and the provider XML in `res/xml/automatic_clock_widget_info.xml`.

#### Testing
- Unit tests (JVM): JUnit4, no Android deps.
  - Run all unit tests: `./gradlew :app:testDebugUnitTest`
  - Run a specific test class: `./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.ExampleUnitTest'`
  - Run a specific test method: `./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.ExampleUnitTest.addition_isCorrect'`
- Instrumented tests (Android): androidx test runner.
  - Requires emulator/device (API 31+). Run: `./gradlew :app:connectedDebugAndroidTest`
  - Runner: `androidx.test.runner.AndroidJUnitRunner` (declared in `defaultConfig`).
- Compose UI tests
  - Dependencies for Compose UI testing are present under `androidTest` via the Compose BOM. Add tests in `app/src/androidTest/...` and use `createAndroidComposeRule`.
- HTTP and time-dependent code
  - For networking, we currently do not include `mockwebserver`. If needed, add it via the version catalog (recommend `com.squareup.okhttp3:mockwebserver` aligned with the OkHttp BOM) and use it in JVM tests where possible.
  - For time, `kotlinx-datetime` simplifies pure JVM tests; abstract system clock if you need deterministic results.

##### Verified example (executed before writing these guidelines)
- We validated that JVM tests run successfully in this project env. Example command and result:
  - Command: `./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.GuidelinesSanityTest.sanity_addition'`
  - Result: Passed (1/1). The temporary test file used for this verification has been removed, as it was only for demonstration.

##### Adding a new unit test
1. Create a file under `app/src/test/java/com/julian/automaticclockwidget/` (or a suitable package) with `@Test` methods using JUnit4.
2. Keep tests free of Android SDK types; use pure Kotlin/JVM utilities.
3. Run: `./gradlew :app:testDebugUnitTest` or target it with `--tests`.

##### Adding a new instrumented/Compose test
1. Create tests under `app/src/androidTest/...`.
2. Use `androidx.test.ext.junit.runners.AndroidJUnit4` and, for Compose, `createAndroidComposeRule`.
3. Launch an emulator/device (API 31+), then run: `./gradlew :app:connectedDebugAndroidTest`.

#### Development Practices & Tips
- Code style
  - Follow Kotlin style and the module’s existing formatting. Compose: prefer small, previewable composables. Avoid business logic in composables.
  - Keep widget layout lightweight; avoid heavy allocations in `provideGlance`/`provideContent`.
- DI with Koin
  - Use `AppModule.kt` to register singletons and view models. When adding new repositories/use-cases, declare them there and inject via `by inject()` / `koinViewModel()`.
- Networking
  - OkHttp BOM ensures version alignment. Prefer a single OkHttpClient with the logging interceptor conditionally enabled for debug builds.
- Calendars & Timezones
  - `biweekly` handles iCalendar; wrap parsing in repository/use case layers. Keep timezone conversions within `kotlinx-datetime` to stay testable.
- Version catalog hygiene
  - Add new libraries to `gradle/libs.versions.toml` and depend on them via aliases in Gradle Kotlin DSL. Keep BOMs for families (Compose, OkHttp, Koin) to avoid version drift.
- Minimum Android version features
  - With minSdk 31, you can rely on modern APIs (pending intents mutability, notifications, etc.) without deep compatibility layers.
- Code style and best practices
  - Use SOLID principles and DRY principles when developing 
  - Use the principles of clean architecture for clear separation of concerns and testability with the appropriate layers :
    - Create a Usecase for the domain behavior and features
    - Create a Repository for data access
  - Use anti-corruption layers to isolate the domain from the UI and the data from the domain

#### Troubleshooting
- Build fails with SDK mismatch: Ensure Android 36 platform and corresponding Build Tools are installed. Sync Gradle and re-import.
- Compose/Glance preview issues: Clear Gradle caches, invalidate IDE caches. Some Glance previews require running on device; rely on `providePreview` when possible.
- Runtime widget issues: Check that the widget provider XML and receiver `exported`/`intent-filters` match what Glance expects; verify `APPWIDGET_UPDATE` delivery in logs.
