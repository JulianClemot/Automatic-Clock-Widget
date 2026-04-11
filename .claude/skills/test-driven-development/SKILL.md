---
name: test-driven-development
description: >
  Apply Test-Driven Development (TDD) workflow to implement features in this Android project. Use
  when building new use cases, repositories, or business logic test-first. Follows the
  Red-Green-Refactor cycle with JUnit 4, Given-When-Then naming, Fake repositories, and Result types.
  Triggers on "use TDD", "test first", "write test before implementation", "TDD approach",
  "red green refactor", "test-driven".
allowed-tools: Read, Write, Edit, Bash, Glob, Grep
effort: medium
tags: [tdd, testing, junit4, android, red-green-refactor]
---

# Test-Driven Development

## Red → Green → Refactor Cycle

1. **Red** — write a failing test describing the desired behavior
2. **Green** — write the minimal code to make it pass
3. **Refactor** — improve the code while keeping tests green
4. **Repeat** — add the next test

```bash
# Run continuously during TDD:
./gradlew :app:testDebugUnitTest --continuous
```

## Step-by-Step Example

### 1. Write Failing Test (Red)

```kotlin
@Test
fun `given valid airport code when getting timezone then returns airport`() = runBlocking {
    val fakeRepo = FakeAirportsRepository().apply {
        responses["JFK"] = Result.success(Airport("JFK", "New York", TimeZone.of("America/New_York")))
    }
    val useCase = GetAirportTimezoneUseCase(fakeRepo)  // ← doesn't exist yet

    val result = useCase.getAirportTimezone("JFK")

    assertTrue(result.isSuccess)
    assertEquals("JFK", result.getOrThrow().iataCode)
}
```

Run → **FAILS** (compilation error — class missing). ✓ Expected.

### 2. Write Minimal Implementation (Green)

```kotlin
class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {
    suspend fun getAirportTimezone(iataCode: String): Result<Airport> =
        airportsRepository.findAirport(iataCode)
}
```

Run → **PASSES** ✓

### 3. Refactor (still Green)

```kotlin
class GetAirportTimezoneUseCase(private val airportsRepository: AirportsRepository) {
    suspend fun getAirportTimezone(iataCode: String): Result<Airport> =
        airportsRepository.findAirport(iataCode)
            .recoverCatching { throwable ->
                throw AirportError.NotFound(message = "Airport $iataCode not found", cause = throwable)
            }
}
```

Run → **PASSES** ✓

### 4. Add Next Test (repeat)

```kotlin
@Test
fun `given unknown airport code when getting timezone then returns NotFound error`() = runBlocking {
    val fakeRepo = FakeAirportsRepository()  // empty — nothing pre-configured
    val useCase = GetAirportTimezoneUseCase(fakeRepo)

    val result = useCase.getAirportTimezone("XXX")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is AirportError.NotFound)
}
```

## TDD Order for a Feature

1. Happy path test (success scenario)
2. Validation test (invalid/blank input)
3. Edge case tests (empty, null, boundary)
4. Error tests (each domain error type)

## Test Quality Rules

- **One concept per test** — multiple assertions OK if they verify the same thing
- **Independent tests** — each test creates its own fakes (no shared state)
- **Fast** — use Fakes, never real network calls
- **Descriptive names** — the test name IS the documentation

```kotlin
// ✅ Tests business logic
fun `given duplicate urls when adding then list is deduped`()

// ❌ Tests stdlib — skip this
fun `given string when calling trim then whitespace removed`()
```

## TDD with Use Cases

```
Write test for happy path
→ Create use case class + method (minimal)
→ Test passes
→ Write test for validation
→ Add validation to use case
→ Test passes
→ Write test for error case
→ Add recoverCatching block
→ Test passes
→ Register in Koin
→ Done
```

## Reference Files

- **[tdd-examples.md](references/tdd-examples.md)** — complete TDD session examples for AddUrl, SelectUrl, and Repository patterns
