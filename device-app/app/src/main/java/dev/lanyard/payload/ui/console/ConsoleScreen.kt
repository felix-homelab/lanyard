package dev.lanyard.payload.ui.console

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lanyard.payload.R
import dev.lanyard.payload.engine.MarkerState
import dev.lanyard.payload.engine.RelayStatus
import dev.lanyard.payload.engine.RunOutcome
import dev.lanyard.payload.engine.SocketState
import dev.lanyard.payload.engine.TargetMode
import dev.lanyard.payload.ui.components.Eyebrow
import dev.lanyard.payload.ui.components.FieldBlock
import dev.lanyard.payload.ui.components.HoldToArm
import dev.lanyard.payload.ui.components.InstrumentCard
import dev.lanyard.payload.ui.components.Led
import dev.lanyard.payload.ui.components.Mono
import dev.lanyard.payload.ui.components.ScreenColumn
import dev.lanyard.payload.ui.components.SectionHeading
import dev.lanyard.payload.ui.runner.DeviceUi
import dev.lanyard.payload.ui.runner.GateUi
import dev.lanyard.payload.ui.runner.RunUi
import dev.lanyard.payload.ui.runner.RunnerEvent
import dev.lanyard.payload.ui.runner.RunnerUiState
import dev.lanyard.payload.ui.theme.LocalInstrument

@Composable
fun ConsoleScreen(
    state: RunnerUiState,
    onEvent: (RunnerEvent) -> Unit,
    onOpenLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenColumn(modifier) {
        Spacer(Modifier.height(18.dp))
        Eyebrow(stringResource(R.string.console_eyebrow))
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.console_title),
            style = MaterialTheme.typography.displaySmall,
            color = LocalInstrument.current.ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.console_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = LocalInstrument.current.inkMuted,
        )
        Spacer(Modifier.height(18.dp))

        RiskCard()
        Spacer(Modifier.height(12.dp))

        GateHero(gate = state.gate)
        Spacer(Modifier.height(12.dp))

        PayloadCard(
            title = stringResource(R.string.payload_init_title),
            meta = stringResource(R.string.payload_init_meta),
            body = stringResource(R.string.payload_init_body),
            enabled = state.armed,
            running = state.run.running && state.run.mode == TargetMode.INIT,
            accent = LocalInstrument.current.danger,
            tag = "arm_init",
            onConfirm = { onEvent(RunnerEvent.Arm(TargetMode.INIT)) },
        )
        Spacer(Modifier.height(12.dp))

        PayloadCard(
            title = stringResource(R.string.payload_bt_title),
            meta = stringResource(R.string.payload_bt_meta),
            body = stringResource(R.string.payload_bt_body),
            enabled = state.armed,
            running = state.run.running && state.run.mode == TargetMode.BLUETOOTH,
            accent = LocalInstrument.current.warn,
            tag = "arm_bt",
            onConfirm = { onEvent(RunnerEvent.Arm(TargetMode.BLUETOOTH)) },
        )

        if (state.run.outcome != null || state.run.running) {
            Spacer(Modifier.height(12.dp))
            RunResultCard(
                run = state.run,
                onOpenLog = onOpenLog,
                onDismiss = { onEvent(RunnerEvent.AcknowledgeRun) },
            )
        }

        Spacer(Modifier.height(12.dp))
        RelayCard(relay = state.relay, onRefresh = { onEvent(RunnerEvent.RefreshRelay) })

        Spacer(Modifier.height(12.dp))
        DeviceCard(device = state.device)

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun RiskCard() {
    val instrument = LocalInstrument.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    InstrumentCard(emphasized = expanded) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Led(instrument.danger)
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.console_warning_title),
                style = MaterialTheme.typography.titleSmall,
                color = instrument.danger,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    text = stringResource(
                        if (expanded) R.string.warning_collapse else R.string.warning_expand,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = instrument.inkMuted,
                )
            }
        }
        Text(
            text = stringResource(R.string.console_warning_body),
            style = MaterialTheme.typography.bodySmall,
            color = instrument.inkMuted,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun GateHero(gate: GateUi) {
    val instrument = LocalInstrument.current
    val color = when (gate) {
        GateUi.Booting -> instrument.inkFaint
        is GateUi.Verified -> instrument.ok
        is GateUi.Compatible -> instrument.warn
        is GateUi.Unsupported -> instrument.danger
        is GateUi.Broken -> instrument.danger
    }
    val label: String
    val reason: String
    val hint: String?
    when (gate) {
        GateUi.Booting -> {
            label = stringResource(R.string.gate_booting)
            reason = stringResource(R.string.gate_boot_hint)
            hint = null
        }
        is GateUi.Verified -> {
            label = stringResource(R.string.gate_verified)
            reason = gate.reason
            hint = stringResource(R.string.gate_verified_hint)
        }
        is GateUi.Compatible -> {
            label = stringResource(R.string.gate_compatible)
            reason = gate.reason
            hint = stringResource(R.string.gate_blocked_hint)
        }
        is GateUi.Unsupported -> {
            label = stringResource(R.string.gate_unsupported)
            reason = gate.reason
            hint = stringResource(R.string.gate_blocked_hint)
        }
        is GateUi.Broken -> {
            label = stringResource(R.string.gate_broken)
            reason = gate.message
            hint = stringResource(R.string.gate_blocked_hint)
        }
    }

    InstrumentCard(emphasized = gate is GateUi.Verified) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 3.dp, height = 36.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow(stringResource(R.string.gate_label))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    modifier = Modifier.semantics { contentDescription = label },
                )
            }
            Led(color)
        }
        Spacer(Modifier.height(10.dp))
        Mono(text = reason, color = instrument.inkMuted)
        if (hint != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = instrument.inkFaint,
            )
        }
    }
}

