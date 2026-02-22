
# Adding a Use Case

Use cases encapsulate single business operations in the domain layer.

## Structure

1. **Location**: `app/src/main/java/com/julian/automaticclockwidget/[feature]/`
2. **Naming**: `VerbNounUseCase` (e.g., `GetAirportTimezoneUseCase`, `AddUrlUseCase`)
3. **File**: `[VerbNoun]UseCase.kt`

## Template

```kotlin
package com.julian.automaticclockwidget.[feature]

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class [VerbNoun]UseCase(
  private val repository: [Feature]Repository,
) {
  suspend fun [verbNoun]([params]): Result<[ReturnType]> {
      return withContext(Dispatchers.IO) {
          repository.[operation]([params])
      }
  }
}
```

## Rules

- **Single responsibility**: One public method only
- **Naming**: Method name matches use case purpose (camelCase)
- **Return type**: `Result<T>` for operations that can fail
- **Async**: Use `suspend` for I/O or long-running operations
- **Dependencies**: Inject via constructor (repositories, other use cases)
- **Error handling**: Use `recoverCatching` to transform errors into domain errors

## Register in Koin

Add to `AppModule.kt`:

```kotlin
val appModule = module {
  // ... other dependencies
  single { [VerbNoun]UseCase(get()) }
}
```

## Testing

Create `app/src/test/java/com/julian/automaticclockwidget/[feature]/[VerbNoun]UseCaseTest.kt`:

```kotlin
class [VerbNoun]UseCaseTest {
  
  @Test
  fun `given [condition] when [action] then [expected result]`() {
      // Given
      val fakeRepo = Fake[Feature]Repository()
      val useCase = [VerbNoun]UseCase(fakeRepo)
      
      // When
      val result = runBlocking { useCase.[verbNoun]([params]) }
      
      // Then
      assertTrue(result.isSuccess)
      assertEquals(expected, result.getOrThrow())
  }
}
```

## Example: GetAirportTimezoneUseCase

```kotlin
class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {
  suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
      return airportsRepository.findAirport(iataCode)
          .recoverCatching { throwable ->
              throw AirportError.NotFound(
                  message = "Airport $iataCode not found",
                  cause = throwable
              )
          }
  }
}
```

## Checklist

- [ ] Create use case class in feature package
- [ ] Single public method with descriptive name
- [ ] Return `Result<T>` for fallible operations
- [ ] Use `suspend` for async work
- [ ] Register in Koin `appModule`
- [ ] Create unit tests with Given-When-Then
- [ ] Run tests: `./gradlew :app:testDebugUnitTest`
