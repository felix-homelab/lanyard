package dev.lanyard.payload.engine

/**
 * The two vendored Open Crown entries, and the mode value the native relay
 * expects. The integers mirror PageCacheChain's TARGET_INIT / TARGET_BT.
 */
enum class TargetMode(val entry: String, val chainMode: Int, val wireName: String) {
    INIT("payload.bin", 0, "init"),
    BLUETOOTH("payload_bt_read.bin", 1, "bluetooth"),
}
