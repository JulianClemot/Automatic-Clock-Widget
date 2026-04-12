package com.julian.automaticclockwidget.observability.sentry

import com.julian.automaticclockwidget.observability.BreadcrumbLevel
import com.julian.automaticclockwidget.observability.ObservabilityRepository
import com.julian.automaticclockwidget.observability.ObservabilitySpan
import com.julian.automaticclockwidget.observability.ObservabilityTransaction
import io.sentry.Breadcrumb
import io.sentry.ISpan
import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SpanStatus

class SentryObservabilityRepository : ObservabilityRepository {

    override fun sendErrorEvent(
        throwable: Throwable,
        context: Map<String, Any?>,
        tags: Map<String, String>,
    ) {
        Sentry.withScope { scope ->
            tags.forEach { (key, value) -> scope.setTag(key, value) }
            context.forEach { (key, value) -> scope.setContexts(key, value ?: "null") }
            Sentry.captureException(throwable)
        }
    }

    override fun log(
        message: String,
        category: String,
        level: BreadcrumbLevel,
        data: Map<String, Any?>,
    ) {
        val breadcrumb = Breadcrumb(message).apply {
            this.category = category
            this.level = level.toSentryLevel()
            data.forEach { (key, value) -> setData(key, value) }
        }
        Sentry.addBreadcrumb(breadcrumb)
    }

    override fun startTransaction(name: String, operation: String): ObservabilityTransaction {
        val transaction = Sentry.startTransaction(name, operation)
        return SentryTransactionAdapter(transaction)
    }

    private fun BreadcrumbLevel.toSentryLevel(): SentryLevel = when (this) {
        BreadcrumbLevel.DEBUG -> SentryLevel.DEBUG
        BreadcrumbLevel.INFO -> SentryLevel.INFO
        BreadcrumbLevel.WARNING -> SentryLevel.WARNING
        BreadcrumbLevel.ERROR -> SentryLevel.ERROR
    }
}

private class SentryTransactionAdapter(private val tx: ITransaction) : ObservabilityTransaction {

    override fun setData(key: String, value: Any?) {
        tx.setData(key, value ?: "null")
    }

    override fun setStatus(status: String) {
        tx.status = status.toSpanStatus()
    }

    override fun startChild(operation: String, description: String?): ObservabilitySpan {
        val span = if (description != null) tx.startChild(operation, description) else tx.startChild(operation)
        return SentrySpanAdapter(span)
    }

    override fun finish() {
        tx.finish()
    }
}

private class SentrySpanAdapter(private val span: ISpan) : ObservabilitySpan {

    override fun setData(key: String, value: Any?) {
        span.setData(key, value ?: "null")
    }

    override fun setStatus(status: String) {
        span.status = status.toSpanStatus()
    }

    override fun finish() {
        span.finish()
    }
}

private fun String.toSpanStatus(): SpanStatus = when (this) {
    "ok" -> SpanStatus.OK
    "internal_error" -> SpanStatus.INTERNAL_ERROR
    "not_found" -> SpanStatus.NOT_FOUND
    "cancelled" -> SpanStatus.CANCELLED
    else -> SpanStatus.UNKNOWN_ERROR
}
