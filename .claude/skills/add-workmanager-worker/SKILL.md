---
name: add-workmanager-worker
description: >
  Create a WorkManager CoroutineWorker for this Android project. Use when implementing background
  work, periodic data refresh, or deferred tasks. Covers CoroutineWorker implementation, Koin
  worker registration, scheduling (one-time and periodic), constraints, and work policies.
  Triggers on "add a worker", "create WorkManager worker", "background work", "periodic refresh",
  "schedule background task", "add CoroutineWorker".
allowed-tools: Read, Write, Edit, Bash, Glob, Grep
effort: low
tags: [workmanager, background, worker, scaffold]
---

# Add WorkManager Worker

Workers live in `app/src/main/java/com/julian/automaticclockwidget/workers/`.

## Step 1: Worker Class

```kotlin
class [Name]Worker(
    val appContext: Context,
    params: WorkerParameters,
    private val someRepository: SomeRepository,
    private val someUseCase: SomeUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "$TAG: starting work")

        return try {
            someUseCase.execute()
                .fold(
                    onSuccess = {
                        Log.i(TAG, "$TAG: completed successfully")
                        Result.success()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "$TAG: failed: ${error.message}", error)
                        Result.retry()  // retry for transient errors
                    },
                )
        } catch (t: Throwable) {
            Log.e(TAG, "$TAG: unexpected error: ${t.message}", t)
            Result.failure()  // permanent failure
        }
    }

    companion object {
        private const val TAG = "[Name]Worker"
        const val UNIQUE_WORK_NAME = "[Name]Worker_Unique"

        fun createConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
    }
}
```

## Step 2: Register in Koin (`AppModule.kt`)

```kotlin
import org.koin.androidx.workmanager.dsl.worker

val appModule = module {
    worker { [Name]Worker(get(), get(), get(), get()) }
}
```

> Ensure `workManagerFactory()` is called in Application `startKoin { }` block.

## Step 3: Schedule Work

**One-time:**
```kotlin
val workRequest = OneTimeWorkRequestBuilder<[Name]Worker>()
    .setConstraints([Name]Worker.createConstraints())
    .setInitialDelay(5, TimeUnit.SECONDS)  // optional
    .build()

WorkManager.getInstance(context).enqueueUniqueWork(
    [Name]Worker.UNIQUE_WORK_NAME,
    ExistingWorkPolicy.REPLACE,
    workRequest,
)
```

**Periodic:**
```kotlin
val workRequest = PeriodicWorkRequestBuilder<[Name]Worker>(1, TimeUnit.HOURS)
    .setConstraints([Name]Worker.createConstraints())
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "${[Name]Worker.UNIQUE_WORK_NAME}_Periodic",
    ExistingPeriodicWorkPolicy.KEEP,
    workRequest,
)
```

## Result Types

| Result | When to use |
|--------|-------------|
| `Result.success()` | Work completed, no retry needed |
| `Result.retry()` | Transient failure (network, temp error) — exponential backoff |
| `Result.failure()` | Permanent failure — no retry |

## Work Policies

- `REPLACE` — cancel existing, start fresh (use for manual triggers)
- `KEEP` — ignore new request if work already queued (use for periodic)
- `APPEND` — chain sequentially

## Key Rules

- Always use `CoroutineWorker` (not `Worker`) for suspend functions
- Log extensively — workers run in background, hard to debug
- Use `UNIQUE_WORK_NAME` to prevent duplicate work
- Return `Result.retry()` for network/IO errors; `Result.failure()` for logic errors
- Set constraints to respect battery and network

## Checklist

- [ ] `CoroutineWorker` subclass in `workers/`
- [ ] Constructor injection of dependencies
- [ ] `doWork()` with proper Result handling
- [ ] Logging with TAG constant
- [ ] `createConstraints()` in companion object
- [ ] `worker { }` in `AppModule.kt`
- [ ] `workManagerFactory()` in Application (if not already present)
- [ ] Scheduling call from appropriate location (Activity/Application/Receiver)
- [ ] `./gradlew installDebug` and verify via Logcat
