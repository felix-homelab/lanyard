package dev.lanyard.payload.engine

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.compose.runtime.Immutable
import java.io.BufferedReader
import java.io.FileReader

enum class MarkerState { PRESENT, PRESENT_DENIED, ABSENT, ERROR, UNKNOWN }

enum class SocketState { LISTENING, ABSENT, UNREADABLE }

@Immutable
data class RelayStatus(
    val initMarker: MarkerState = MarkerState.UNKNOWN,
    val initSocket: SocketState = SocketState.UNREADABLE,
    val btMarker: MarkerState = MarkerState.UNKNOWN,
    val btSocket: SocketState = SocketState.UNREADABLE,
    val at: Long = 0L,
    val refreshing: Boolean = false,
)

/**
 * Reads the two markers and the two abstract sockets the relay creates.
 *
 * The app domain is expected to be denied @dfi_init and is often denied
 * /proc/net/unix; both are reported as such rather than as an error.
 */
object RelayProbe {
    fun read(now: Long = System.currentTimeMillis(), refreshing: Boolean = false): RelayStatus =
        RelayStatus(
            initMarker = markerState("/dev/.dfi"),
            initSocket = socketState("@dfi_init"),
            btMarker = markerState("/dev/.dfib"),
            btSocket = socketState("@dfi_bt"),
            at = now,
            refreshing = refreshing,
        )

    fun markerState(path: String): MarkerState = try {
        Os.stat(path)
        MarkerState.PRESENT
    } catch (error: ErrnoException) {
        when (error.errno) {
            OsConstants.EACCES, OsConstants.EPERM -> MarkerState.PRESENT_DENIED
            OsConstants.ENOENT -> MarkerState.ABSENT
            else -> MarkerState.ERROR
        }
    } catch (_: Throwable) {
        MarkerState.UNKNOWN
    }

    fun socketState(name: String): SocketState = try {
        BufferedReader(FileReader("/proc/net/unix")).use { reader ->
            var line = reader.readLine()
            var found = false
            while (line != null) {
                if (line.contains(name)) {
                    found = true
                    break
                }
                line = reader.readLine()
            }
            if (found) SocketState.LISTENING else SocketState.ABSENT
        }
    } catch (_: Throwable) {
        SocketState.UNREADABLE
    }
}
