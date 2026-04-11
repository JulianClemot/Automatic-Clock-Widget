# TDD Session Examples

## Table of Contents
1. [AddUrlUseCase TDD Session](#addurl-session)
2. [SelectUrlUseCase TDD Session](#selecturl-session)
3. [REST Repository TDD Session](#repository-session)

---

## AddUrlUseCase TDD Session {#addurl-session}

### Test 1: Happy path (Red → Green)

```kotlin
@Test
fun `given valid url when adding then url is saved`() {
    val repo = FakeUrlPreferencesRepository()
    val useCase = AddUrlUseCase(repo)  // RED: doesn't exist

    val result = useCase.addUrl("https://example.com")

    assertTrue(result.isSuccess)
    assertTrue(repo.getUrls().getOrThrow().contains("https://example.com"))
}
```

Minimal implementation:
```kotlin
class AddUrlUseCase(private val repository: UrlPreferencesRepository) {
    fun addUrl(url: String): Result<Unit> = repository.addUrl(url)
}
```

→ GREEN ✓

### Test 2: Blank URL validation

```kotlin
@Test
fun `given blank url when adding then returns InvalidInput error`() {
    val useCase = AddUrlUseCase(FakeUrlPreferencesRepository())

    val result = useCase.addUrl("   ")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is SettingsError.InvalidInput)
}
```

Updated implementation:
```kotlin
fun addUrl(url: String): Result<Unit> {
    if (url.isBlank()) return Result.failure(SettingsError.InvalidInput("URL cannot be blank"))
    return repository.addUrl(url.trim())
}
```

→ GREEN ✓

### Test 3: Trimming (already passing from previous refactor)

```kotlin
@Test
fun `given url with spaces when adding then url is trimmed`() {
    val repo = FakeUrlPreferencesRepository()
    val useCase = AddUrlUseCase(repo)

    useCase.addUrl("  https://example.com  ")

    assertEquals("https://example.com", repo.getUrls().getOrThrow().first())
}
```

→ GREEN ✓ (already implemented)

---

## SelectUrlUseCase TDD Session {#selecturl-session}

### Test 1: Select existing URL

```kotlin
@Test
fun `given existing url when selecting then url becomes selected`() {
    val repo = FakeUrlPreferencesRepository().apply { addUrl("https://example.com") }
    val useCase = SelectUrlUseCase(repo)  // RED

    val result = useCase.selectUrl("https://example.com")

    assertTrue(result.isSuccess)
    assertEquals("https://example.com", repo.getSelectedUrl().getOrThrow())
}
```

Minimal implementation:
```kotlin
class SelectUrlUseCase(private val repository: UrlPreferencesRepository) {
    fun selectUrl(url: String): Result<Unit> = repository.selectUrl(url)
}
```

→ GREEN ✓

### Test 2: Select non-existing URL

```kotlin
@Test
fun `given non-existing url when selecting then returns NotFound error`() {
    val useCase = SelectUrlUseCase(FakeUrlPreferencesRepository())

    val result = useCase.selectUrl("https://nonexistent.com")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is SettingsError.NotFound)
}
```

→ GREEN ✓ (fake already handles missing URLs)

---

## REST Repository TDD Session {#repository-session}

Uses MockWebServer for HTTP layer testing.

### Test 1: Successful response

```kotlin
@Test
fun `given valid response when finding airport then returns airport domain model`() = runBlocking {
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setBody("""
        {"code":"JFK","city":"New York","timezone":"America/New_York"}
    """.trimIndent()))
    mockServer.start()

    val repository = RestAirportRepository(OkHttpClient(), mockServer.url("/").toString())
    val result = repository.findAirport("JFK")

    assertTrue(result.isSuccess)
    assertEquals("JFK", result.getOrThrow().iataCode)
    mockServer.shutdown()
}
```

### Test 2: Network failure

```kotlin
@Test
fun `given network error when finding airport then returns Network error`() = runBlocking {
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
    mockServer.start()

    val repository = RestAirportRepository(OkHttpClient(), mockServer.url("/").toString())
    val result = repository.findAirport("JFK")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is AirportError.Network)
    mockServer.shutdown()
}
```

### Test 3: HTTP 404

```kotlin
@Test
fun `given 404 response when finding airport then returns NotFound error`() = runBlocking {
    val mockServer = MockWebServer()
    mockServer.enqueue(MockResponse().setResponseCode(404))
    mockServer.start()

    val repository = RestAirportRepository(OkHttpClient(), mockServer.url("/").toString())
    val result = repository.findAirport("XXX")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is AirportError.NotFound)
    mockServer.shutdown()
}
```
