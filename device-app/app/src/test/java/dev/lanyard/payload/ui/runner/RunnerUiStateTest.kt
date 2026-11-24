package dev.lanyard.payload.ui.runner

import dev.lanyard.payload.engine.RunOutcome
import dev.lanyard.payload.engine.TargetMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunnerUiStateTest {

    @Test
    fun `only a verified gate without a run in flight is armed`() {
        val verified = RunnerUiState(gate = GateUi.Verified("ok"))
        assertTrue(verified.armed)

        val running = verified.copy(run = RunUi(running = true, mode = TargetMode.INIT))
        assertFalse(running.armed)

        val compatible = RunnerUiState(gate = GateUi.Compatible("untested"))
        assertFalse(compatible.armed)

        val unsupported = RunnerUiState(gate = GateUi.Unsupported("wrong abi"))
        assertFalse(unsupported.armed)

        val booting = RunnerUiState()
        assertFalse(booting.armed)
    }

    @Test
    fun `a finished run keeps its outcome next to the gate`() {
        val state = RunnerUiState(
            gate = GateUi.Verified("ok"),
            run = RunUi(outcome = RunOutcome.VERIFIED),
        )
        assertTrue(state.armed)
        assertTrue(state.run.outcome == RunOutcome.VERIFIED)
    }
}
