package com.julian.automaticclockwidget.observability

enum class BreadcrumbLevel { DEBUG, INFO, WARNING, ERROR }

interface ObservabilityTransaction {
    fun setData(key: String, value: Any?)
    fun setStatus(status: String)
    fun startChild(operation: String, description: String? = null): ObservabilitySpan
    fun finish()
}

interface ObservabilitySpan {
    fun setData(key: String, value: Any?)
    fun setStatus(status: String)
    fun finish()
}

interface ObservabilityRepository {

    fun sendErrorEvent(
        throwable: Throwable,
        context: Map<String, Any?> = emptyMap(),
        tags: Map<String, String> = emptyMap(),
    )

    fun log(
        message: String,
        category: String = "app",
        level: BreadcrumbLevel = BreadcrumbLevel.INFO,
        data: Map<String, Any?> = emptyMap(),
    )

    fun startTransaction(name: String, operation: String): ObservabilityTransaction
}
