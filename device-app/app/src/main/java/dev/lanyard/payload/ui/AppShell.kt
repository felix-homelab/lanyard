package dev.lanyard.payload.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lanyard.payload.R
import dev.lanyard.payload.ui.about.AboutScreen
import dev.lanyard.payload.ui.components.Hairline
import dev.lanyard.payload.ui.console.ConsentSheet
import dev.lanyard.payload.ui.console.ConsoleScreen
import dev.lanyard.payload.ui.log.LogScreen
import dev.lanyard.payload.ui.runner.RunnerEffect
import dev.lanyard.payload.ui.runner.RunnerEvent
import dev.lanyard.payload.ui.runner.RunnerViewModel
import dev.lanyard.payload.ui.theme.LocalInstrument
import kotlinx.coroutines.launch

enum class Destination(@StringRes val labelRes: Int) {
    CONSOLE(R.string.nav_console),
    LOG(R.string.nav_log),
    ABOUT(R.string.nav_about),
}

@Composable
fun AppShell(
    viewModel: RunnerViewModel,
    onCopyTrigger: () -> Unit,
    onCopyDeviceFacts: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var destination by rememberSaveable { mutableStateOf(Destination.CONSOLE) }

    val notify: (String) -> Unit = { message ->
        scope.launch { snackbar.showSnackbar(message) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RunnerEffect.ShowMessage -> snackbar.showSnackbar(context.getString(effect.messageRes))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            Column {
                Hairline()
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    Destination.entries.forEach { item ->
                        val selected = destination == item
                        NavigationBarItem(
                            selected = selected,
                            onClick = { destination = item },
                            icon = {
                                NavGlyph(
                                    destination = item,
                                    tint = if (selected) {
                                        LocalInstrument.current.signal
                                    } else {
                                        LocalInstrument.current.inkFaint
                                    },
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = LocalInstrument.current.signal,
                                selectedTextColor = LocalInstrument.current.ink,
                                unselectedIconColor = LocalInstrument.current.inkFaint,
                                unselectedTextColor = LocalInstrument.current.inkFaint,
                                indicatorColor = LocalInstrument.current.signal.copy(alpha = 0.12f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = destination,
            modifier = Modifier.padding(padding),
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "destination",
        ) { target ->
            when (target) {
                Destination.CONSOLE -> ConsoleScreen(
                    state = state,
                    onEvent = viewModel::onEvent,
                    onOpenLog = { destination = Destination.LOG },
                )
                Destination.LOG -> LogScreen(
                    state = state,
                    onClear = { viewModel.onEvent(RunnerEvent.ClearLog) },
                    onNotify = notify,
                )
                Destination.ABOUT -> AboutScreen(
                    state = state,
                    onNotify = notify,
                    onLoadManifest = viewModel::manifestJson,
                    onLoadLastRun = viewModel::lastRunJson,
                    onLoadDigests = viewModel::payloadDigests,
                    onProbe = { viewModel.onEvent(RunnerEvent.ProbeNative) },
                    onCopyTrigger = onCopyTrigger,
                    onCopyDeviceFacts = onCopyDeviceFacts,
                )
            }
        }
    }

    val consent = state.consent
    if (consent != null) {
        ConsentSheet(
            target = consent,
            payload = state.payload,
            device = state.device,
            onDismiss = { viewModel.onEvent(RunnerEvent.DismissConsent) },
            onConfirm = { viewModel.onEvent(RunnerEvent.ConfirmRun) },
        )
    }
}

@Composable
private fun NavGlyph(destination: Destination, tint: Color) {
    Canvas(Modifier.size(22.dp)) {
        val width = 1.7.dp.toPx()
        val stroke = Stroke(width = width, cap = StrokeCap.Round)
        when (destination) {
            Destination.CONSOLE -> {
                val radius = size.minDimension / 2f - width
                drawCircle(color = tint, radius = radius, style = stroke)
                drawLine(
                    color = tint,
                    start = center,
                    end = Offset(center.x + radius * 0.62f, center.y - radius * 0.62f),
                    strokeWidth = width,
                    cap = StrokeCap.Round,
                )
                drawCircle(color = tint, radius = width * 0.9f, center = center)
            }
            Destination.LOG -> {
                listOf(0.26f, 0.5f, 0.74f).forEachIndexed { index, fraction ->
                    val y = size.height * fraction
                    val endX = size.width * if (index == 2) 0.68f else 0.86f
                    drawLine(
                        color = tint,
                        start = Offset(size.width * 0.14f, y),
                        end = Offset(endX, y),
                        strokeWidth = width,
                        cap = StrokeCap.Round,
                    )
                }
            }
            Destination.ABOUT -> {
                val radius = size.minDimension / 2f - width
                drawCircle(color = tint, radius = radius, style = stroke)
                drawLine(
                    color = tint,
                    start = Offset(center.x, center.y - radius * 0.12f),
                    end = Offset(center.x, center.y + radius * 0.46f),
                    strokeWidth = width,
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = tint,
                    radius = width * 0.85f,
                    center = Offset(center.x, center.y - radius * 0.44f),
                )
            }
        }
    }
}
