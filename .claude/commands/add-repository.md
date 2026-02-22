
# Adding a Repository

Repositories abstract data sources following Clean Architecture principles.

## Structure

1. **Interface**: `app/src/main/java/com/julian/automaticclockwidget/[feature]/[Noun]Repository.kt`
2. **Implementation**: `app/src/main/java/com/julian/automaticclockwidget/[feature]/[source]/[Source][Noun]Repository.kt`
3. **Data models**: In the `[source]/` package with `.toDomainModel()` mapping

## Interface Template

```kotlin
package com.julian.automaticclockwidget.[feature]

interface [Noun]Repository {
  suspend fun [operation]([params]): Result<[ReturnType]>
}
```

## Implementation Template (REST API)

```kotlin
package com.julian.automaticclockwidget.[feature].rest

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.IOException

class Rest[Noun]Repository(
  private val client: OkHttpClient,
) : [Noun]Repository {
  
  override suspend fun [operation]([params]) = withContext(Dispatchers.IO) {
      runCatching {
          val request = Request.Builder()
              .url("[API_URL]")
              .build()
          
          val response = client.newCall(request).execute()
          
          if (!response.isSuccessful) {
              throw [Feature]Error.HttpFailure(
                  code = response.code,
                  message = "HTTP ${response.code}"
              )
          }
          
          val body = response.body?.string()
              ?: throw [Feature]Error.Parse("Empty response")
          
          val data = Json.decodeFromString<Rest[Model]>(body)
          data.toDomainModel()
      }.recoverCatching { t ->
          when (t) {
              is [Feature]Error -> throw t
              is IOException -> throw [Feature]Error.Network(cause = t)
              else -> throw UnknownError(cause = t)
          }
      }
  }
}
```

## Data Model with Mapping

```kotlin
package com.julian.automaticclockwidget.[feature].rest

import kotlinx.serialization.Serializable
import com.julian.automaticclockwidget.[feature].[DomainModel]

@Serializable
data class Rest[Model](
  val field1: String,
  val field2: Int,
) {
  fun toDomainModel() = [DomainModel](
      field1 = field1,
      field2 = field2,
  )
}
```

## Domain Model (in feature package)

```kotlin
package com.julian.automaticclockwidget.[feature]

data class [Model](
  val field1: String,
  val field2: Int,
)
```

## Error Handling

Define feature-specific errors in `core/AppError.kt`:

```kotlin
sealed class [Feature]Error(
  message: String? = null,
  cause: Throwable? = null
) : AppError(message, cause) {
  class Network(message: String? = null, cause: Throwable? = null) : [Feature]Error(message, cause)
  class Parse(message: String? = null, cause: Throwable? = null) : [Feature]Error(message, cause)
  class HttpFailure(val code: Int, message: String? = null, cause: Throwable? = null) : [Feature]Error(message, cause)
}
```

## Register in Koin

Add to `AppModule.kt`:

```kotlin
val appModule = module {
  // ... other dependencies
  
  // Repository (interface → implementation)
  single<[Noun]Repository> { Rest[Noun]Repository(get()) }
}
```

## Testing

Create fake in `app/src/test/java/com/julian/automaticclockwidget/fixtures/`:

```kotlin
/**
 * In-memory fake for [Noun]Repository used in unit tests.
 * Behavior mirrors production rules: [description]
 */
class Fake[Noun]Repository : [Noun]Repository {
  private val data = mutableListOf<[Model]>()
  
  override suspend fun [operation]([params]): Result<[ReturnType]> {
      // Implement test-friendly logic
      return Result.success([returnValue])
  }
}
```

## Example: AirportsRepository

Interface:
```kotlin
interface AirportsRepository {
  suspend fun findAirport(iataCode: String): Result<Airport>
}
```

Implementation:
```kotlin
class RestAirportRepository(private val client: OkHttpClient) : AirportsRepository {
  override suspend fun findAirport(iataCode: String) = withContext(Dispatchers.IO) {
      runCatching {
          // HTTP call, parse, map to domain model
      }.recoverCatching { t ->
          when (t) {
              is AirportError -> throw t
              is IOException -> throw AirportError.Network(cause = t)
              else -> throw UnknownError(cause = t)
          }
      }
  }
}
```

## Checklist

- [ ] Define repository interface in feature package
- [ ] Create implementation in `[source]/` subpackage
- [ ] Define data models with `@Serializable` in source package
- [ ] Add `.toDomainModel()` mapping functions
- [ ] Define domain errors in `core/AppError.kt`
- [ ] Use `runCatching` and `recoverCatching` for error handling
- [ ] Register in Koin module
- [ ] Create fake implementation for testing
- [ ] Write unit tests
- [ ] Run tests: `./gradlew :app:testDebugUnitTest`
