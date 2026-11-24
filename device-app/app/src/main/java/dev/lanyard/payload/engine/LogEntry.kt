package dev.lanyard.payload.engine

import androidx.compose.runtime.Immutable

enum class LogLevel { INFO, OK, WARN, FAIL }

@Immutable
data class LogEntry(
    val id: Long,
    val at: Long,
    val level: LogLevel,
    val message: String,
)

/**
 * The native relay emits free-form lines. This maps each line onto a level so
 * the log can colour the one line that matters without changing what the
 * relay prints. Failure markers are checked before success markers, because
 * "did not report a verified page-cache write" contains "verified".
 */
object LogClassifier {
    private val failureMarkers = listOf(
        "failed",
        "failure",
        "error",
        "unavailable",
        "invalid",
        "did not",
        "no result",
        "refused",
        "denied",
        "not loaded",
        "unknown target",
    )

    private val successMarkers = listOf(
        "verified:",
        "page cache modified",
        "ready",
        " ok",
        "success",
    )

    private val warningMarkers = listOf(
        "warn",
        "may ",
        "denied /proc",
    )

    fun classify(message: String): LogLevel {
        val text = message.lowercase()
        if (failureMarkers.any(text::contains)) return LogLevel.FAIL
        if (successMarkers.any(text::contains)) return LogLevel.OK
        if (warningMarkers.any(text::contains)) return LogLevel.WARN
        return LogLevel.INFO
    }
}
