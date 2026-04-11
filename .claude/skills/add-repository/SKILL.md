---
name: add-repository
description: >
  Create a repository following this project's Clean Architecture repository pattern. Use when
  adding a new data source abstraction, implementing REST or local data access, or creating fake
  implementations for tests. Pattern is interface in feature package + SourceNounRepository
  implementation in source/ subpackage + domain model mapping. Triggers on "add a repository",
  "create repository", "implement data source", "add REST repository", "create fake repository".
allowed-tools: Read, Write, Edit, Bash, Glob, Grep
effort: low
tags: [repository, clean-architecture, data, scaffold]
---

# Add Repository

Repositories abstract data sources. Structure:
- `[feature]/[Noun]Repository.kt` — interface
- `[feature]/[source]/[Source][Noun]Repository.kt` — implementation
- `[feature]/[source]/Rest[Model].kt` — source data model with `.toDomainModel()`

## Step 1: Interface

```kotlin
// [feature]/[Noun]Repository.kt
package com.julian.automaticclockwidget.[feature]

interface [Noun]Repository {
    suspend fun [operation]([params]): Result<[ReturnType]>
}
```

## Step 2: Domain Model

```kotlin
// [feature]/[Model].kt
data class [Model](
    val field1: String,
    val field2: Int,
)
```

## Step 3: REST Implementation

```kotlin
// [feature]/rest/Rest[Noun]Repository.kt
class Rest[Noun]Repository(
    private val client: OkHttpClient,
) : [Noun]Repository {

    override suspend fun [operation]([params]) = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(URL).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw [Feature]Error.HttpFailure(response.code)
            }

            val body = response.body?.string() ?: throw [Feature]Error.Parse("Empty response")
            Json.decodeFromString<Rest[Model]>(body).toDomainModel()
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

## Step 4: Source Data Model with Mapping

```kotlin
// [feature]/rest/Rest[Model].kt
@Serializable
data class Rest[Model](
    val field1: String,
    val field2: Int,
) {
    fun toDomainModel() = [Model](field1 = field1, field2 = field2)
}
```

## Step 5: Domain Errors (`core/AppError.kt`)

```kotlin
sealed class [Feature]Error(
    message: String? = null,
    cause: Throwable? = null,
) : AppError(message, cause) {
    class Network(message: String? = null, cause: Throwable? = null) : [Feature]Error(message, cause)
    class Parse(message: String? = null, cause: Throwable? = null) : [Feature]Error(message, cause)
    class HttpFailure(val code: Int, message: String? = null, cause: Throwable? = null) : [Feature]Error(message, cause)
}
```

## Step 6: Register in Koin (`AppModule.kt`)

```kotlin
single<[Noun]Repository> { Rest[Noun]Repository(get()) }
```

## Step 7: Fake for Tests

```kotlin
// app/src/test/.../fixtures/Fake[Noun]Repository.kt
/**
 * In-memory fake for [Noun]Repository used in unit tests.
 * Behavior mirrors production rules: [describe key behaviors].
 */
class Fake[Noun]Repository : [Noun]Repository {
    private val data = mutableListOf<[Model]>()
    var shouldFail = false

    override suspend fun [operation]([params]): Result<[ReturnType]> {
        if (shouldFail) return Result.failure([Feature]Error.Network("Test error"))
        return Result.success(/* test data */)
    }
}
```

## Error Handling Rules

- `runCatching { }` wraps the entire implementation body
- `recoverCatching { }` maps raw exceptions to domain errors
- Always re-throw already-typed domain errors unchanged
- Map `IOException` → `FeatureError.Network`
- Map unexpected throwables → `UnknownError`

## Checklist

- [ ] Interface in `[feature]/[Noun]Repository.kt`
- [ ] Implementation in `[feature]/[source]/[Source][Noun]Repository.kt`
- [ ] Domain model in `[feature]/[Model].kt`
- [ ] Source model with `@Serializable` + `.toDomainModel()` in source package
- [ ] Domain errors in `core/AppError.kt`
- [ ] `runCatching` + `recoverCatching` error chain
- [ ] `single<Interface> { Implementation(get()) }` in `AppModule.kt`
- [ ] Fake in `test/.../fixtures/` for unit tests
- [ ] `./gradlew :app:testDebugUnitTest`