@Composable
private fun PayloadCard(
    title: String,
    meta: String,
    body: String,
    enabled: Boolean,
    running: Boolean,
    accent: Color,
    tag: String,
    onConfirm: () -> Unit,
) {
    val instrument = LocalInstrument.current
    InstrumentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Led(accent, diameter = 7.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = instrument.ink,
                )
                Spacer(Modifier.height(4.dp))
                Mono(meta, color = instrument.inkMuted)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = instrument.inkMuted,
        )
        Spacer(Modifier.height(14.dp))
        HoldToArm(
            label = stringResource(if (running) R.string.arm_running else R.string.arm_label),
            hint = stringResource(R.string.arm_hint),
            enabled = enabled && !running,
            accent = accent,
            onConfirm = onConfirm,
            tag = tag,
        )
    }
}

@Composable
private fun RunResultCard(run: RunUi, onOpenLog: () -> Unit, onDismiss: () -> Unit) {
    val instrument = LocalInstrument.current
    val title: String
    val body: String
    val color: Color
    when {
        run.running -> {
            title = stringResource(R.string.run_running_title)
            body = stringResource(R.string.run_running_body)
            color = instrument.signal
        }
        run.outcome == RunOutcome.VERIFIED -> {
            title = stringResource(R.string.run_verified_title)
            body = stringResource(R.string.run_verified_body)
            color = instrument.ok
        }
        run.outcome == RunOutcome.FAILED -> {
            title = stringResource(R.string.run_failed_title)
            body = run.failure ?: stringResource(R.string.run_failed_body)
            color = instrument.danger
        }
        else -> {
            title = stringResource(R.string.run_not_verified_title)
            body = stringResource(R.string.run_not_verified_body)
            color = instrument.warn
        }
    }
    InstrumentCard(emphasized = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Led(color)
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodySmall, color = instrument.inkMuted)
        Row(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onOpenLog) {
                Text(stringResource(R.string.run_open_log), style = MaterialTheme.typography.labelMedium)
            }
            if (!run.running) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.run_dismiss), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun RelayCard(relay: RelayStatus, onRefresh: () -> Unit) {
    val instrument = LocalInstrument.current
    InstrumentCard {
        SectionHeading(
            title = stringResource(R.string.relay_label),
            trailing = {
                TextButton(onClick = onRefresh) {
                    Text(stringResource(R.string.relay_refresh), style = MaterialTheme.typography.labelMedium)
                }
            },
        )
        Spacer(Modifier.height(2.dp))
        RelayRow(
            label = stringResource(R.string.relay_init_marker),
            value = markerLabel(relay.initMarker),
            color = markerColor(relay.initMarker),
        )
        RelayRow(
            label = stringResource(R.string.relay_init_socket),
            value = socketLabel(relay.initSocket),
            color = socketColor(relay.initSocket),
        )
        Spacer(Modifier.height(6.dp))
        RelayRow(
            label = stringResource(R.string.relay_bt_marker),
            value = markerLabel(relay.btMarker),
            color = markerColor(relay.btMarker),
        )
        RelayRow(
            label = stringResource(R.string.relay_bt_socket),
            value = socketLabel(relay.btSocket),
            color = socketColor(relay.btSocket),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.relay_note),
            style = MaterialTheme.typography.bodySmall,
            color = instrument.inkFaint,
        )
    }
}

