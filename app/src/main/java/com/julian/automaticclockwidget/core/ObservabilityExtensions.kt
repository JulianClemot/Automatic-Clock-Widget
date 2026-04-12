package com.julian.automaticclockwidget.core

/**
 * Returns only the host part of the URL to avoid logging sensitive tokens or paths.
 */
fun sanitizeUrl(raw: String): String = try {
    java.net.URI(raw.replace("webcal://", "https://")).host ?: "unknown"
} catch (_: Exception) {
    "unknown"
}

/**
 * Converts a [Throwable] into a structured context map for observability error events.
 * Includes error type and any domain-specific fields.
 */
fun Throwable.toErrorContext(): Map<String, Any?> {
    val base: MutableMap<String, Any?> = mutableMapOf(
        "errorType" to this::class.simpleName,
        "message" to message,
    )
    when (this) {
        is CalendarError.HttpFailure -> base["httpCode"] = code
        is AirportError.NotFound -> base["airportMessage"] = message
        is SettingsError.InvalidInput -> base["inputMessage"] = message
        is SettingsError.NotFound -> base["settingsMessage"] = message
    }
    return base
}
