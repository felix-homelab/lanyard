package dev.lanyard.payload.engine

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes the same files/last_run.json shape the lab workflow already greps
 * with: adb shell run-as dev.lanyard.payload cat files/last_run.json
 */
class LastRunStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, "last_run.json")

    fun write(state: RunState, snapshot: DeviceSnapshot.Ready) {
        try {
            val status = JSONObject()
            status.put("app", "dev.lanyard.payload")
            status.put("payload", snapshot.manifest.id)
            status.put("entry", state.mode?.entry ?: "unknown")
            status.put("target_mode", state.mode?.wireName ?: "unknown")
            status.put("verified_write", state.outcome == RunOutcome.VERIFIED)
            state.failure?.let { status.put("error", it) }
            status.put(
                "finished_at",
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                    .format(Date(state.finishedAt ?: System.currentTimeMillis())),
            )
            status.put("model", snapshot.facts.model)
            status.put("kernel", snapshot.facts.kernel)
            status.put("marker_init", RelayProbe.markerState("/dev/.dfi").name)
            status.put("marker_bt", RelayProbe.markerState("/dev/.dfib").name)
            status.put("log_tail", state.tail(8_000))
            FileOutputStream(file).use { output ->
                output.write(status.toString(2).toByteArray(StandardCharsets.UTF_8))
            }
        } catch (error: Exception) {
            Log.w(TAG, "status write failed", error)
        }
    }

    fun read(): String? = try {
        if (file.exists()) file.readText() else null
    } catch (error: Exception) {
        Log.w(TAG, "status read failed", error)
        null
    }

    val path: String get() = file.absolutePath

    private companion object {
        const val TAG = "LanyardPayload"
    }
}
