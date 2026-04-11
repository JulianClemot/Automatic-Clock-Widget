# Fake Patterns & Advanced Test Scenarios

## Table of Contents
1. [Configurable Fake with Responses Map](#configurable-fake)
2. [Testing Multiple Dependencies](#multiple-dependencies)
3. [Testing Data Transformations](#data-transformations)
4. [Testing ViewModels](#testing-viewmodels)
5. [Common Assertions Reference](#assertions)

---

## Configurable Fake with Responses Map {#configurable-fake}

Use a `responses` map when the fake needs to return different results per input:

```kotlin
class FakeAirportsRepository : AirportsRepository {
    val responses = mutableMapOf<String, Result<Airport>>()
    var shouldFail = false

    override suspend fun findAirport(iataCode: String): Result<Airport> {
        if (shouldFail) return Result.failure(AirportError.Network("Test error"))
        return responses[iataCode]
            ?: Result.failure(AirportError.NotFound("$iataCode not found"))
    }
}

// Usage:
val fakeRepo = FakeAirportsRepository().apply {
    responses["JFK"] = Result.success(Airport("JFK", "New York", TimeZone.of("America/New_York")))
    responses["LHR"] = Result.failure(AirportError.NotFound("LHR not found"))
}
```

---

## Testing Multiple Dependencies {#multiple-dependencies}

```kotlin
@Test
fun `given calendar with events when getting clocks then returns matching airports`() = runBlocking {
    // Given
    val fakeCalRepo = FakeCalendarsRepository().apply {
        result = Result.success(Calendar("id", Events(listOf(
            Event("Flight - ON123 - JFK", startDate, endDate)
        ))))
    }
    val fakeAirRepo = FakeAirportsRepository().apply {
        responses["JFK"] = Result.success(Airport("JFK", "New York", TimeZone.of("America/New_York")))
    }
    val downloadUC = DownloadCalendarUseCase(fakeCalRepo)
    val airportUC = GetAirportTimezoneUseCase(fakeAirRepo)
    val useCase = GetUpcomingClocksUseCase(downloadUC, airportUC)

    // When
    val result = useCase.getUpcomingClocks("https://cal.ics", startDate)

    // Then
    assertTrue(result.isSuccess)
    assertEquals(1, result.getOrThrow().size)
    assertEquals("JFK", result.getOrThrow().first().iataCode)
}
```

---

## Testing Data Transformations {#data-transformations}

Test `.toDomainModel()` mapping functions directly:

```kotlin
@Test
fun `given rest airport when mapping to domain then all fields are correct`() {
    // Given
    val restAirport = RestAirport(code = "JFK", city = "New York", timezone = "America/New_York")

    // When
    val airport = restAirport.toAirport()

    // Then
    assertEquals("JFK", airport.iataCode)
    assertEquals("New York", airport.city)
    assertEquals(TimeZone.of("America/New_York"), airport.timezone)
}
```

---

## Testing ViewModels {#testing-viewmodels}

```kotlin
@Test
fun `given valid url when adding then state updates without error`() = runBlocking {
    // Given
    val fakeRepo = FakeUrlPreferencesRepository()
    val addUseCase = AddUrlUseCase(fakeRepo)
    val viewModel = HomeViewModel(addUseCase, ...)

    // When
    viewModel.onEvent(HomeUiEvent.AddUrl("https://example.com"))
    delay(100) // Allow StateFlow to propagate

    // Then
    val state = viewModel.uiState.value
    assertNull(state.errorMessage)
    assertTrue(state.urls.contains("https://example.com"))
}
```

---

## Common Assertions Reference {#assertions}

```kotlin
// Boolean
assertTrue(condition)
assertFalse(condition)

// Equality
assertEquals(expected, actual)
assertNotEquals(unexpected, actual)

// Nullability
assertNull(value)
assertNotNull(value)

// Collections
assertEquals(emptyList<Item>(), list)
assertTrue(list.contains(item))
assertTrue(list.isEmpty())
assertEquals(3, list.size)

// Result<T>
assertTrue(result.isSuccess)
assertTrue(result.isFailure)
assertEquals(expected, result.getOrThrow())
assertTrue(result.exceptionOrNull() is SomeError)
assertEquals("msg", result.exceptionOrNull()?.message)
```
