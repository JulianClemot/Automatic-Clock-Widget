package com.julian.automaticclockwidget.observability

interface ObservabilityRepository {

    suspend fun sendErrorEvent(throwable: Throwable)
}
