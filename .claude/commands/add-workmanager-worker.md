
# Adding a WorkManager Worker

WorkManager is Android's solution for deferrable, guaranteed background work.

## File Structure

In `app/src/main/java/com/julian/automaticclockwidget/workers/`:

1. **[Name]Worker.kt** - Worker implementation
2. **[Name]Scheduler.kt** (optional) - Helper for scheduling work

## Step 1: Create Worker Class

```kotlin
package com.julian.automaticclockwidget.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters

class [Name]Worker(
    val appContext: Context,
    params: WorkerParameters,
  private val someRepository: SomeRepository,
  private val someUseCase: SomeUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "[Name]Worker: starting work")
        
      return try {
            // Execute business logic
            someUseCase.execute()
                .fold(
                    onSuccess = { result ->
                        Log.i(TAG, "[Name]Worker: work completed successfully")
                        Result.success()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "[Name]Worker: work failed: ${error.message}", error)
                        Result.retry()
                    }
                )
      } catch (t: Throwable) {
            Log.e(TAG, "[Name]Worker: unexpected error: ${t.message}", t)
            Result.failure()
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

## Step 2: Register Worker in Koin

In **AppModule.kt**:

```kotlin
import org.koin.androidx.workmanager.dsl.worker

val appModule = module {
    // ... other dependencies
    
    worker { [Name]Worker(get(), get(), get(), get()) }
}
```

## Step 3: Configure Koin WorkManager Factory

In **AutomaticClockWidgetApplication.kt** (if not already done):

```kotlin
import org.koin.androidx.workmanager.koin.workManagerFactory

class AutomaticClockWidgetApplication : Application(), KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AutomaticClockWidgetApplication)
            workManagerFactory()  // Enable WorkManager factory
            modules(appModule)
      }
  }
}
```

## Step 4: Create Scheduler (Optional but Recommended)

```kotlin
package com.julian.automaticclockwidget.workers

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object [Name]Scheduler {
    
    private const val TAG = "[Name]Scheduler"
    
    fun scheduleOneTime(context: Context, delaySeconds: Long = 0) {
        val workRequest = OneTimeWorkRequestBuilder<[Name]Worker>()
            .setConstraints([Name]Worker.createConstraints())
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                [Name]Worker.UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        
        Log.i(TAG, "Scheduled one-time work with ${delaySeconds}s delay")
  }
    
    fun schedulePeriodic(context: Context, intervalHours: Long = 1) {
        val workRequest = PeriodicWorkRequestBuilder<[Name]Worker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints([Name]Worker.createConstraints())
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "${[Name]Worker.UNIQUE_WORK_NAME}_Periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        
        Log.i(TAG, "Scheduled periodic work every ${intervalHours}h")
  }
    
    fun cancel(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork([Name]Worker.UNIQUE_WORK_NAME)
        Log.i(TAG, "Cancelled work")
  }
}
```

## Step 5: Schedule Work

From Activity, Service, or BroadcastReceiver:

```kotlin
// One-time work
[Name]Scheduler.scheduleOneTime(context, delaySeconds = 5)

// Periodic work
[Name]Scheduler.schedulePeriodic(context, intervalHours = 6)

// Cancel work
[Name]Scheduler.cancel(context)
```

Or schedule directly:

```kotlin
val workRequest = OneTimeWorkRequestBuilder<[Name]Worker>()
    .setConstraints([Name]Worker.createConstraints())
    .build()

WorkManager.getInstance(context)
    .enqueueUniqueWork(
        [Name]Worker.UNIQUE_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        workRequest
    )
```

## WorkManager Patterns

### Result Types

- **Result.success()**: Work completed successfully, won't retry
- **Result.retry()**: Work failed but should be retried (exponential backoff)
- **Result.failure()**: Work failed permanently, won't retry

### Work Policies

**OneTime Work:**
- `REPLACE`: Cancel existing work and start new
- `KEEP`: Keep existing work, ignore new request
- `APPEND`: Chain work sequentially
- `APPEND_OR_REPLACE`: Append if existing work running, else replace

**Periodic Work:**
- `KEEP`: Keep existing periodic work
- `REPLACE`: Replace with new periodic work

### Constraints

Common constraints:

```kotlin
Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)     // Require any network
    .setRequiredNetworkType(NetworkType.UNMETERED)     // Require WiFi
    .setRequiresBatteryNotLow(true)                    // Battery > 15%
    .setRequiresCharging(true)                         // Device charging
    .setRequiresDeviceIdle(true)                       // Device idle (API 23+)
    .setRequiresStorageNotLow(true)                    // Storage available
    .build()
