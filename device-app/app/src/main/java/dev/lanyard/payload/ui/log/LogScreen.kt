package dev.lanyard.payload.ui.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lanyard.payload.R
import dev.lanyard.payload.engine.LogEntry
import dev.lanyard.payload.engine.LogLevel
import dev.lanyard.payload.ui.components.Eyebrow
import dev.lanyard.payload.ui.components.Hairline
import dev.lanyard.payload.ui.components.InstrumentCard
import dev.lanyard.payload.ui.components.Led
import dev.lanyard.payload.ui.components.Mono
import dev.lanyard.payload.ui.formatClock
import dev.lanyard.payload.ui.levelLabel
import dev.lanyard.payload.ui.runner.RunnerUiState
import dev.lanyard.payload.ui.theme.LocalInstrument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen(
    state: RunnerUiState,
    onClear: () -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val instrument = LocalInstrument.current
    val context = LocalContext.current
    val entries = state.run.entries
    var follow by rememberSaveable { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size, follow) {
        if (!follow || entries.isEmpty()) return@LaunchedEffect
        listState.animateScrollToItem(entries.lastIndex)
    }

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            Eyebrow(stringResource(R.string.log_title))
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.log_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = instrument.inkMuted,
            )
            Spacer(Modifier.height(14.dp))
            LogStatusStrip(state)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val actionColors = ButtonDefaults.textButtonColors(
                    contentColor = instrument.inkMuted,
                    disabledContentColor = instrument.inkFaint,
                )
                TextButton(
                    onClick = { copyLog(context, entries, onNotify) },
                    enabled = entries.isNotEmpty(),
                    colors = actionColors,
                ) {
                    Text(stringResource(R.string.log_copy), style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = { saveLog(context, entries, onNotify) },
                    enabled = entries.isNotEmpty(),
                    colors = actionColors,
                ) {
                    Text(stringResource(R.string.log_save), style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = {
                        onClear()
                        if (state.run.running) onNotify(context.getString(R.string.log_clear_refused))
                    },
                    enabled = entries.isNotEmpty(),
                    colors = actionColors,
                ) {
                    Text(stringResource(R.string.log_clear), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.log_follow),
                    style = MaterialTheme.typography.labelMedium,
                    color = instrument.inkFaint,
                )
                Spacer(Modifier.width(6.dp))
                Switch(
                    checked = follow,
                    onCheckedChange = { follow = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = instrument.onSignal,
                        checkedTrackColor = instrument.signal,
                        uncheckedThumbColor = instrument.inkMuted,
                        uncheckedTrackColor = instrument.hairline,
                        uncheckedBorderColor = instrument.hairline,
                    ),
                )
            }
            if (state.run.dropped > 0) {
                Text(
                    text = pluralStringResource(R.plurals.log_dropped, state.run.dropped, state.run.dropped),
                    style = MaterialTheme.typography.bodySmall,
                    color = instrument.warn,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Hairline()
        }

        if (entries.isEmpty()) {
            EmptyLog()
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 32.dp,
                ),
            ) {
                items(entries, key = { it.id }, contentType = { it.level }) { entry ->
                    LogRow(entry)
                }
            }
        }
    }
}

@Composable
private fun LogStatusStrip(state: RunnerUiState) {
    val instrument = LocalInstrument.current
    val running = state.run.running
    val color = when {
        running -> instrument.signal
        state.run.outcome == dev.lanyard.payload.engine.RunOutcome.VERIFIED -> instrument.ok
        state.run.outcome == dev.lanyard.payload.engine.RunOutcome.FAILED -> instrument.danger
        state.run.outcome != null -> instrument.warn
        else -> instrument.inkFaint
    }
    val label = when {
        running -> stringResource(R.string.log_running, state.run.mode?.wireName ?: "")
        state.run.finishedAt != null -> stringResource(
            R.string.log_finished,
            state.run.mode?.wireName ?: "",
        )
        else -> stringResource(R.string.log_idle)
    }
    InstrumentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Led(color)
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = instrument.ink,
                modifier = Modifier.weight(1f),
            )
            state.run.finishedAt?.let {
                Mono(dev.lanyard.payload.ui.formatStamp(it), color = instrument.inkMuted)
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val instrument = LocalInstrument.current
    val color = when (entry.level) {
        LogLevel.INFO -> instrument.inkMuted
        LogLevel.OK -> instrument.ok
        LogLevel.WARN -> instrument.warn
        LogLevel.FAIL -> instrument.danger
    }
    val level = levelLabel(entry.level)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 3.dp)
                .size(width = 2.dp, height = 15.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color)
                .semantics { contentDescription = level },
        )
        Spacer(Modifier.width(10.dp))
        Mono(
            text = formatClock(entry.at),
            color = instrument.inkFaint,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            modifier = Modifier.width(58.dp),
        )
        Mono(
            text = entry.message,
            color = if (entry.level == LogLevel.INFO) instrument.ink else color,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EmptyLog() {
    val instrument = LocalInstrument.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BackgroundDot()
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.log_empty),
            style = MaterialTheme.typography.titleSmall,
            color = instrument.inkMuted,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.log_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = instrument.inkFaint,
        )
    }
}

@Composable
private fun BackgroundDot() {
    Box(
        Modifier
            .size(10.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(LocalInstrument.current.hairlineStrong),
    )
}

private fun copyLog(context: Context, entries: List<LogEntry>, notify: (String) -> Unit) {
    if (entries.isEmpty()) return
    val text = entries.joinToString("\n") { formatClock(it.at) + "  " + it.message }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Lanyard run log", text))
    notify(context.getString(R.string.log_copied))
}

private fun saveLog(context: Context, entries: List<LogEntry>, notify: (String) -> Unit) {
    if (entries.isEmpty()) return
    val directory = File(context.filesDir, "logs").apply { mkdirs() }
    val name = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".log"
    val target = File(directory, name)
    val text = entries.joinToString("\n") {
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(it.at)) + "  " + it.level.name + "  " + it.message
    }
    runCatching {
        target.writeText(text)
        notify(context.getString(R.string.log_saved, target.absolutePath))
    }.onFailure { notify(it.message ?: "save failed") }
}

