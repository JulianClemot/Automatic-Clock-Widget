package com.julian.automaticclockwidget.core

/**
 * Domain-wide error model to wrap failures in repositories and use cases.
 * These errors extend [Exception] so they can be used directly with Kotlin Result failures.
 */
sealed class AppError(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

sealed class SettingsError(message: String? = null, cause: Throwable? = null) : AppError(message, cause) {
    /** Provided input (e.g., URL) is invalid. */
    class InvalidInput(message: String? = null, cause: Throwable? = null) : SettingsError(message, cause)

    /** Target item not found (e.g., selecting/deleting a URL that doesn’t exist). */
    class NotFound(message: String? = null, cause: Throwable? = null) : SettingsError(message, cause)

    /** Persistent storage problem (SharedPreferences/DataStore). */
    class StorageFailure(message: String? = null, cause: Throwable? = null) : SettingsError(message, cause)
}

sealed class CalendarError(message: String? = null, cause: Throwable? = null) : AppError(message, cause) {
    /** HTTP/network error while downloading calendar. */
    class Network(message: String? = null, cause: Throwable? = null) : CalendarError(message, cause)

    /** iCalendar parse error. */
    class Parse(message: String? = null, cause: Throwable? = null) : CalendarError(message, cause)

    /** Non-2xx response or server failure */
    class HttpFailure(val code: Int, message: String? = null, cause: Throwable? = null) : CalendarError(message, cause)
}

sealed class AirportError(message: String? = null, cause: Throwable? = null) : AppError(message, cause) {
    class NotFound(message: String? = null, cause: Throwable? = null) : AirportError(message, cause)
    class Network(message: String? = null, cause: Throwable? = null) : AirportError(message, cause)
}

/** Fallback for unexpected errors */
class UnknownError(message: String? = null, cause: Throwable? = null) : AppError(message, cause)
