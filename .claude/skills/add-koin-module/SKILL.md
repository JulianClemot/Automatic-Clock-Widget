---
name: add-koin-module
description: >
  Register dependencies in Koin for this Android project. Use when wiring a new use case,
  repository, ViewModel, or WorkManager worker into the dependency injection graph. Covers all
  declaration types used in this project: single, viewModel, worker, interface bindings, and
  androidApplication context. All registrations go in AppModule.kt. Triggers on "register in
  Koin", "add to Koin", "wire up DI", "inject dependency", "Koin setup", "add to app module",
  "Koin registration".
allowed-tools: Read, Edit, Glob, Grep
effort: low
tags: [koin, di, dependency-injection, scaffold]
---

# Add Koin Module

All registrations live in `AppModule.kt` inside the single `appModule` block.

## Declaration Types

### Use Case
```kotlin
single { GetAirportTimezoneUseCase(get()) }
single { AddUrlUseCase(get()) }
```

### Repository (interface → implementation)
```kotlin
single<AirportsRepository> { RestAirportRepository(get()) }
single<UrlPreferencesRepository> { UrlPreferencesRepositoryImpl(get()) }
```

### ViewModel
```kotlin
// Import: org.koin.core.module.dsl.viewModel
viewModel<HomeViewModel> {
    HomeViewModel(get(), get(), get(), androidApplication())
}
```
`androidApplication()` provides the `Application` context — use it when the ViewModel needs `Context`.

### WorkManager Worker
```kotlin
// Import: org.koin.androidx.workmanager.dsl.worker
worker { CalendarRefreshWorker(get(), get(), get(), get(), get()) }
```
Workers use `worker { }` (not `single`). Requires `workManagerFactory()` in the Application (already set up).

### Singleton with qualifier or dispatcher
```kotlin
single<WidgetUpdateUseCase> { GlanceWidgetUpdateUseCase(get(), Dispatchers.Default) }
```

## How `get()` Works

Each `get()` resolves a registered dependency by type. Order of `get()` calls matches the constructor parameter order:

```kotlin
// Constructor: RefreshTimezonesUseCase(repo1, repo2, useCase3)
single { RefreshTimezonesUseCase(get(), get(), get()) }
//                                 ↑      ↑      ↑
//                           AirportsRepo  ClocksRepo  DownloadUseCase
```

Koin resolves each `get()` by matching its declared type — make sure every type has a registration.

## Where to Add in `AppModule.kt`

Follow the existing grouping order:
```kotlin
val appModule = module {

    // 1. ViewModels
    viewModel<MyViewModel> { MyViewModel(get(), androidApplication()) }

    // 2. Use cases
    single { MyUseCase(get()) }

    // 3. Repositories (interface → implementation)
    single<MyRepository> { MyRepositoryImpl(get()) }

    // 4. Workers
    worker { MyWorker(get(), get(), get()) }

    // 5. Networking / infrastructure
    single<OkHttpClient> { /* ... */ }
}
```

## Consuming in Compose

```kotlin
// In a composable (EntryPoint):
val viewModel: MyViewModel = koinViewModel()
// Import: org.koin.androidx.compose.koinViewModel
```

## Consuming outside Compose

```kotlin
// In a class implementing KoinComponent:
val useCase: MyUseCase by inject()

// Or imperatively:
val useCase = getKoin().get<MyUseCase>()
```

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| `single { MyRepository(...) }` for interface | Use `single<MyRepository> { MyRepositoryImpl(...) }` |
| `single { MyWorker(...) }` | Use `worker { MyWorker(...) }` |
| `get()` count doesn't match constructor params | Count params carefully, including `appContext: Context` |
| Missing `workManagerFactory()` in Application | Already configured — don't touch Application unless adding a new module |

## Checklist

- [ ] Add declaration to `AppModule.kt` in the right group
- [ ] Use `single<Interface> { Impl(get()) }` for repository bindings
- [ ] Use `viewModel<VM> { VM(get(), ..., androidApplication()) }` for ViewModels
- [ ] Use `worker { }` for WorkManager workers
- [ ] Count `get()` calls to match constructor parameter count
- [ ] Add imports at top of `AppModule.kt`
- [ ] Build to verify no Koin injection errors at runtime
