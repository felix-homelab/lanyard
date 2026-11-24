package dev.lanyard.payload.engine

import dev.lanyard.payload.DeviceFacts
import dev.lanyard.payload.PayloadManifest

sealed interface DeviceSnapshot {
    data object Loading : DeviceSnapshot
    data class Ready(
        val facts: DeviceFacts,
        val manifest: PayloadManifest,
        val match: PayloadManifest.Match,
    ) : DeviceSnapshot
    data class Failed(val message: String) : DeviceSnapshot
}