@Composable
private fun RelayRow(label: String, value: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Led(color, diameter = 6.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalInstrument.current.inkMuted,
            modifier = Modifier.weight(1f),
        )
        Mono(value, color = color)
    }
}

@Composable
private fun DeviceCard(device: DeviceUi?) {
    val instrument = LocalInstrument.current
    InstrumentCard {
        SectionHeading(title = stringResource(R.string.device_label))
        Spacer(Modifier.height(12.dp))
        if (device == null) {
            Text(
                text = stringResource(R.string.device_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = instrument.inkFaint,
            )
            return@InstrumentCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldBlock(stringResource(R.string.device_model), device.model, Modifier.weight(1f))
                FieldBlock(stringResource(R.string.device_name), device.device, Modifier.weight(1f))
            }
            FieldBlock(stringResource(R.string.device_kernel), device.kernel)
            FieldBlock(stringResource(R.string.device_build), device.build)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FieldBlock(
                    label = stringResource(R.string.device_android),
                    value = stringResource(R.string.device_android_value, device.android, device.sdk),
                    modifier = Modifier.weight(1f),
                )
                FieldBlock(stringResource(R.string.device_abi), device.abi, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun markerLabel(state: MarkerState): String = stringResource(
    when (state) {
        MarkerState.PRESENT -> R.string.marker_present
        MarkerState.PRESENT_DENIED -> R.string.marker_present_denied
        MarkerState.ABSENT -> R.string.marker_absent
        MarkerState.ERROR -> R.string.marker_error
        MarkerState.UNKNOWN -> R.string.marker_unknown
    },
)

@Composable
private fun socketLabel(state: SocketState): String = stringResource(
    when (state) {
        SocketState.LISTENING -> R.string.socket_listening
        SocketState.ABSENT -> R.string.socket_absent
        SocketState.UNREADABLE -> R.string.socket_unreadable
    },
)

@Composable
private fun markerColor(state: MarkerState): Color {
    val instrument = LocalInstrument.current
    return when (state) {
        MarkerState.PRESENT -> instrument.ok
        MarkerState.PRESENT_DENIED -> instrument.warn
        MarkerState.ABSENT -> instrument.inkFaint
        MarkerState.ERROR -> instrument.danger
        MarkerState.UNKNOWN -> instrument.inkFaint
    }
}

@Composable
private fun socketColor(state: SocketState): Color {
    val instrument = LocalInstrument.current
    return when (state) {
        SocketState.LISTENING -> instrument.ok
        SocketState.ABSENT -> instrument.inkFaint
        SocketState.UNREADABLE -> instrument.warn
    }
}
