
# Debugging Android Applications

Comprehensive guide for debugging Android apps using various techniques.

## Logcat Debugging

### Adding Logs

```kotlin
import android.util.Log

class MyClass {
    companion object {
        private const val TAG = "MyClass"
  }
    
    fun someMethod() {
        Log.v(TAG, "Verbose: detailed debug info")
        Log.d(TAG, "Debug: development debug messages")
        Log.i(TAG, "Info: informational messages")
        Log.w(TAG, "Warning: warning messages")
        Log.e(TAG, "Error: error messages", exception)
  }
}
```

### Log Levels

- **Log.v()** - Verbose (gray) - Very detailed, development only
- **Log.d()** - Debug (blue) - Debug messages for development
- **Log.i()** - Info (green) - General informational messages
- **Log.w()** - Warning (orange) - Warnings, potential issues
- **Log.e()** - Error (red) - Errors and exceptions

### Viewing Logcat

**In Android Studio:**
- View → Tool Windows → Logcat
- Filter by tag, package, or regex
- Select log level (Verbose, Debug, Info, Warn, Error)

**Command Line:**
```bash
# View all logs
adb logcat

# Filter by tag
adb logcat -s MyTag

# Filter by package
adb logcat | grep "com.julian.automaticclockwidget"

# Clear logs
adb logcat -c

# Save to file
adb logcat > logcat.txt

# Show only errors
adb logcat *:E
```

### Best Practices for Logging

```kotlin
// Use companion object for TAG
companion object {
    private const val TAG = "CalendarRefreshWorker"
}

// Log method entry/exit for debugging flow
Log.d(TAG, "execute: starting calendar refresh")

// Log important values
Log.i(TAG, "Processing ${events.size} events")

// Log errors with exception
Log.e(TAG, "Failed to download calendar: ${error.message}", error)

// Log success states
Log.i(TAG, "Calendar refresh completed successfully")
```

## Breakpoint Debugging

### Setting Breakpoints

1. Click gutter (left of line numbers) to add breakpoint
2. Right-click breakpoint for conditions
3. Run app in Debug mode (Shift+F9 or Debug icon)

### Conditional Breakpoints

Right-click breakpoint → Condition:
```kotlin
// Break only when url contains "calendar"
url.contains("calendar")

// Break only for specific item
item.id == "12345"

// Break on Nth iteration
i == 50
```

### Breakpoint Actions

- **Step Over (F8)**: Execute line and move to next
- **Step Into (F7)**: Step into method call
- **Step Out (Shift+F8)**: Step out of current method
- **Resume (F9)**: Continue to next breakpoint
- **Evaluate Expression (Alt+F8)**: Evaluate code at breakpoint

### Exception Breakpoints

Run → View Breakpoints → Add Java Exception Breakpoints
- Break on specific exception types
- Break on caught/uncaught exceptions

## Debugging Compose UI

### Layout Inspector

View → Tool Windows → Layout Inspector
- Select running device/emulator
- Inspect composable hierarchy
- View properties and modifiers
- 3D view of layout layers

### Compose Preview

Add `@Preview` to composables:

```kotlin
@Preview(showBackground = true)
@Composable
fun HomeContentPreview() {
    HomeContent(
        state = HomeUiState(
            urls = listOf("https://example.com"),
            selected = "https://example.com",
            errorMessage = null,
            successMessage = null,
            perMinuteTickEnabled = false,
            requestExactAlarmPermission = false
        ),
        onEvent = {}
    )
}
```

### Debugging Compose Recomposition

```kotlin
// Log recompositions
@Composable
fun DebugComposable() {
    SideEffect {
        Log.d("Recomposition", "DebugComposable recomposed")
  }
    // ...
}
```

## Debugging WorkManager

### Enable Work Manager Logging

```kotlin
// In Application onCreate()
if (BuildConfig.DEBUG) {
    WorkManager.initialize(
        this,
        Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
    )
}
```

### View Work Status

```bash
# View all work
adb shell dumpsys jobscheduler

# Filter by package
adb shell dumpsys jobscheduler | grep "com.julian.automaticclockwidget"
```

In code:
```kotlin
WorkManager.getInstance(context)
    .getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME)
    .observe(lifecycleOwner) { workInfos ->
        workInfos.firstOrNull()?.let { workInfo ->
            Log.d(TAG, "Work state: ${workInfo.state}")
            Log.d(TAG, "Run attempt: ${workInfo.runAttemptCount}")
      }
  }
```

## Debugging Network Requests

### OkHttp Logging Interceptor

Already configured in this project:

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
  })
    .build()
