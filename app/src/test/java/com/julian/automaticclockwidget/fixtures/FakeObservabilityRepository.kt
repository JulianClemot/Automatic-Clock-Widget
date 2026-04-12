package com.julian.automaticclockwidget.fixtures

import com.julian.automaticclockwidget.observability.BreadcrumbLevel
import com.julian.automaticclockwidget.observability.ObservabilityRepository
import com.julian.automaticclockwidget.observability.ObservabilitySpan
import com.julian.automaticclockwidget.observability.ObservabilityTransaction

/**
 * No-op fake for ObservabilityRepository used in unit tests.
 * Records errors and logs so tests can assert on them if needed.
 */
class FakeObservabilityRepository : ObservabilityRepository {

    val recordedErrors = mutableListOf<Throwable>()
    val recordedLogs = mutableListOf<String>()

    override fun sendErrorEvent(
        throwable: Throwable,
        context: Map<String, Any?>,
        tags: Map<String, String>,
    ) {
        recordedErrors.add(throwable)
    }

    override fun log(
        message: String,
        category: String,
        level: BreadcrumbLevel,
        data: Map<String, Any?>,
    ) {
        recordedLogs.add(message)
    }

    override fun startTransaction(name: String, operation: String): ObservabilityTransaction =
        NoOpTransaction()
}

private class NoOpTransaction : ObservabilityTransaction {
    override fun setData(key: String, value: Any?) = Unit
    override fun setStatus(status: String) = Unit
    override fun startChild(operation: String, description: String?): ObservabilitySpan = NoOpSpan()
    override fun finish() = Unit
}

private class NoOpSpan : ObservabilitySpan {
    override fun setData(key: String, value: Any?) = Unit
    override fun setStatus(status: String) = Unit
    override fun finish() = Unit
}
