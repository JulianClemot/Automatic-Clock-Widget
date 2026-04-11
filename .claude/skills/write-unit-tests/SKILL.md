---
name: write-unit-tests
description: >
  Write JUnit 4 unit tests for this Android project following its testing conventions. Use when
  adding tests for use cases, repositories, ViewModels, or data transformations. Covers
  Given-When-Then test naming in backtick format, Fake implementations over mocks, Result type
  assertions, suspend function testing with runBlocking, and test organization in the fixtures/ package.
  Triggers on "write tests", "add unit tests", "test this use case", "write a test for",
  "create test class", "add test coverage".
allowed-tools: Read, Write, Edit, Bash, Glob, Grep
effort: low
tags: [testing, junit4, android, fakes, unit-tests]
---

# Write Unit Tests

Tests mirror production structure in `app/src/test/java/com/julian/automaticclockwidget/`.
Fakes live in the `fixtures/` subpackage.

## Naming Convention

Backtick Given-When-Then full sentences:

```kotlin
@Test
fun `given valid url when adding then url is saved and selected`() { }

@Test
fun `given network error when fetching airport then returns Network error`() { }

@Test
fun `given urls with spaces and duplicates when adding then list is deduped and trimmed`() { }
```

## Test Structure

```kotlin
class [ClassName]Test {

    @Test
    fun `given [condition] when [action] then [expected result]`() = runBlocking {
        // Given
        val fakeRepo = Fake[Feature]Repository()
        val useCase = [ClassName](fakeRepo)

        // When
        val result = useCase.execute(params)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrThrow())
    }
}
```

## Fake Pattern

```kotlin
// fixtures/Fake[Noun]Repository.kt
/**
 * In-memory fake for [Noun]Repository used in unit tests.
 * Behavior mirrors production rules: [describe invariants].
 */
class Fake[Noun]Repository : [Noun]Repository {
    private val items = mutableListOf<Item>()
    var shouldFail = false

    override suspend fun getData(id: String): Result<Data> {
        if (shouldFail) return Result.failure([Feature]Error.Network("Test error"))
        return items.find { it.id == id }
            ?.let { Result.success(it) }
            ?: Result.failure([Feature]Error.NotFound("Not found"))
    }

    override suspend fun addItem(item: Item): Result<Unit> {
        items.add(item)
        return Result.success(Unit)
    }
}
```

**Fake rules:** mirror production behavior exactly; use `shouldFail` flag for error scenarios; reuse one fake per repository across test classes.

## Result Assertions

```kotlin
// Success
assertTrue(result.isSuccess)
assertEquals(expected, result.getOrThrow())

// Failure
assertTrue(result.isFailure)
val error = result.exceptionOrNull()
assertTrue(error is SettingsError.InvalidInput)
assertEquals("Expected message", error?.message)

// Using fold
result.fold(
    onSuccess = { assertEquals(expected, it) },
    onFailure = { fail("Expected success but got: $it") },
)
```

## Coverage Guidelines

For each use case/class, test:
1. **Happy path** — normal successful execution
2. **Edge cases** — empty input, null values, boundary conditions
3. **Error cases** — each type of domain error
4. **Business rules** — validation, transformation, deduplication logic

## Run Commands

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:testDebugUnitTest --tests 'com.julian.automaticclockwidget.[package].[ClassName]Test'
# Exact method (use backtick-quoted name):
./gradlew :app:testDebugUnitTest --tests 'com.julian...ClassName.given X when Y then Z'
```

## Reference Files

- **[fake-patterns.md](references/fake-patterns.md)** — detailed fake implementation patterns and complex test scenarios
