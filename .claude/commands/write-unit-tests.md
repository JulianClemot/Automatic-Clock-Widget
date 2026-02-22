
# Writing Unit Tests

This project uses JUnit 4 with comprehensive unit testing for use cases and business logic.

## Test Organization

- **Location**: `app/src/test/java/com/julian/automaticclockwidget/`
- **Structure**: Mirror production code structure
- **Naming**: `[ClassName]Test.kt`
- **Fakes**: Store in `fixtures/` package for reuse

## Test Naming Convention

Use backticks with **Given-When-Then** pattern in full sentences:

```kotlin
@Test
fun `given valid input when executing use case then returns success`() {
    // Test implementation
}

@Test
fun `given network error when fetching data then returns failure with NetworkError`() {
    // Test implementation
}
```

### Naming Guidelines

- **Given**: Describe preconditions and test setup
- **When**: Describe the action being tested
- **Then**: Describe expected outcome
- Use lowercase, separate clauses with "when" and "then"
- Be specific and descriptive

**Good Examples:**
```kotlin
`given urls added with spaces and duplicates then list is deduped trimmed and last is selected`
`given blank url then addUrl fails with InvalidInput and state remains empty`
`given mixed events and airport lookups then returns only successful airports preserving order`
`given calendar download fails then use case forwards failure`
```

## Test Structure Pattern

```kotlin
package com.julian.automaticclockwidget.[feature]

import com.julian.automaticclockwidget.fixtures.Fake[Feature]Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class [ClassName]Test {

    @Test
    fun `given [condition] when [action] then [expected result]`() {
        // Given - Setup test dependencies and state
        val fakeRepository = Fake[Feature]Repository()
        val useCase = [ClassName](fakeRepository)
        
        // When - Execute the action being tested
        val result = useCase.execute(params)
        // For suspend functions: runBlocking { useCase.execute(params) }
        
        // Then - Assert expected outcomes
        assertTrue(result.isSuccess)
        assertEquals(expectedValue, result.getOrThrow())
  }
}
```

## Testing Suspend Functions

Use `runBlocking` for suspend functions:

```kotlin
@Test
fun `given valid airport code when getting timezone then returns airport`() = runBlocking {
    // Given
    val fakeRepo = FakeAirportsRepository().apply {
        responses["JFK"] = Result.success(
            Airport("JFK", "New York", TimeZone.of("America/New_York"))
        )
  }
    val useCase = GetAirportTimezoneUseCase(fakeRepo)
    
    // When
    val result = useCase.getAirportTimezone("JFK")
    
    // Then
    assertTrue(result.isSuccess)
    val airport = result.getOrThrow()
    assertEquals("JFK", airport.iataCode)
    assertEquals("New York", airport.city)
}
```

## Testing Result Types

### Success Cases

```kotlin
@Test
fun `test success scenario`() {
    // When
    val result = useCase.execute()
    
    // Then - Multiple assertion styles
    assertTrue(result.isSuccess)
    assertFalse(result.isFailure)
    
    val value = result.getOrThrow()
    assertEquals(expected, value)
    
    // Or using fold
    result.fold(
        onSuccess = { value ->
            assertEquals(expected, value)
      },
        onFailure = {
            fail("Expected success but got failure")
      }
    )
}
```

### Failure Cases

```kotlin
@Test
fun `test failure scenario with specific error`() {
    // When
    val result = useCase.execute()
    
    // Then - Assert failure
    assertTrue(result.isFailure)
    assertFalse(result.isSuccess)
    
    // Assert error type
    val error = result.exceptionOrNull()
    assertTrue(error is SettingsError.InvalidInput)
    
    // Assert error message
    assertEquals("Expected error message", error?.message)
}
```

## Creating Fake Implementations

Store fakes in `app/src/test/java/com/julian/automaticclockwidget/fixtures/`:

```kotlin
package com.julian.automaticclockwidget.fixtures

import com.julian.automaticclockwidget.[feature].[Feature]Repository

/**
 * In-memory fake for [Feature]Repository used in unit tests.
 * Behavior mirrors production rules:
 * - [Document key behaviors]
 * - [Document edge cases]
 */
class Fake[Feature]Repository : [Feature]Repository {
    
    // In-memory storage
  private val items = mutableListOf<Item>()
    
    // For testing specific scenarios
    var shouldFail = false
    var responses = mutableMapOf<String, Result<Data>>()
    
    override suspend fun getData(id: String): Result<Data> {
        if (shouldFail) {
            return Result.failure([Feature]Error.Network("Test error"))
      }
        
        // Return pre-configured response
        responses[id]?.let { return it }
        
        // Default behavior
      return items.find { it.id == id }
            ?.let { Result.success(it) }
            ?: Result.failure([Feature]Error.NotFound("Item not found"))
  }
    
    override fun addItem(item: Item): Result<Unit> {
        items.add(item)
      return Result.success(Unit)
  }
}
```

### Fake Best Practices

- **Mirror production behavior** exactly
- **Document behavior** in KDoc comments
- **Use mutable state** for test setup
- **Provide test hooks** (shouldFail, custom responses)
- **Keep simple** - no complex logic
- **Reuse across tests** - one fake per repository

## Testing Use Cases

### Simple Use Case

```kotlin
@Test
fun `given valid input when executing use case then returns expected result`() {
    // Given
    val fakeRepo = FakeSomeRepository()
    val useCase = SomeUseCase(fakeRepo)
    
    // When
    val result = useCase.execute("input")
    
    // Then
    assertTrue(result.isSuccess)
    assertEquals(expectedOutput, result.getOrThrow())
}
```

### Use Case with Multiple Dependencies

