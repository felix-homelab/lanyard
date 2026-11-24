package dev.lanyard.payload.engine

import android.content.Context
import android.util.Log
import dev.lanyard.payload.DeviceFacts
import dev.lanyard.payload.PageCacheChain
import dev.lanyard.payload.PayloadManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

data class PayloadDigest(val entry: String, val sha256: String, val bytes: Int)

/**
 * Application-scoped owner of the payload run.
 *
 * It outlives every Activity, so a rotation (or a trip to another tab) never
 * detaches the native callback from the log. Process death still ends a run:
 * the native call cannot be migrated, and the page cache survives in the
 * kernel regardless of what this process does.
 */
class RunEngine(context: Context) {

    private val app: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastRun = LastRunStore(app)

    private val controller = RunController(
        executor = PayloadExecutor { mode, entry, onLog -> runPayload(mode, entry, onLog) },
        scope = scope,
        onFinished = { state -> persist(state) },
    )

    val runState: StateFlow<RunState> = controller.state

    private val _device = MutableStateFlow<DeviceSnapshot>(DeviceSnapshot.Loading)
    val device: StateFlow<DeviceSnapshot> = _device.asStateFlow()

    private val _relay = MutableStateFlow(RelayStatus())
    val relay: StateFlow<RelayStatus> = _relay.asStateFlow()

    val lastRunPath: String get() = lastRun.path

    fun bootstrap() {
        scope.launch {
            loadDevice()
            refreshRelayBlocking()
        }
    }

    fun refreshRelay() {
        scope.launch { refreshRelayBlocking() }
    }

    fun startRun(mode: TargetMode): Boolean = controller.start(mode)

    fun append(line: String) = controller.append(line)

    fun clearLog(): Boolean = controller.clearLog()

    fun acknowledge() = controller.acknowledge()

    /** Loads the arm64 relay and calls the non-writing probe. */
    fun probe() {
        controller.append("=== native relay check ===")
        scope.launch {
            val ok = try {
                PageCacheChain(app).probe { line -> controller.append(line) }
            } catch (error: Throwable) {
                controller.append("native relay check failed: " + (error.message ?: error.javaClass.simpleName))
                false
            }
            controller.append(if (ok) "native relay check: OK" else "native relay check: FAILED")
        }
    }

    suspend fun manifestJson(): String? = readAsset("payloads/" + PayloadManifest.ID + "/manifest.json")

    suspend fun lastRunJson(): String? = lastRun.read()

    fun payloadDigests(): List<PayloadDigest> {
        val ready = _device.value as? DeviceSnapshot.Ready ?: return emptyList()
        return TargetMode.entries.mapNotNull { mode ->
            try {
                val bytes = ready.manifest.payloadBytes(app, mode.entry)
                PayloadDigest(mode.entry, sha256(bytes), bytes.size)
            } catch (error: Exception) {
                Log.w(TAG, "digest failed for " + mode.entry, error)
                null
            }
        }
    }

    private fun loadDevice() {
        _device.value = try {
            val manifest = PayloadManifest.load(app)
            val facts = DeviceFacts.current()
            DeviceSnapshot.Ready(facts, manifest, manifest.match(facts))
        } catch (error: Exception) {
            Log.e(TAG, "manifest load failed", error)
            DeviceSnapshot.Failed(error.message ?: error.javaClass.simpleName)
        }
    }

    private fun refreshRelayBlocking() {
        _relay.value = _relay.value.copy(refreshing = true)
        _relay.value = RelayProbe.read()
    }

    private fun runPayload(mode: TargetMode, entry: String, onLog: (String) -> Unit): Boolean {
        val ready = _device.value as? DeviceSnapshot.Ready
        if (ready == null) {
            onLog("device snapshot is unavailable; refusing to run")
            return false
        }
        if (!ready.match.canRun()) {
            onLog("target gate refused: this is not a verified manifest row")
            return false
        }
        val payload = ready.manifest.payloadBytes(app, entry)
        if (ready.manifest.entrySize > 0 && payload.size != ready.manifest.entrySize) {
            throw IllegalStateException(
                "payload size " + payload.size + " does not match manifest " + ready.manifest.entrySize,
            )
        }
        onLog("asset loaded: " + payload.size + " bytes")
        return PageCacheChain(app).run(payload, mode.chainMode) { line -> onLog(line) }
    }

    private fun persist(state: RunState) {
        val ready = _device.value as? DeviceSnapshot.Ready ?: return
        lastRun.write(state, ready)
    }

    private fun readAsset(name: String): String? = try {
        app.assets.open(name).use { input ->
            ByteArrayOutputStream().use { output ->
                input.copyTo(output)
                output.toString(Charsets.UTF_8.name())
            }
        }
    } catch (error: Exception) {
        Log.w(TAG, "asset read failed: " + name, error)
        null
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { byte ->
            "%02x".format(byte)
        }

    private companion object {
        const val TAG = "LanyardPayload"
    }
}
