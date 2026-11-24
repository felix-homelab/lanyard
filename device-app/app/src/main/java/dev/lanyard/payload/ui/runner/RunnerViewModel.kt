package dev.lanyard.payload.ui.runner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.lanyard.payload.R
import dev.lanyard.payload.engine.DeviceSnapshot
import dev.lanyard.payload.engine.PayloadDigest
import dev.lanyard.payload.engine.RunEngine
import dev.lanyard.payload.engine.RunPhase
import dev.lanyard.payload.engine.TargetMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mirrors the app-scoped engine into a screen state. It never owns the run
 * itself: that lives in RunEngine so rotation and tab switches are free.
 */
class RunnerViewModel(private val engine: RunEngine) : ViewModel() {

    private val _state = MutableStateFlow(RunnerUiState())
    val state: StateFlow<RunnerUiState> = _state.asStateFlow()

    private val _effects = Channel<RunnerEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var lastPhase: RunPhase? = null

    init {
        engine.bootstrap()
        viewModelScope.launch {
            combine(engine.device, engine.relay, engine.runState) { device, relay, run ->
                Triple(device, relay, run)
            }.collect { (device, relay, run) ->
                _state.update { current ->
                    current.copy(
                        booting = device is DeviceSnapshot.Loading,
                        device = device.toDeviceUi(current.device),
                        gate = device.toGateUi(),
                        payload = device.toPayloadUi(current.payload),
                        relay = relay,
                        run = RunUi(
                            running = run.isRunning,
                            mode = run.mode,
                            outcome = run.outcome,
                            failure = run.failure,
                            finishedAt = run.finishedAt,
                            entries = run.entries,
                            dropped = run.droppedEntries,
                        ),
                    )
                }
                announceIfFinished(run.phase)
            }
        }
    }

    fun onEvent(event: RunnerEvent) {
        when (event) {
            RunnerEvent.RefreshRelay -> engine.refreshRelay()
            RunnerEvent.ProbeNative -> engine.probe()
            is RunnerEvent.Arm -> arm(event.mode)
            RunnerEvent.DismissConsent -> _state.update { it.copy(consent = null) }
            RunnerEvent.ConfirmRun -> confirmRun()
            RunnerEvent.ClearLog -> engine.clearLog()
            RunnerEvent.AcknowledgeRun -> engine.acknowledge()
        }
    }

    suspend fun payloadDigests(): List<PayloadDigest> = engine.payloadDigests()

    suspend fun manifestJson(): String? = engine.manifestJson()

    suspend fun lastRunJson(): String? = engine.lastRunJson()

    val lastRunPath: String get() = engine.lastRunPath

    private fun arm(mode: TargetMode) {
        val current = _state.value
        if (!current.armed) return
        _state.update { it.copy(consent = mode) }
    }

    private fun confirmRun() {
        val mode = _state.value.consent ?: return
        _state.update { it.copy(consent = null) }
        if (!engine.startRun(mode)) {
            _effects.trySend(RunnerEffect.ShowMessage(R.string.snackbar_run_in_flight))
        }
    }

    private fun announceIfFinished(phase: RunPhase) {
        if (phase == RunPhase.FINISHED && lastPhase != RunPhase.FINISHED) {
            _effects.trySend(RunnerEffect.ShowMessage(R.string.snackbar_run_finished))
        }
        lastPhase = phase
    }

    companion object {
        fun factory(engine: RunEngine) = viewModelFactory {
            initializer { RunnerViewModel(engine) }
        }
    }
}

private fun DeviceSnapshot.toDeviceUi(previous: DeviceUi?): DeviceUi? = when (this) {
    DeviceSnapshot.Loading -> previous
    is DeviceSnapshot.Failed -> null
    is DeviceSnapshot.Ready -> DeviceUi(
        manufacturer = facts.manufacturer,
        brand = facts.brand,
        model = facts.model,
        device = facts.device,
        product = facts.product,
        board = facts.board,
        hardware = facts.hardware,
        soc = facts.soc,
        socManufacturer = facts.socManufacturer,
        bootloader = facts.bootloader,
        build = facts.fingerprint,
        buildId = facts.buildId,
        incremental = facts.incremental,
        securityPatch = facts.securityPatch,
        buildType = facts.buildType,
        buildTags = facts.buildTags,
        kernel = facts.kernel,
        android = facts.release,
        sdk = facts.sdk,
        abi = facts.abi,
        appDomain = facts.appDomain,
    )
}

private fun DeviceSnapshot.toGateUi(): GateUi = when (this) {
    DeviceSnapshot.Loading -> GateUi.Booting
    is DeviceSnapshot.Failed -> GateUi.Broken(message)
    is DeviceSnapshot.Ready -> when (match.compatibility) {
        dev.lanyard.payload.PayloadManifest.Compatibility.VERIFIED -> GateUi.Verified(match.reason)
        dev.lanyard.payload.PayloadManifest.Compatibility.COMPATIBLE -> GateUi.Compatible(match.reason)
        else -> GateUi.Unsupported(match.reason)
    }
}

private fun DeviceSnapshot.toPayloadUi(previous: PayloadMetaUi?): PayloadMetaUi? = when (this) {
    DeviceSnapshot.Loading -> previous
    is DeviceSnapshot.Failed -> null
    is DeviceSnapshot.Ready -> PayloadMetaUi(
        id = manifest.id,
        version = manifest.version,
        family = manifest.family,
        capabilities = manifest.capabilities.toList(),
        persistence = manifest.persistence,
    )
}