```

### Input/Output Data

Pass data to worker:

```kotlin
val inputData = workDataOf(
    "url" to "https://example.com",
    "retryCount" to 3
)

val workRequest = OneTimeWorkRequestBuilder<[Name]Worker>()
    .setInputData(inputData)
    .build()
```

Access in worker:

```kotlin
override suspend fun doWork(): Result {
    val url = inputData.getString("url") ?: return Result.failure()
    val retryCount = inputData.getInt("retryCount", 0)
    // ... use data
}
```

Return output data:

```kotlin
val outputData = workDataOf("result" to "success")
Result.success(outputData)
```

### Backoff Policy

Configure retry backoff:

```kotlin
OneTimeWorkRequestBuilder<[Name]Worker>()
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS
    )
    .build()
```

### Observing Work

```kotlin
WorkManager.getInstance(context)
    .getWorkInfosForUniqueWorkLiveData([Name]Worker.UNIQUE_WORK_NAME)
    .observe(lifecycleOwner) { workInfos ->
        workInfos.firstOrNull()?.let { workInfo ->
            when (workInfo.state) {
                WorkInfo.State.ENQUEUED -> Log.d(TAG, "Work enqueued")
                WorkInfo.State.RUNNING -> Log.d(TAG, "Work running")
                WorkInfo.State.SUCCEEDED -> Log.d(TAG, "Work succeeded")
                WorkInfo.State.FAILED -> Log.d(TAG, "Work failed")
                WorkInfo.State.CANCELLED -> Log.d(TAG, "Work cancelled")
                else -> {}
            }
      }
  }
```

## Testing Workers

```kotlin
class [Name]WorkerTest {
    
    @Test
    fun `given successful use case then worker returns success`() = runBlocking {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeRepo = FakeSomeRepository()
        val useCase = SomeUseCase(fakeRepo)
        
        val worker = TestListenableWorkerBuilder<[Name]Worker>(context)
            .build()
        
        // Inject dependencies (if using constructor injection)
        // worker.someRepository = fakeRepo
        
        // When
        val result = worker.doWork()
        
        // Then
        assertTrue(result is ListenableWorker.Result.Success)
  }
}
```

Add test dependency in `build.gradle.kts`:

```kotlin
testImplementation("androidx.work:work-testing:2.x.x")
```

## Best Practices

- **Use CoroutineWorker** for suspend functions (not Worker)
- **Return Result.retry()** for transient failures (network errors)
- **Return Result.failure()** for permanent failures
- **Log extensively** - workers run in background, hard to debug
- **Use constraints** to optimize battery and data usage
- **Keep work short** - WorkManager is for deferrable work, not long-running
- **Use unique work names** - prevent duplicate work
- **Handle cancellation** - check `isStopped` for long-running work

## Common Use Cases

- **Sync data** when network available
- **Periodic background refresh** (calendar, weather, etc.)
- **Upload logs or analytics** when on WiFi
- **Download updates** when charging and on WiFi
- **Clean up old data** during idle time

## Checklist

- [ ] Create worker class extending `CoroutineWorker`
- [ ] Inject dependencies via constructor
- [ ] Implement `doWork()` with proper error handling
- [ ] Return appropriate `Result` (success/retry/failure)
- [ ] Define constraints in companion object
- [ ] Add logging for debugging
- [ ] Register worker in Koin with `worker { }`
- [ ] Configure Koin WorkManager factory in Application
- [ ] Create scheduler helper (optional)
- [ ] Schedule work from appropriate location
- [ ] Test worker with WorkManager testing library
- [ ] Run app and verify work executes: check Logcat
