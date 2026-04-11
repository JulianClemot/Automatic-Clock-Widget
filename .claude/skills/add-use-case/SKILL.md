---
name: add-use-case
description: >
  Create a new use case following this project's Clean Architecture use case pattern. Use when
  implementing a new business operation, encapsulating domain logic, or wiring a repository
  operation. Naming convention is VerbNounUseCase (e.g. GetAirportTimezoneUseCase, AddUrlUseCase).
  Triggers on "add a use case", "create use case", "implement business logic", "add domain operation".
allowed-tools: Read, Write, Edit, Glob, Grep
effort: low
tags: [use-case, clean-architecture, domain, scaffold]
---

# Add Use Case

Use cases live in `app/src/main/java/com/julian/automaticclockwidget/[feature]/` and are named `VerbNounUseCase`.

## Template

```kotlin
package com.julian.automaticclockwidget.[feature]

class [VerbNoun]UseCase(
    private val repository: [Feature]Repository,
) {
    suspend fun [verbNoun]([params]): Result<[ReturnType]> {
        return repository.[operation]([params])
            .recoverCatching { throwable ->
                when (throwable) {
                    is [Feature]Error -> throw throwable
                    else -> throw [Feature]Error.Unknown(cause = throwable)
                }
            }
    }
}
```

## Rules

- **Single public method** — one use case, one responsibility
- **Naming** — method name mirrors the use case (camelCase)
- **Return type** — `Result<T>` for all fallible operations
- **Suspend** — always `suspend` for I/O or async work
- **Error handling** — use `recoverCatching` to map raw exceptions to domain errors
- **No business logic in repositories** — transform/filter/validate here, not in the repo

## Real Example

```kotlin
class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {
    suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
        return airportsRepository.findAirport(iataCode)
            .recoverCatching { throwable ->
                throw AirportError.NotFound(
                    message = "Airport $iataCode not found",
                    cause = throwable,
                )
            }
    }
}
```

## Register in Koin (`AppModule.kt`)

```kotlin
single { [VerbNoun]UseCase(get()) }
```

## Test Template

```kotlin
// app/src/test/java/com/julian/automaticclockwidget/[feature]/[VerbNoun]UseCaseTest.kt
class [VerbNoun]UseCaseTest {

    @Test
    fun `given [condition] when [action] then [expected result]`() = runBlocking {
        // Given
        val fakeRepo = Fake[Feature]Repository()
        val useCase = [VerbNoun]UseCase(fakeRepo)

        // When
        val result = useCase.[verbNoun]([params])

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrThrow())
    }
}
```

## Checklist

- [ ] Class in `[feature]/` package, named `VerbNounUseCase`
- [ ] Single public `suspend` method returning `Result<T>`
- [ ] `recoverCatching` for error mapping to domain errors
- [ ] Registered in `AppModule.kt` with `single { }`
- [ ] Tests with Given-When-Then naming in backticks
- [ ] `./gradlew :app:testDebugUnitTest`
