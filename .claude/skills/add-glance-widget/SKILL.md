---
name: add-glance-widget
description: >
  Create a new Jetpack Glance home screen widget for this Android project. Use when adding a new
  app widget, modifying widget UI, implementing widget state management with DataStore, or creating
  widget update logic. Covers sealed UiState, GlanceStateDefinition, GlanceAppWidget,
  GlanceAppWidgetReceiver, update use case, AndroidManifest registration, and widget info XML.
  Triggers on "add a widget", "create a glance widget", "add home screen widget", "implement widget",
  "update widget UI".
allowed-tools: Read, Write, Edit, Bash, Glob, Grep
effort: low
tags: [glance, widget, android, scaffold, datastore]
---

# Add Glance Widget

Files live in `app/src/main/java/com/julian/automaticclockwidget/widgets/`:
1. `[Name]WidgetUiState.kt` — state sealed class
2. `[Name]GlanceStateDefinition.kt` — DataStore state definition
3. `[Name]Widget.kt` — widget composable UI
4. `[Name]WidgetReceiver.kt` — broadcast receiver

## Step 1: Widget State

```kotlin
@Serializable
sealed class [Name]WidgetUiState {
    @Serializable data object Empty : [Name]WidgetUiState()
    @Serializable data class Loaded(val data: List<SomeData>) : [Name]WidgetUiState()
}
```

## Step 2: Glance State Definition (DataStore)

```kotlin
object [Name]GlanceStateDefinition : GlanceStateDefinition<[Name]WidgetUiState> {

    private const val DATA_STORE_FILENAME = "[name]_widget_state"

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<[Name]WidgetUiState> =
        context.[name]WidgetStateDataStore

    override fun getLocation(context: Context, fileKey: String): File =
        context.dataStoreFile(DATA_STORE_FILENAME)

    object Serializer : Serializer<[Name]WidgetUiState> {
        override val defaultValue = [Name]WidgetUiState.Empty

        override suspend fun readFrom(input: InputStream): [Name]WidgetUiState =
            try {
                Json.decodeFromString([Name]WidgetUiState.serializer(), input.readBytes().decodeToString())
            } catch (e: SerializationException) {
                throw CorruptionException("Could not read widget data", e)
            }

        override suspend fun writeTo(t: [Name]WidgetUiState, output: OutputStream) {
            output.write(Json.encodeToString([Name]WidgetUiState.serializer(), t).encodeToByteArray())
        }
    }
}

val Context.[name]WidgetStateDataStore by dataStore(
    fileName = [Name]GlanceStateDefinition.DATA_STORE_FILENAME,
    serializer = [Name]GlanceStateDefinition.Serializer,
)
```

## Step 3: Widget Class

```kotlin
class [Name]Widget : GlanceAppWidget() {

    override val stateDefinition = [Name]GlanceStateDefinition
    override val sizeMode = SizeMode.Exact  // responsive layout

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = currentState<[Name]WidgetUiState>()
                [Name]WidgetContent(state)
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            GlanceTheme { [Name]WidgetContent([Name]WidgetUiState.Empty) }
        }
    }

    @Composable
    private fun [Name]WidgetContent(state: [Name]WidgetUiState) {
        val size = LocalSize.current  // available for responsive layout

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = Color.White.copy(alpha = 0.8f), night = Color.Black.copy(alpha = 0.8f)))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                is [Name]WidgetUiState.Empty -> Text("No data yet")
                is [Name]WidgetUiState.Loaded -> state.data.forEach { Text(it.toString()) }
            }
        }
    }
}
```

> For complex views (e.g. analog clocks), use `AndroidRemoteViews`:
> ```kotlin
> AndroidRemoteViews(RemoteViews(packageName, R.layout.clock).apply {
>     setString(R.id.clock, "setTimeZone", timezoneId)
> })
> ```

## Step 4: Widget Receiver

```kotlin
class [Name]WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = [Name]Widget()
}
```

## Step 5: Update Use Case

```kotlin
class Update[Name]WidgetUseCase(private val repository: SomeRepository) {
    suspend fun updateAll(context: Context) {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds([Name]Widget::class.java)
        val data = repository.getData().getOrNull() ?: emptyList()
        val newState = if (data.isEmpty()) [Name]WidgetUiState.Empty else [Name]WidgetUiState.Loaded(data)

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, [Name]GlanceStateDefinition, glanceId) { newState }
            [Name]Widget().update(context, glanceId)
        }
    }
}
```

Register in Koin: `single { Update[Name]WidgetUseCase(get()) }`

## Step 6: AndroidManifest.xml

```xml
<receiver android:name=".widgets.[Name]WidgetReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/[name]_widget_info" />
</receiver>
```

## Step 7: Widget Info XML (`res/xml/[name]_widget_info.xml`)

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/[name]_widget_description"
    android:initialLayout="@layout/glance_default_loading_layout"
    android:minWidth="110dp"
    android:minHeight="110dp"
    android:resizeMode="horizontal|vertical"
    android:targetCellWidth="2"
    android:targetCellHeight="2"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen" />
```

## Glance Key Rules

- Use `GlanceModifier` (not regular Compose modifiers)
- Use `ColorProvider(day = ..., night = ...)` for theme-aware colors
- Use `LocalSize.current` for responsive layouts with `SizeMode.Exact`
- Set `updatePeriodMillis="0"` — update manually via use case
- Always call `widget.update(context, glanceId)` after `updateAppWidgetState`
- Keep widget UI simple — it's RemoteViews under the hood

## Checklist

- [ ] Sealed `[Name]WidgetUiState` with `@Serializable`
- [ ] `[Name]GlanceStateDefinition` with DataStore serializer
- [ ] `[Name]Widget` extending `GlanceAppWidget`
- [ ] `[Name]WidgetReceiver` extending `GlanceAppWidgetReceiver`
- [ ] `Update[Name]WidgetUseCase` registered in Koin
- [ ] Receiver in `AndroidManifest.xml`
- [ ] `res/xml/[name]_widget_info.xml`
- [ ] String resources + preview drawable
- [ ] Test on device/emulator with different widget sizes
