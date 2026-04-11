# Android Studio Debugging Tools

## Breakpoints

1. Click gutter (left of line numbers) to set breakpoint
2. Run → Debug (Shift+F9)
3. Right-click breakpoint → add Condition (e.g. `item.id == "JFK"`)

Breakpoint navigation:
- **F8** — Step Over
- **F7** — Step Into
- **Shift+F8** — Step Out
- **F9** — Resume
- **Alt+F8** — Evaluate Expression

Exception breakpoints: **Run → View Breakpoints → Add Java Exception Breakpoints**

---

## Layout Inspector

**View → Tool Windows → Layout Inspector**

- Select running device/emulator
- Inspect Compose hierarchy tree
- View applied modifiers and properties
- Use 3D layer view to diagnose z-ordering or clipping issues

---

## Profiler

**View → Tool Windows → Profiler**

### CPU Profiler
- Record method traces to find slow code
- Use "Sample Java/Kotlin Methods" for general profiling

### Memory Profiler
- Monitor heap usage over time
- Capture heap dump to inspect retained objects
- Look for growing allocations that never GC

### Network Profiler
- View all OkHttp requests
- Inspect request/response headers and body
- Check timing (DNS, connect, SSL, TTFB, response)

---

## App Inspection

**View → Tool Windows → App Inspection**

### Database Inspector
- View DataStore proto files
- Inspect SharedPreferences
- Live query Room databases

```bash
# Export DataStore manually:
adb pull /data/data/com.julian.automaticclockwidget/files/datastore/
```

---

## LeakCanary (Memory Leaks)

Add to debug build only:

```kotlin
// build.gradle.kts
debugImplementation("com.squareup.leakcanary:leakcanary-android:2.x.x")
```

LeakCanary auto-detects leaked Activities, Fragments, ViewModels. Notification appears on device when leak found.

---

## WorkManager Inspector

**View → Tool Windows → App Inspection → WorkManager**

- View all enqueued/running/completed work
- See work constraints and retry count
- Cancel work directly from UI

Enable verbose WorkManager logging in debug:

```kotlin
// Application.onCreate()
if (BuildConfig.DEBUG) {
    WorkManager.initialize(this,
        Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build()
    )
}
```
