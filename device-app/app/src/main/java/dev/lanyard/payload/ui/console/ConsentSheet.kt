package dev.lanyard.payload.ui.console

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lanyard.payload.R
import dev.lanyard.payload.engine.TargetMode
import dev.lanyard.payload.ui.components.Eyebrow
import dev.lanyard.payload.ui.components.FieldRow
import dev.lanyard.payload.ui.components.Hairline
import dev.lanyard.payload.ui.components.InstrumentCard
import dev.lanyard.payload.ui.components.Led
import dev.lanyard.payload.ui.components.Mono
import dev.lanyard.payload.ui.runner.DeviceUi
import dev.lanyard.payload.ui.runner.PayloadMetaUi
import dev.lanyard.payload.ui.theme.LocalInstrument

/**
 * The second deliberate gesture: the operator reads what will run, against
 * which target, and what the phone may do, before the payload is staged.
 *
 * The disclosures scroll; the two decisions stay pinned above the navigation
 * bar so Run is never reachable without the sheet having been opened.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentSheet(
    target: TargetMode,
    payload: PayloadMetaUi?,
    device: DeviceUi?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val instrument = LocalInstrument.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val payloadTitle = stringResource(
        if (target == TargetMode.INIT) R.string.payload_init_title else R.string.payload_bt_title,
    )
    val title = stringResource(
        R.string.consent_title,
        payloadTitle.replaceFirstChar { it.lowercaseChar() },
    )
    val accent = if (target == TargetMode.INIT) instrument.danger else instrument.warn

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = instrument.ink,
        dragHandle = {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                ModalDragHandle()
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Led(accent, diameter = 8.dp)
                    Spacer(Modifier.width(4.dp))
                    Eyebrow(stringResource(R.string.consent_eyebrow), color = accent)
                }
                Spacer(Modifier.height(8.dp))
                Text(text = title, style = MaterialTheme.typography.headlineSmall, color = instrument.ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.consent_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = instrument.inkMuted,
                )
                Spacer(Modifier.height(16.dp))

                InstrumentCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldRow(
                            label = stringResource(R.string.consent_row_payload),
                            value = payload?.let { it.id + " " + it.version } ?: "—",
                        )
                        FieldRow(stringResource(R.string.consent_row_entry), target.entry)
                        FieldRow(stringResource(R.string.consent_row_family), payload?.family ?: "—")
                        FieldRow(
                            stringResource(R.string.consent_row_capabilities),
                            payload?.capabilities?.joinToString(", ") ?: "—",
                        )
                        FieldRow(
                            stringResource(R.string.consent_row_persistence),
                            payload?.let {
                                stringResource(R.string.consent_persistence_value, it.persistence)
                            } ?: "—",
                        )
                        FieldRow(
                            stringResource(R.string.consent_row_target),
                            device?.let { it.model + " / " + it.kernel } ?: "—",
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                InstrumentCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Led(instrument.danger, diameter = 8.dp)
                        Text(
                            text = stringResource(R.string.consent_risk_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = instrument.danger,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.consent_risk_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = instrument.inkMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Hairline()
                    Spacer(Modifier.height(12.dp))
                    Mono(
                        text = stringResource(
                            if (target == TargetMode.INIT) R.string.consent_init_extra else R.string.consent_bt_extra,
                        ),
                        color = instrument.inkMuted,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Hairline()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.consent_cancel))
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = instrument.buttonInk,
                    ),
                ) {
                    Text(stringResource(R.string.consent_confirm))
                }
            }
        }
    }
}

@Composable
private fun ModalDragHandle() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .padding(vertical = 4.dp)
                .height(4.dp)
                .fillMaxWidth(0.12f)
                .background(
                    LocalInstrument.current.hairlineStrong,
                    MaterialTheme.shapes.extraSmall,
                ),
        )
    }
}
