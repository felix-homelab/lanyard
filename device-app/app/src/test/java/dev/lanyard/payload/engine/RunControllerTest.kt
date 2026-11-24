package dev.lanyard.payload.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RunControllerTest {

    private fun controller(
        onFinished: (RunState) -> Unit = {},
        maxEntries: Int = 2_000,
        executor: PayloadExecutor,
    ) = RunController(
        executor = executor,
        scope = CoroutineScope(Dispatchers.Unconfined),
        workerDispatcher = Dispatchers.Unconfined,
        maxEntries = maxEntries,
        onFinished = onFinished,
    )

    @Test
    fun `a verified run ends in the verified outcome and keeps its log`() {
        val controller = controller { _, _, onLog ->
            onLog("asset loaded: 576 bytes")
            onLog("Verified: 576/576 bytes")
            true
        }

        assertTrue(controller.start(TargetMode.INIT))

        val state = controller.state.value
        assertEquals(RunPhase.FINISHED, state.phase)
        assertEquals(RunOutcome.VERIFIED, state.outcome)
        assertEquals(TargetMode.INIT, state.mode)
        assertEquals(2, state.entries.size)
        assertEquals(LogLevel.OK, state.entries.last().level)
        assertFalse(state.isRunning)
    }

    @Test
    fun `an unverified run is not reported as a failure`() {
        val controller = controller { _, _, _ -> false }

        controller.start(TargetMode.BLUETOOTH)

        val state = controller.state.value
        assertEquals(RunOutcome.NOT_VERIFIED, state.outcome)
        assertEquals(null, state.failure)
    }

    @Test
    fun `a throwing executor ends in failed with the message recorded`() {
        val controller = controller { _, _, _ -> error("IpSecManager is unavailable") }

        controller.start(TargetMode.INIT)

        val state = controller.state.value
        assertEquals(RunOutcome.FAILED, state.outcome)
        assertTrue(state.failure!!.contains("IpSecManager is unavailable"))
        assertTrue(state.entries.last().message.startsWith("run failed:"))
    }

    @Test
    fun `the executor receives the manifest entry for the mode`() {
        var seenEntry: String? = null
        val controller = controller { _, entry, _ ->
            seenEntry = entry
            true
        }

        controller.start(TargetMode.BLUETOOTH)

        assertEquals(TargetMode.BLUETOOTH.entry, seenEntry)
    }

    @Test
    fun `a second run is refused while one is in flight`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val controller = RunController(
            executor = { _, _, _ ->
                entered.countDown()
                release.await(5, TimeUnit.SECONDS)
                true
            },
            scope = CoroutineScope(Dispatchers.IO),
        )

        assertTrue(controller.start(TargetMode.INIT))
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        assertFalse(controller.start(TargetMode.BLUETOOTH))

        release.countDown()
        runBlocking {
            withTimeout(5_000) { controller.state.first { it.phase == RunPhase.FINISHED } }
        }
    }

    @Test
    fun `the log is capped and reports how much it dropped`() {
        val controller = controller(
            executor = { _, _, onLog ->
                repeat(5) { onLog("line " + it) }
                true
            },
            maxEntries = 3,
        )

        controller.start(TargetMode.INIT)

        val state = controller.state.value
        assertEquals(3, state.entries.size)
        assertEquals(2, state.droppedEntries)
        assertEquals("line 4", state.entries.last().message)
    }

    @Test
    fun `the log cannot be cleared while a run is in flight`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val controller = RunController(
            executor = { _, _, _ ->
                entered.countDown()
                release.await(5, TimeUnit.SECONDS)
                true
            },
            scope = CoroutineScope(Dispatchers.IO),
        )

        controller.start(TargetMode.INIT)
        assertTrue(entered.await(5, TimeUnit.SECONDS))

        assertFalse(controller.clearLog())

        release.countDown()
        runBlocking {
            withTimeout(5_000) { controller.state.first { it.phase == RunPhase.FINISHED } }
        }
        assertTrue(controller.clearLog())
        assertTrue(controller.state.value.entries.isEmpty())
    }

    @Test
    fun `the finish callback sees the completed state once`() {
        val finished = mutableListOf<RunState>()
        val controller = controller(
            executor = { _, _, _ -> true },
            onFinished = { finished += it },
        )

        controller.start(TargetMode.INIT)
        controller.clearLog()
        controller.acknowledge()

        assertEquals(1, finished.size)
        assertEquals(RunOutcome.VERIFIED, finished.single().outcome)
    }
}
