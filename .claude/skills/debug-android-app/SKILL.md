---
name: debug-android-app
description: >
  Debug Android application issues for this project using Logcat, Android Studio tools, ADB
  commands, and code inspection. Use when investigating crashes, unexpected behavior, widget
  issues, WorkManager problems, network failures, Compose recomposition issues, or StateFlow not
  updating. Triggers on "debug", "app is crashing", "why isn't it working", "investigate bug",
  "check logs", "widget not updating", "state not updating", "network request failing".
allowed-tools: Bash, Read, Grep
effort: medium
tags: [debug, android, logcat, adb, glance, workmanager]
---

# Debug Android App

## Quick Diagnosis

| Symptom | Tool |
|---------|------|
| Crash / exception | Logcat → find FATAL EXCEPTION |
| UI not updating | Layout Inspector / recomposition logs |
| Network call failing | OkHttp interceptor logs / Network Profiler |
| Widget not updating | Glance state logs / force update via ADB |
| Worker not running | WorkManager logs / `adb shell dumpsys jobscheduler` |
| StateFlow not emitting | Add log after `_uiState.update {}` |
| Memory leak | Memory Profiler / LeakCanary |

## Logcat Patterns

```kotlin
companion object { private const val TAG = "MyClass" }

Log.d(TAG, "method: entering with param=$param")
Log.i(TAG, "method: success, result=$result")
Log.e(TAG, "method: failed: ${error.message}", error)
```

**ADB Logcat:**
```bash
adb logcat | grep "com.julian.automaticclockwidget"
adb logcat -s CalendarRefreshWorker    # Filter by TAG
adb logcat *:E                          # Errors only
adb logcat -c                           # Clear logs
```

## Debugging StateFlow

```kotlin
// Not updating? Add log after update:
_uiState.update { it.copy(data = newData) }
Log.d(TAG, "State updated: ${_uiState.value}")

// Check collection side:
viewModel.uiState.collect { Log.d(TAG, "UI state: $it") }
```

## Debugging Compose

```kotlin
// Log recompositions:
@Composable
fun MyComposable() {
    SideEffect { Log.d("Recompose", "MyComposable recomposed") }
}

// Preview for isolated UI testing:
@Preview(showBackground = true)
@Composable
fun MyComposablePreview() { MyComposable(state = previewState, onEvent = {}) }
```

Layout Inspector: **View → Tool Windows → Layout Inspector**

## Debugging Network

OkHttp interceptor already configured — set log level in DI setup:
- `NONE / BASIC / HEADERS / BODY`

```bash
# Network Profiler: View → Tool Windows → Profiler → Network
```

## Debugging Glance Widget

```kotlin
override suspend fun provideGlance(context: Context, id: GlanceId) {
    Log.d(TAG, "provideGlance: id=$id")
    provideContent {
        val state = currentState<AutomaticClockWidgetUiState>()
        Log.d(TAG, "Widget state: $state")
        ClockWidgetContent(state)
    }
}
```

```bash
# Force widget update:
adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE
```

## Debugging WorkManager

```bash
adb shell dumpsys jobscheduler | grep "com.julian.automaticclockwidget"
```

```kotlin
WorkManager.getInstance(context)
    .getWorkInfosForUniqueWorkLiveData(CalendarRefreshWorker.UNIQUE_WORK_NAME)
    .observe(lifecycleOwner) { workInfos ->
        Log.d(TAG, "Work state: ${workInfos.firstOrNull()?.state}")
    }
```

## Common ADB Commands

```bash
adb devices
adb shell pm clear com.julian.automaticclockwidget   # Clear app data
adb shell am start -n com.julian.automaticclockwidget/.MainActivity
adb shell am force-stop com.julian.automaticclockwidget
adb pull /data/data/com.julian.automaticclockwidget/files/datastore/  # Export DataStore
adb shell screencap /sdcard/screen.png && adb pull /sdcard/screen.png
```

## Reading Crash Stack Traces

```
FATAL EXCEPTION: main
java.lang.NullPointerException: Attempt to invoke method on null
    at com.julian.automaticclockwidget.MyClass.process(MyClass.kt:42)  ← start here
```

Key info: exception type → message → first `com.julian.*` line (root cause).

## Reference Files

- **[android-studio-tools.md](references/android-studio-tools.md)** — Profiler, Layout Inspector, App Inspection, breakpoints
