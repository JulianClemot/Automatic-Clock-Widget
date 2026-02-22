
# Adding a Glance Widget

Glance is Jetpack's declarative UI framework for app widgets (home screen widgets).

## File Structure

In `app/src/main/java/com/julian/automaticclockwidget/widgets/`:

1. **[Name]Widget.kt** - Main widget class with Glance UI
2. **[Name]WidgetReceiver.kt** - Broadcast receiver for widget updates
3. **[Name]WidgetUiState.kt** - Widget state definition
4. **[Name]GlanceStateDefinition.kt** - State persistence

## Step 1: Define Widget State

```kotlin
package com.julian.automaticclockwidget.widgets

import kotlinx.serialization.Serializable

@Serializable
sealed class [Name]WidgetUiState {
    @Serializable
    data object Empty : [Name]WidgetUiState()
    
    @Serializable
    data class Loaded(val data: List<SomeData>) : [Name]WidgetUiState()
}
```

## Step 2: Create State Definition

```kotlin
package com.julian.automaticclockwidget.widgets

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object [Name]GlanceStateDefinition : GlanceStateDefinition<[Name]WidgetUiState> {
    
    private const val DATA_STORE_FILENAME = "[name]_widget_state"
    
    override suspend fun getDataStore(
        context: Context,
        fileKey: String
    ): DataStore<[Name]WidgetUiState> {
      return context.[name]WidgetStateDataStore
  }
    
    override fun getLocation(context: Context, fileKey: String): File {
      return context.dataStoreFile(DATA_STORE_FILENAME)
  }
    
    object [Name]WidgetStateSerializer : Serializer<[Name]WidgetUiState> {
        override val defaultValue = [Name]WidgetUiState.Empty
        
        override suspend fun readFrom(input: InputStream): [Name]WidgetUiState {
            return try {
                Json.decodeFromString(
                    [Name]WidgetUiState.serializer(),
                    input.readBytes().decodeToString()
                )
            } catch (exception: SerializationException) {
                throw CorruptionException("Could not read widget data", exception)
            }
      }
        
        override suspend fun writeTo(t: [Name]WidgetUiState, output: OutputStream) {
            output.write(
                Json.encodeToString([Name]WidgetUiState.serializer(), t)
                    .encodeToByteArray()
            )
      }
  }
}

val Context.[name]WidgetStateDataStore by dataStore(
    fileName = "[Name]GlanceStateDefinition.DATA_STORE_FILENAME",
    serializer = [Name]GlanceStateDefinition.[Name]WidgetStateSerializer
)
```

## Step 3: Create Widget Class

```kotlin
package com.julian.automaticclockwidget.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class [Name]Widget : GlanceAppWidget() {

    override val stateDefinition = [Name]GlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = currentState<[Name]WidgetUiState>()
                [Name]WidgetContent(state)
            }
      }
  }

    @Composable
    private fun [Name]WidgetContent(state: [Name]WidgetUiState) {
        val size = LocalSize.current

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(
                    ColorProvider(
                        day = Color.White.copy(alpha = 0.8f),
                        night = Color.Black.copy(alpha = 0.8f)
                    )
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is [Name]WidgetUiState.Empty -> {
                    Text(
                        "No data yet",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = ColorProvider(
                                day = Color.Black,
                                night = Color.White
                            )
                        )
                    )
                }
                is [Name]WidgetUiState.Loaded -> {
                    state.data.forEach { item ->
                        Text(
                            item.toString(),
                            style = TextStyle(fontSize = 12.sp)
                        )
                    }
                }
            }
      }
  }

    // Use SizeMode.Exact for responsive layouts based on widget size
    override val sizeMode = SizeMode.Exact

    override suspend fun providePreview(
        context: Context,
        widgetCategory: Int
    ) {
        provideContent {
            GlanceTheme {
                [Name]WidgetContent([Name]WidgetUiState.Empty)
            }
      }
  }
}
```

## Step 4: Create Widget Receiver

```kotlin
package com.julian.automaticclockwidget.widgets

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class [Name]WidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = [Name]Widget()
}
```

## Step 5: Create Update Use Case

```kotlin
package com.julian.automaticclockwidget.widgets

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

class [Name]WidgetUpdateUseCase(
  private val repository: SomeRepository,
) {
  suspend fun updateAll(context: Context) {
        val glanceIds = GlanceAppWidgetManager(context)
            .getGlanceIds([Name]Widget::class.java)
        
        val data = repository.getData().getOrNull() ?: emptyList()
        val newState = if (data.isEmpty()) {
            [Name]WidgetUiState.Empty
      } else {
            [Name]WidgetUiState.Loaded(data)
      }
        
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                definition = [Name]GlanceStateDefinition,
                glanceId = glanceId
            ) { newState }
            
            [Name]Widget().update(context, glanceId)
      }
  }
}
```

## Step 6: Register in AndroidManifest.xml

```xml
<receiver
    android:name=".widgets.[Name]WidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/[name]_widget_info" />
</receiver>
```

## Step 7: Create Widget Info XML

In `app/src/main/res/xml/[name]_widget_info.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/[name]_widget_description"
    android:initialLayout="@layout/glance_default_loading_layout"
    android:minWidth="110dp"
    android:minHeight="110dp"
    android:previewImage="@drawable/[name]_widget_preview"
    android:resizeMode="horizontal|vertical"
    android:targetCellWidth="2"
    android:targetCellHeight="2"
    android:updatePeriodMillis="0"
    android:widgetCategory="home_screen" />
```

## Step 8: Register in Koin

```kotlin
val appModule = module {
    // ... other dependencies
    
    single { [Name]WidgetUpdateUseCase(get()) }
}
```

## Glance Best Practices

### Layout
- Use `GlanceModifier` (not regular Compose modifiers)
- `SizeMode.Exact` allows responsive layouts based on widget size
- Access size with `LocalSize.current`
- Calculate layout dynamically based on available space

### Styling
- Use `ColorProvider` for day/night themes:
  ```kotlin
  ColorProvider(day = Color.White, night = Color.Black)
  ```
- Use `GlanceTheme` for consistent theming
- Use `cornerRadius()` for rounded corners

### State Management
- Store state in DataStore using `GlanceStateDefinition`
- Use sealed classes for type-safe state
- Update state with `updateAppWidgetState()`
- Always call `widget.update()` after state changes

### Performance
- Widgets are remote views - keep UI simple
- Limit number of elements based on widget size
- Use `take()` or `chunked()` to limit displayed items
- Set `updatePeriodMillis="0"` - update manually via use case

### AndroidRemoteViews
- For complex views (like analog clocks), use `AndroidRemoteViews`:
  ```kotlin
  AndroidRemoteViews(
      RemoteViews(packageName, R.layout.clock).apply {
          setString(R.id.clock, "setTimeZone", timezoneId)
      }
  )
  ```

## Testing

- Test widget preview in Android Studio widget picker
- Test different widget sizes (small, medium, large)
- Test light/dark themes
- Verify updates work correctly

## Checklist

- [ ] Create widget state sealed class with `@Serializable`
- [ ] Create `GlanceStateDefinition` with DataStore serializer
- [ ] Create widget class extending `GlanceAppWidget`
- [ ] Implement `provideGlance()` with UI composables
- [ ] Create widget receiver extending `GlanceAppWidgetReceiver`
- [ ] Create update use case
- [ ] Add receiver to AndroidManifest.xml
- [ ] Create widget info XML in res/xml/
- [ ] Add strings and preview image
- [ ] Register update use case in Koin
- [ ] Test widget on device/emulator
