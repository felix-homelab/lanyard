package dev.lanyard.payload.engine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong

/** Blocking payload executor. Called from the worker dispatcher. */
fun interface PayloadExecutor {
    fun run(mode: TargetMode, entry: String, onLog: (String) -> Unit): Boolean
}

/**
 * The single-run state machine. One run at a time, one log, one owner.
 *
 * The executor blocks on a native call, so it is never cancelled; the
 * controller only refuses to start a second run while one is in flight.
 */
class RunController(
    private val executor: PayloadExecutor,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxEntries: Int = 2_000,
    private val onFinished: (RunState) -> Unit = {},
) {
    private val _state = MutableStateFlow(RunState())
    val state: StateFlow<RunState> = _state.asStateFlow()

    private val nextEntryId = AtomicLong(0L)

    /** Returns false when a run is already in flight. */
    fun start(mode: TargetMode): Boolean {
        if (_state.value.isRunning) return false
        val startedAt = clock()
        _state.update {
            it.copy(
                phase = RunPhase.RUNNING,
                mode = mode,
                startedAt = startedAt,
                finishedAt = null,
                outcome = null,
                failure = null,
                droppedEntries = 0,
            )
        }
        scope.launch {
            var outcome = RunOutcome.FAILED
            var failure: String? = null
            try {
                val verified = withContext(workerDispatcher) {
                    executor.run(mode, mode.entry, ::append)
                }
                outcome = if (verified) RunOutcome.VERIFIED else RunOutcome.NOT_VERIFIED
            } catch (error: Throwable) {
                failure = error.javaClass.simpleName + ": " + (error.message ?: "no message")
                append("run failed: " + failure)
            }
            _state.update {
                it.copy(
                    phase = RunPhase.FINISHED,
                    outcome = outcome,
                    failure = failure,
                    finishedAt = clock(),
                )
            }
            onFinished(_state.value)
        }
        return true
    }

    /** Append a line to the current log. Safe from any thread. */
    fun append(message: String) {
        val text = message.trimEnd('\n')
        if (text.isEmpty()) return
        val entry = LogEntry(
            id = nextEntryId.getAndIncrement(),
            at = clock(),
            level = LogClassifier.classify(text),
            message = text,
        )
        _state.update { current ->
            val entries = current.entries + entry
            if (entries.size <= maxEntries) {
                current.copy(entries = entries)
            } else {
                current.copy(
                    entries = entries.takeLast(maxEntries),
                    droppedEntries = current.droppedEntries + (entries.size - maxEntries),
                )
            }
        }
    }

    /** Clears the log. Refused while a run is in flight. */
    fun clearLog(): Boolean {
        if (_state.value.isRunning) return false
        _state.update { it.copy(entries = emptyList(), droppedEntries = 0) }
        return true
    }

    /** Drops the previous run's outcome but keeps its log. */
    fun acknowledge() {
        if (_state.value.isRunning) return
        _state.update {
            it.copy(
                phase = RunPhase.IDLE,
                mode = null,
                startedAt = 0L,
                finishedAt = null,
                outcome = null,
                failure = null,
            )
        }
    }
}