```kotlin
@Test
fun `given calendar with events when getting clocks then returns airports`() = runBlocking {
    // Given
    val fakeCalRepo = FakeCalendarsRepository().apply {
        result = Result.success(Calendar("id", Events(listOf(
            Event("Flight - 123ON - JFK", startDate, endDate)
        ))))
  }
    val downloadUC = DownloadCalendarUseCase(fakeCalRepo)
    
    val fakeAirRepo = FakeAirportsRepository().apply {
        responses["JFK"] = Result.success(
            Airport("JFK", "New York", TimeZone.of("America/New_York"))
        )
  }
    val airportUC = GetAirportTimezoneUseCase(fakeAirRepo)
    
    val useCase = GetUpcomingClocksUseCase(downloadUC, airportUC)
    
    // When
    val result = useCase.getUpcomingClocks("https://cal.ics", startDate)
    
    // Then
    assertTrue(result.isSuccess)
    val airports = result.getOrThrow()
    assertEquals(1, airports.size)
    assertEquals("JFK", airports.first().iataCode)
}
```

## Testing Error Handling

```kotlin
@Test
fun `given repository error when executing then use case maps error correctly`() = runBlocking {
    // Given
    val fakeRepo = FakeSomeRepository().apply {
        shouldFail = true
  }
    val useCase = SomeUseCase(fakeRepo)
    
    // When
    val result = useCase.execute()
    
    // Then
    assertTrue(result.isFailure)
    val error = result.exceptionOrNull()
    assertTrue(error is SomeError.Network)
}
```

## Testing Edge Cases

```kotlin
@Test
fun `given empty list when processing then returns empty result`() {
    // Given
    val fakeRepo = FakeSomeRepository()
    val useCase = ProcessItemsUseCase(fakeRepo)
    
    // When
    val result = useCase.process(emptyList())
    
    // Then
    assertTrue(result.isSuccess)
    assertEquals(emptyList<Item>(), result.getOrThrow())
}

@Test
fun `given null input when executing then returns InvalidInput error`() {
    // Given
    val useCase = ValidateInputUseCase()
    
    // When
    val result = useCase.validate(null)
    
    // Then
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is ValidationError.InvalidInput)
}
```

## Testing ViewModels (Advanced)

For ViewModels with StateFlow:

```kotlin
class HomeViewModelTest {
    
    @Test
    fun `given valid url when adding then state updates with new url`() = runBlocking {
        // Given
        val fakeRepo = FakeUrlPreferencesRepository()
        val addUseCase = AddUrlUseCase(fakeRepo)
        val getUseCase = GetUrlStateUseCase(fakeRepo)
        val viewModel = HomeViewModel(addUseCase, getUseCase, ...)
        
        // Collect initial state
        val states = mutableListOf<HomeUiState>()
        val job = launch {
            viewModel.uiState.toList(states)
      }
        
        // When
        viewModel.onEvent(HomeUiEvent.AddUrl("https://example.com"))
        delay(100) // Allow state to update
        
        // Then
        val currentState = viewModel.uiState.value
        assertTrue(currentState.urls.contains("https://example.com"))
        assertNull(currentState.errorMessage)
        
        job.cancel()
  }
}
```

Add test dependencies:

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.x.x")
testImplementation("app.cash.turbine:turbine:1.x.x") // For testing flows
```

## Testing Data Transformations

```kotlin
@Test
fun `given rest airport when converting to domain then fields map correctly`() {
    // Given
    val restAirport = RestAirport(
        code = "JFK",
        city = "New York",
        timezone = "America/New_York"
    )
    
    // When
    val airport = restAirport.toAirport()
    
    // Then
    assertEquals("JFK", airport.iataCode)
    assertEquals("New York", airport.city)
    assertEquals(TimeZone.of("America/New_York"), airport.timezone)
}
```

## Common Assertions

```kotlin
// Boolean checks
assertTrue(condition)
assertFalse(condition)

// Equality
assertEquals(expected, actual)
assertNotEquals(unexpected, actual)

// Nullability
assertNull(value)
assertNotNull(value)

// Collections
assertEquals(expectedList, actualList)
assertTrue(list.contains(item))
assertTrue(list.isEmpty())
assertEquals(3, list.size)

// Result types
assertTrue(result.isSuccess)
assertTrue(result.isFailure)
assertEquals(expected, result.getOrThrow())
assertTrue(result.exceptionOrNull() is ErrorType)

// Exception types
val error = result.exceptionOrNull()
assertTrue(error is SpecificError)
assertEquals("Expected message", error?.message)
```

## Test Coverage Guidelines

For each use case, test:

1. **Happy path** - Normal successful execution
2. **Edge cases** - Empty input, null values, boundary conditions
3. **Error cases** - Each type of domain error
4. **Business rules** - Validation, transformation, filtering logic

## Running Tests

```bash
# All unit tests
./gradlew :app:testDebugUnitTest

# Specific test class
./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.settings.AddUrlUseCaseTest'

# Specific test method (exact name in backticks)
./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.settings.AddUrlUseCaseTest.given urls added with spaces and duplicates then list is deduped trimmed and last is selected'

# With coverage
./gradlew :app:testDebugUnitTest jacocoTestReport
```

## Checklist

- [ ] Create test file mirroring production structure
- [ ] Name tests with Given-When-Then in backticks
- [ ] Create fake implementations in fixtures/
- [ ] Test happy path (success scenarios)
- [ ] Test edge cases (empty, null, boundaries)
- [ ] Test error cases (all error types)
- [ ] Test business rules and transformations
- [ ] Use appropriate assertions
- [ ] Run tests: `./gradlew :app:testDebugUnitTest`
- [ ] Verify all tests pass before committing
