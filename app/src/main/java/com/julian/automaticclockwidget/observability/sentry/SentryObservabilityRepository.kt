package com.julian.automaticclockwidget.observability.sentry

import com.julian.automaticclockwidget.observability.ObservabilityRepository
import io.sentry.Sentry

class SentryObservabilityRepository : ObservabilityRepository {

    override suspend fun sendErrorEvent(throwable: Throwable) {
        Sentry.captureException(throwable)
    }
}