```

Levels:
- `NONE` - No logs
- `BASIC` - Request/response line
- `HEADERS` - Request/response headers
- `BODY` - Full request/response body

### Network Profiler

View → Tool Windows → Profiler → Network
- View all network requests
- Inspect request/response details
- Monitor network usage

## Debugging Glance Widgets

### Widget Logs

```kotlin
class AutomaticClockWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "provideGlance called for widget: $id")
        provideContent {
            val state = currentState<AutomaticClockWidgetUiState>()
            Log.d(TAG, "Widget state: $state")
            ClockWidgetContent(state)
      }
  }
}
```

### Force Widget Update

```bash
# Trigger widget update
adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE
```

### Widget State Debugging

```kotlin
suspend fun debugWidgetState(context: Context) {
    val glanceIds = GlanceAppWidgetManager(context)
        .getGlanceIds(AutomaticClockWidget::class.java)
    
    glanceIds.forEach { glanceId ->
        val state = getAppWidgetState(
            context,
            AutomaticClockGlanceStateDefinition,
            glanceId
        )
        Log.d(TAG, "Widget $glanceId state: $state")
  }
}
```

## Debugging Crashes

### Reading Stack Traces

```
FATAL EXCEPTION: main
Process: com.julian.automaticclockwidget, PID: 12345
java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String getData()' on a null object reference
    at com.julian.automaticclockwidget.MyClass.process(MyClass.kt:42)
    at com.julian.automaticclockwidget.MyClass$execute$1.invokeSuspend(MyClass.kt:28)
    ...
```

Key information:
- **Exception type**: NullPointerException
- **Message**: "Attempt to invoke..."
- **Location**: MyClass.kt line 42
- **Call stack**: Trace of method calls

### Sentry Integration

This project uses Sentry for crash reporting. Crashes automatically reported in production.

View crashes at: [Sentry Dashboard]

### Local Crash Testing

```kotlin
// Trigger crash for testing
if (BuildConfig.DEBUG) {
    throw RuntimeException("Test crash")
}
```

## Memory Debugging

### Memory Profiler

View → Tool Windows → Profiler → Memory
- Monitor heap usage
- Track allocations
- Detect memory leaks
- Capture heap dump

### Detecting Leaks

Add LeakCanary (debug builds only):

```kotlin
debugImplementation("com.squareup.leakcanary:leakcanary-android:2.x.x")
```

## Database Debugging

### Database Inspector

View → Tool Windows → App Inspection → Database Inspector
- View DataStore files
- Query databases
- Live updates

### Export DataStore

```bash
# Pull DataStore file
adb pull /data/data/com.julian.automaticclockwidget/files/datastore/automatic_clock_widget_state
```

## Performance Profiling

### CPU Profiler

View → Tool Windows → Profiler → CPU
- Record method traces
- Identify slow methods
- Optimize hotspots

### Common Issues

- **ANR (Application Not Responding)**: Blocking main thread > 5s
- **Jank**: Frame drops, UI stuttering
- **Memory leaks**: Objects not garbage collected

## ADB Debugging Commands

```bash
# List devices
adb devices

# Install APK
adb install app-debug.apk

# Uninstall app
adb uninstall com.julian.automaticclockwidget

# Clear app data
adb shell pm clear com.julian.automaticclockwidget

# Start activity
adb shell am start -n com.julian.automaticclockwidget/.MainActivity

# Force stop app
adb shell am force-stop com.julian.automaticclockwidget

# View app info
adb shell dumpsys package com.julian.automaticclockwidget

# Screenshot
adb shell screencap /sdcard/screen.png
adb pull /sdcard/screen.png

# Screen recording
adb shell screenrecord /sdcard/demo.mp4
```

## Debugging Tips

### Enable Debug Mode

```kotlin
if (BuildConfig.DEBUG) {
    // Enable verbose logging
    // Disable analytics
    // Enable debug UI elements
}
```

### Debug vs Release

- **Debug**: Includes debug symbols, logs, LeakCanary
- **Release**: Optimized, minified, no debug symbols

### Common Debugging Scenarios

**Coroutine not executing:**
```kotlin
Log.d(TAG, "Before launch")
viewModelScope.launch {
    Log.d(TAG, "Inside coroutine")
    // Check if this logs
}
```

**StateFlow not updating:**
```kotlin
_uiState.value = _uiState.value.copy(data = newData)
Log.d(TAG, "Updated state: ${_uiState.value}")
```

**Result always failing:**
```kotlin
result.fold(
    onSuccess = { Log.i(TAG, "Success: $it") },
    onFailure = { Log.e(TAG, "Failure: ${it.message}", it) }
)
```

## Checklist

- [ ] Add meaningful log statements with TAG
- [ ] Use appropriate log levels (v/d/i/w/e)
- [ ] Set breakpoints at critical points
- [ ] Use conditional breakpoints for specific cases
- [ ] Check Logcat for errors and warnings
- [ ] Use Layout Inspector for UI issues
- [ ] Profile performance with Profiler tools
- [ ] Test on multiple devices/API levels
- [ ] Check for memory leaks with LeakCanary
- [ ] Review Sentry for production crashes
