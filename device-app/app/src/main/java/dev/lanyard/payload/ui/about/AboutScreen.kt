package dev.lanyard.payload.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.lanyard.payload.BuildConfig
import dev.lanyard.payload.R
import dev.lanyard.payload.engine.PayloadDigest
import dev.lanyard.payload.ui.components.Eyebrow
import dev.lanyard.payload.ui.components.FieldBlock
import dev.lanyard.payload.ui.components.FieldRow
import dev.lanyard.payload.ui.components.Hairline
import dev.lanyard.payload.ui.components.InstrumentCard
import dev.lanyard.payload.ui.components.Mono
import dev.lanyard.payload.ui.components.ScreenColumn
import dev.lanyard.payload.ui.components.SectionHeading
import dev.lanyard.payload.ui.deviceFactsText
import dev.lanyard.payload.ui.runner.RunnerUiState
import dev.lanyard.payload.ui.theme.LocalInstrument
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    state: RunnerUiState,
    onNotify: (String) -> Unit,
    onLoadManifest: suspend () -> String?,
    onLoadLastRun: suspend () -> String?,
    onLoadDigests: suspend () -> List<PayloadDigest>,
    onProbe: () -> Unit,
    onCopyTrigger: () -> Unit,
    onCopyDeviceFacts: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val instrument = LocalInstrument.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var digests by remember { mutableStateOf<List<PayloadDigest>>(emptyList()) }
    var sheetTitle by remember { mutableStateOf<String?>(null) }
    var sheetBody by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.payload?.id) { digests = onLoadDigests() }

    fun openSheet(title: String, loader: suspend () -> String?) {
        sheetTitle = title
        sheetBody = null
        scope.launch {
            sheetBody = loader() ?: context.getString(R.string.about_unavailable)
        }
    }

    ScreenColumn(modifier) {
        Spacer(Modifier.height(18.dp))
        Eyebrow(stringResource(R.string.console_eyebrow))
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.about_title),
            style = MaterialTheme.typography.displaySmall,
            color = instrument.ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.about_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = instrument.inkMuted,
        )
        Spacer(Modifier.height(18.dp))

        InstrumentCard {
            SectionHeading(title = stringResource(R.string.about_build))
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldRow(
                    stringResource(R.string.about_version_label),
                    stringResource(
                        R.string.about_version,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                    ),
                )
                FieldRow(stringResource(R.string.about_package), BuildConfig.APPLICATION_ID)
            }
        }
        Spacer(Modifier.height(12.dp))

        InstrumentCard {
            SectionHeading(title = stringResource(R.string.about_provenance))
            Spacer(Modifier.height(10.dp))
            val payload = state.payload
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldRow(stringResource(R.string.consent_row_payload), payload?.let { it.id + " " + it.version } ?: "—")
                FieldRow(stringResource(R.string.consent_row_family), payload?.family ?: "—")
                FieldRow(
                    stringResource(R.string.consent_row_capabilities),
                    payload?.capabilities?.joinToString(", ") ?: "—",
                )
                FieldRow(
                    stringResource(R.string.consent_row_persistence),
                    payload?.persistence ?: "—",
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        InstrumentCard {
            SectionHeading(title = stringResource(R.string.about_target))
            Spacer(Modifier.height(10.dp))
            val device = state.device
            if (device == null) {
                Text(
                    text = stringResource(R.string.device_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = instrument.inkFaint,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldRow(stringResource(R.string.about_device_manufacturer), device.manufacturer)
                    FieldRow(stringResource(R.string.about_device_brand), device.brand)
                    FieldRow(stringResource(R.string.device_model), device.model)
                    FieldRow(stringResource(R.string.device_name), device.device)
                    FieldRow(stringResource(R.string.about_device_product), device.product)
                    FieldRow(stringResource(R.string.about_device_board), device.board)
                    FieldRow(
                        stringResource(R.string.about_device_soc),
                        device.soc + " / " + device.socManufacturer,
                    )
                    FieldRow(stringResource(R.string.about_device_bootloader), device.bootloader)
                    Spacer(Modifier.height(2.dp))
                    FieldBlock(stringResource(R.string.device_build), device.build)
                    FieldRow(stringResource(R.string.about_device_build_id), device.buildId)
                    FieldRow(stringResource(R.string.about_device_incremental), device.incremental)
                    FieldRow(
                        stringResource(R.string.about_device_security_patch),
                        device.securityPatch,
                    )
                    FieldRow(
                        stringResource(R.string.about_device_build_type),
                        device.buildType + " / " + device.buildTags,
                    )
                    Spacer(Modifier.height(2.dp))
                    FieldBlock(stringResource(R.string.device_kernel), device.kernel)
                    FieldRow(
                        stringResource(R.string.device_android),
                        stringResource(R.string.device_android_value, device.android, device.sdk),
                    )
                    FieldRow(stringResource(R.string.device_abi), device.abi)
                    FieldRow(stringResource(R.string.about_device_selinux), device.appDomain)
                }
                Spacer(Modifier.height(12.dp))
                Hairline()
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.about_device_capture),
                    style = MaterialTheme.typography.bodySmall,
                    color = instrument.inkFaint,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        InstrumentCard {
            SectionHeading(title = stringResource(R.string.about_digests))
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.about_digest_hint),
                style = MaterialTheme.typography.bodySmall,
                color = instrument.inkFaint,
            )
            Spacer(Modifier.height(10.dp))
            if (digests.isEmpty()) {
                Mono(stringResource(R.string.about_loading), color = instrument.inkMuted)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    digests.forEach { digest ->
                        Column {
                            Mono(digest.entry + " · " + digest.bytes + " B", color = instrument.inkMuted)
                            Mono(digest.sha256, color = instrument.ink)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        InstrumentCard {
            SectionHeading(title = stringResource(R.string.about_limits))
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.about_limits_body),
                style = MaterialTheme.typography.bodySmall,
                color = instrument.inkMuted,
            )
        }
        Spacer(Modifier.height(12.dp))

        InstrumentCard {
            SectionHeading(title = stringResource(R.string.about_utils))
            Spacer(Modifier.height(10.dp))
            val manifestLabel = stringResource(R.string.about_utils_manifest)
            val lastRunLabel = stringResource(R.string.about_utils_lastrun)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UtilityButton(manifestLabel) {
                    openSheet(manifestLabel, onLoadManifest)
                }
                UtilityButton(lastRunLabel) {
                    openSheet(lastRunLabel, onLoadLastRun)
                }
                UtilityButton(stringResource(R.string.about_utils_probe)) {
                    onProbe()
                    onNotify(context.getString(R.string.relay_probe))
                }
                UtilityButton(stringResource(R.string.about_utils_copy_facts)) {
                    state.device?.let { onCopyDeviceFacts(deviceFactsText(it)) }
                }
                UtilityButton(stringResource(R.string.about_utils_trigger)) {
                    onCopyTrigger()
                }
            }
            Spacer(Modifier.height(12.dp))
            Hairline()
            Spacer(Modifier.height(12.dp))
            Mono(
                text = stringResource(
                    R.string.about_utils_note,
                    stringResource(R.string.about_utils_command),
                ),
                color = instrument.inkFaint,
            )
        }
        Spacer(Modifier.height(12.dp))

        InstrumentCard {
            SectionHeading(title = stringResource(R.string.about_attribution))
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.about_attribution_body),
                style = MaterialTheme.typography.bodySmall,
                color = instrument.inkMuted,
            )
        }

        Spacer(Modifier.height(28.dp))
    }

    val title = sheetTitle
    if (title != null) {
        JsonSheet(title = title, body = sheetBody, onDismiss = { sheetTitle = null })
    }
}

@Composable
private fun UtilityButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JsonSheet(title: String, body: String?, onDismiss: () -> Unit) {
    val instrument = LocalInstrument.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(title)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.about_sheet_close))
                }
            }
            Spacer(Modifier.height(8.dp))
            Mono(
                text = body ?: stringResource(R.string.about_loading),
                color = instrument.ink,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = instrument.monospace,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                ),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
