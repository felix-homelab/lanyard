package dev.lanyard.payload.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.lanyard.payload.R
import dev.lanyard.payload.engine.LogLevel
import dev.lanyard.payload.ui.runner.DeviceUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
private val stampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

fun formatClock(millis: Long): String = synchronized(clockFormat) { clockFormat.format(Date(millis)) }

fun formatStamp(millis: Long): String = synchronized(stampFormat) { stampFormat.format(Date(millis)) }

@Composable
fun levelLabel(level: LogLevel): String = stringResource(
    when (level) {
        LogLevel.INFO -> R.string.level_info
        LogLevel.OK -> R.string.level_ok
        LogLevel.WARN -> R.string.level_warn
        LogLevel.FAIL -> R.string.level_fail
    },
)

/** A copy-pasteable record of this device, for docs/DEVICES.md. */
fun deviceFactsText(device: DeviceUi): String = buildString {
    appendLine("model=" + device.model)
    appendLine("device=" + device.device)
    appendLine("product=" + device.product)
    appendLine("manufacturer=" + device.manufacturer)
    appendLine("brand=" + device.brand)
    appendLine("board=" + device.board)
    appendLine("hardware=" + device.hardware)
    appendLine("soc=" + device.soc + " (" + device.socManufacturer + ")")
    appendLine("bootloader=" + device.bootloader)
    appendLine("build.fingerprint=" + device.build)
    appendLine("build.id=" + device.buildId)
    appendLine("build.incremental=" + device.incremental)
    appendLine("build.type=" + device.buildType)
    appendLine("build.tags=" + device.buildTags)
    appendLine("build.security_patch=" + device.securityPatch)
    appendLine("android=" + device.android + " (SDK " + device.sdk + ")")
    appendLine("abi=" + device.abi)
    appendLine("kernel=" + device.kernel)
    append("app.domain=" + device.appDomain)
}
