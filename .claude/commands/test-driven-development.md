
# Test-Driven Development (TDD)

TDD is a development approach where you write tests before implementation code.

## TDD Cycle: Red-Green-Refactor

1. **Red**: Write a failing test
2. **Green**: Write minimal code to make test pass
3. **Refactor**: Improve code while keeping tests green
4. **Repeat**: Continue with next test

## TDD Workflow

### Step 1: Write a Failing Test (Red)

```kotlin
@Test
fun `given valid airport code when getting timezone then returns airport`() = runBlocking {
    // Given
    val fakeRepo = FakeAirportsRepository().apply {
        responses["JFK"] = Result.success(
            Airport("JFK", "New York", TimeZone.of("America/New_York"))
        )
  }
    val useCase = GetAirportTimezoneUseCase(fakeRepo)  // Doesn't exist yet!
    
    // When
    val result = useCase.getAirportTimezone("JFK")
    
    // Then
    assertTrue(result.isSuccess)
    assertEquals("JFK", result.getOrThrow().iataCode)
}
```

Run test: `./gradlew :app:testDebugUnitTest --tests 'GetAirportTimezoneUseCaseTest'`

**Expected**: Test fails (compilation error - class doesn't exist)

### Step 2: Write Minimal Implementation (Green)

Create minimal code to make test pass:

```kotlin
class GetAirportTimezoneUseCase(
  private val airportsRepository: AirportsRepository
) {
  suspend fun getAirportTimezone(iataCode: String): Result<Airport> {
      return airportsRepository.findAirport(iataCode)
  }
}
```

Run test again: **Expected**: Test passes ✓

### Step 3: Refactor (Keep Tests Green)

Improve code without breaking tests:

```kotlin
class GetAirportTimezoneUseCase(
  private val airportsRepository: AirportsRepository
) {
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

Run test: **Expected**: Still passes ✓

### Step 4: Add Next Test

```kotlin
@Test
fun `given invalid airport code when getting timezone then returns NotFound error`() = runBlocking {
    // Given
    val fakeRepo = FakeAirportsRepository().apply {
        responses["XXX"] = Result.failure(Exception("Not found"))
  }
    val useCase = GetAirportTimezoneUseCase(fakeRepo)
    
    // When
    val result = useCase.getAirportTimezone("XXX")
    
    // Then
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is AirportError.NotFound)
}
```

Run: **Expected**: Test passes ✓ (implementation already handles this)

## TDD for Use Cases

### Example: AddUrlUseCase

**Test 1: Happy path**
```kotlin
@Test
fun `given valid url when adding then url is added and selected`() {
    // Given
    val repo = FakeUrlPreferencesRepository()
    val useCase = AddUrlUseCase(repo)  // Write this next
    
    // When
    val result = useCase.addUrl("https://example.com")
    
    // Then
    assertTrue(result.isSuccess)
    assertTrue(repo.getUrls().getOrThrow().contains("https://example.com"))
}
```

**Implementation:**
```kotlin
class AddUrlUseCase(private val repository: UrlPreferencesRepository) {
    fun addUrl(url: String): Result<Unit> {
      return repository.addUrl(url)
  }
}
```

**Test 2: Validation**
```kotlin
@Test
fun `given blank url when adding then returns InvalidInput error`() {
    // Given
    val repo = FakeUrlPreferencesRepository()
    val useCase = AddUrlUseCase(repo)
    
    // When
    val result = useCase.addUrl("   ")
    
    // Then
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is SettingsError.InvalidInput)
}
```

**Updated Implementation:**
```kotlin
class AddUrlUseCase(private val repository: UrlPreferencesRepository) {
    fun addUrl(url: String): Result<Unit> {
        if (url.isBlank()) {
            return Result.failure(
                SettingsError.InvalidInput("URL cannot be blank")
            )
      }
      return repository.addUrl(url.trim())
  }
}
```

**Test 3: Trimming**
```kotlin
@Test
fun `given url with spaces when adding then url is trimmed`() {
    // Given
    val repo = FakeUrlPreferencesRepository()
    val useCase = AddUrlUseCase(repo)
    
    // When
    useCase.addUrl("  https://example.com  ")
    
    // Then
    val urls = repo.getUrls().getOrThrow()
    assertEquals("https://example.com", urls.first())
}
```

Tests pass ✓ (trim already implemented)

## TDD for Repositories

### Example: AirportsRepository with REST Implementation

**Test 1: Successful API call**
```kotlin
@Test
fun `given valid airport code when finding airport then returns airport data`() = runBlocking {
    // Given
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setBody("""
        {
            "code": "JFK",
            "city": "New York",
            "timezone": "America/New_York"
      }
    """.trimIndent()))
    mockServer.start()
    
    val client = OkHttpClient()
    val repository = RestAirportRepository(client, mockServer.url("/").toString())
    
    // When
    val result = repository.findAirport("JFK")
    
    // Then
    assertTrue(result.isSuccess)
    val airport = result.getOrThrow()
    assertEquals("JFK", airport.iataCode)
    assertEquals("New York", airport.city)
    
    mockServer.shutdown()
}
```

**Test 2: Network error**
```kotlin
@Test
fun `given network error when finding airport then returns Network error`() = runBlocking {
    // Given
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
    mockServer.start()
    
    val client = OkHttpClient()
    val repository = RestAirportRepository(client, mockServer.url("/").toString())
    
    // When
    val result = repository.findAirport("JFK")
    
    // Then
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is AirportError.Network)
    
    mockServer.shutdown()
}
```

**Test 3: HTTP error (404)**
```kotlin
@Test
fun `given 404 response when finding airport then returns NotFound error`() = runBlocking {
    // Given
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setResponseCode(404))
    mockServer.start()
    
    val client = OkHttpClient()
    val repository = RestAirportRepository(client, mockServer.url("/").toString())
    
    // When
    val result = repository.findAirport("XXX")
    
    // Then
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is AirportError.NotFound)
    
    mockServer.shutdown()
}
```

## TDD Benefits for This Project

### 1. Catch Errors Early

Test edge cases before they become bugs:
```kotlin
@Test
fun `given duplicate urls when adding then list is deduped`() {
    // Catches duplicate handling bugs early
}

