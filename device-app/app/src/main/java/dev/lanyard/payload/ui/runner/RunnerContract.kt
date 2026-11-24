package dev.lanyard.payload.ui.runner

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import dev.lanyard.payload.engine.LogEntry
import dev.lanyard.payload.engine.RelayStatus
import dev.lanyard.payload.engine.RunOutcome
import dev.lanyard.payload.engine.TargetMode

@Immutable
data class DeviceUi(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val hardware: String,
    val soc: String,
    val socManufacturer: String,
    val bootloader: String,
    val build: String,
    val buildId: String,
    val incremental: String,
    val securityPatch: String,
    val buildType: String,
    val buildTags: String,
    val kernel: String,
    val android: String,
    val sdk: Int,
    val abi: String,
    val appDomain: String,
)

sealed interface GateUi {
    data object Booting : GateUi
    data class Verified(val reason: String) : GateUi
    data class Compatible(val reason: String) : GateUi
    data class Unsupported(val reason: String) : GateUi
    data class Broken(val message: String) : GateUi
}

@Immutable
data class PayloadMetaUi(
    val id: String,
    val version: String,
    val family: String,
    val capabilities: List<String>,
    val persistence: String,
)

@Immutable
data class RunUi(
    val running: Boolean = false,
    val mode: TargetMode? = null,
    val outcome: RunOutcome? = null,
    val failure: String? = null,
    val finishedAt: Long? = null,
    val entries: List<LogEntry> = emptyList(),
    val dropped: Int = 0,
)

@Immutable
data class RunnerUiState(
    val booting: Boolean = true,
    val device: DeviceUi? = null,
    val gate: GateUi = GateUi.Booting,
    val payload: PayloadMetaUi? = null,
    val relay: RelayStatus = RelayStatus(),
    val run: RunUi = RunUi(),
    val consent: TargetMode? = null,
) {
    val armed: Boolean get() = gate is GateUi.Verified && !run.running
}

sealed interface RunnerEvent {
    data object RefreshRelay : RunnerEvent
    data object ProbeNative : RunnerEvent
    data class Arm(val mode: TargetMode) : RunnerEvent
    data object DismissConsent : RunnerEvent
    data object ConfirmRun : RunnerEvent
    data object ClearLog : RunnerEvent
    data object AcknowledgeRun : RunnerEvent
}

sealed interface RunnerEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : RunnerEffect
}
