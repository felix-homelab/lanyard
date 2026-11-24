package dev.lanyard.payload.engine

import androidx.compose.runtime.Immutable

enum class RunPhase { IDLE, RUNNING, FINISHED }

enum class RunOutcome { VERIFIED, NOT_VERIFIED, FAILED }

/**
 * Everything one payload run has produced. Held by the app-scoped
 * RunController, so a rotation never drops a run or its log.
 */
@Immutable
data class RunState(
    val phase: RunPhase = RunPhase.IDLE,
    val mode: TargetMode? = null,
    val startedAt: Long = 0L,
    val finishedAt: Long? = null,
    val outcome: RunOutcome? = null,
    val failure: String? = null,
    val entries: List<LogEntry> = emptyList(),
    val droppedEntries: Int = 0,
) {
    val isRunning: Boolean get() = phase == RunPhase.RUNNING

    fun tail(maxChars: Int): String {
        val builder = StringBuilder()
        for (entry in entries.asReversed()) {
            if (builder.length + entry.message.length + 1 > maxChars) break
            builder.insert(0, entry.message + "\n")
        }
        return builder.toString()
    }
}