@Test
fun `given empty calendar when getting clocks then returns empty list`() {
    // Ensures graceful handling of empty data
}
```

### 2. Document Behavior

Tests serve as living documentation:
```kotlin
@Test
fun `given urls added with spaces and duplicates then list is deduped trimmed and last is selected`() {
    // Documents exact business rules
}
```

### 3. Enable Refactoring

Refactor confidently with test safety net:
```kotlin
// Change implementation knowing tests will catch breaks
class RefreshTimezonesUseCase(...) {
    // Refactor this freely - tests ensure it still works
}
```

### 4. Better Design

Writing tests first leads to better API design:
```kotlin
// Test-first thinking makes you design clean, testable APIs
val result = useCase.execute(input)  // Clear, simple API
```

## TDD Best Practices

### Write Tests for All Business Logic

```kotlin
// ✅ Good - Test business logic
@Test
fun `given calendar events when filtering by date then returns only future events`()

@Test
fun `given invalid IATA code format when validating then returns error`()

// ❌ Avoid - Don't test framework code
@Test
fun `given string when trimming then whitespace removed`()  // Tests Kotlin stdlib
```

### One Assertion Per Test (Guideline)

```kotlin
// ✅ Preferred - Single concept
@Test
fun `given valid url when adding then url is added`() {
    useCase.addUrl("https://example.com")
    assertTrue(repo.getUrls().getOrThrow().contains("https://example.com"))
}

// ✅ Also OK - Related assertions for same concept
@Test
fun `given airport data when mapping to domain then all fields map correctly`() {
    val airport = restAirport.toAirport()
    assertEquals("JFK", airport.iataCode)
    assertEquals("New York", airport.city)
    assertEquals(TimeZone.of("America/New_York"), airport.timezone)
}
```

### Test Edge Cases

```kotlin
@Test
fun `given empty list when processing then returns empty result`()

@Test
fun `given null value when processing then handles gracefully`()

@Test
fun `given very long string when processing then truncates correctly`()

@Test
fun `given special characters when processing then escapes properly`()
```

### Keep Tests Fast

```kotlin
// ✅ Fast - Use fakes
val fakeRepo = FakeAirportsRepository()

// ❌ Slow - Don't make real network calls in unit tests
val realRepo = RestAirportRepository(realHttpClient)
```

### Keep Tests Independent

```kotlin
// ✅ Good - Each test creates own setup
@Test
fun `test one`() {
    val repo = FakeUrlPreferencesRepository()
    // Test isolated
}

@Test
fun `test two`() {
    val repo = FakeUrlPreferencesRepository()
    // Test isolated
}

// ❌ Bad - Tests share state
class MyTest {
    val sharedRepo = FakeUrlPreferencesRepository()  // Dangerous!
    
    @Test
    fun `test one`() {
        sharedRepo.addUrl("a")  // Affects test two
  }
}
```

## TDD Workflow Summary

### For Each Feature:

1. **Write failing test** describing desired behavior
2. **Run test** - verify it fails for right reason
3. **Write minimal code** to make test pass
4. **Run test** - verify it passes
5. **Refactor** code while keeping tests green
6. **Repeat** for next behavior

### Commands:

```bash
# Run tests continuously during TDD
./gradlew :app:testDebugUnitTest --continuous

# Run specific test class
./gradlew :app:testDebugUnitTest --tests 'MyUseCaseTest'

# Run with stacktrace for failures
./gradlew :app:testDebugUnitTest --stacktrace
```

## Example TDD Session

**Goal**: Implement `SelectUrlUseCase`

```kotlin
// 1. Write first test (RED)
@Test
fun `given existing url when selecting then url becomes selected`() {
    val repo = FakeUrlPreferencesRepository()
    repo.addUrl("https://example.com")
    val useCase = SelectUrlUseCase(repo)  // Doesn't exist yet
    
    val result = useCase.selectUrl("https://example.com")
    
    assertTrue(result.isSuccess)
    assertEquals("https://example.com", repo.getSelectedUrl().getOrThrow())
}
```

Run: FAILS (compilation error)

```kotlin
// 2. Write minimal implementation (GREEN)
class SelectUrlUseCase(private val repository: UrlPreferencesRepository) {
    fun selectUrl(url: String): Result<Unit> {
      return repository.selectUrl(url)
  }
}
```

Run: PASSES ✓

```kotlin
// 3. Write second test (RED)
@Test
fun `given non-existing url when selecting then returns NotFound error`() {
    val repo = FakeUrlPreferencesRepository()
    val useCase = SelectUrlUseCase(repo)
    
    val result = useCase.selectUrl("https://nonexistent.com")
    
    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is SettingsError.NotFound)
}
```

Run: PASSES ✓ (repository already handles this)

```kotlin
// 4. Refactor if needed
// Code is simple, no refactoring needed
```

## Checklist

- [ ] Write test before implementation (Red)
- [ ] Run test and verify it fails
- [ ] Write minimal code to pass (Green)
- [ ] Run test and verify it passes
- [ ] Refactor while keeping tests green
- [ ] Write tests for happy path first
- [ ] Then add tests for edge cases
- [ ] Then add tests for error cases
- [ ] Keep tests independent and fast
- [ ] Run all tests before committing
- [ ] Commit when all tests green ✓
