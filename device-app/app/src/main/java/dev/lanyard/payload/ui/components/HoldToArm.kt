package dev.lanyard.payload.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.lanyard.payload.ui.theme.LocalInstrument
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Press and hold to charge, lift to open the consent sheet.
 *
 * The fill is drawn through a draw scope that reads the animation value, so a
 * charge never recomposes the card. TalkBack gets a long-click action with the
 * same effect.
 */
@Composable
fun HoldToArm(
    label: String,
    hint: String,
    enabled: Boolean,
    accent: Color,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = label,
    holdMillis: Int = 1500,
) {
    val instrument = LocalInstrument.current
    val progress = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(10.dp)
    val scope = rememberCoroutineScope()

    val track = if (enabled) accent.copy(alpha = 0.08f) else Color.Transparent
    val edge = if (enabled) accent.copy(alpha = 0.45f) else instrument.hairline
    val labelColor = if (enabled) instrument.ink else instrument.inkFaint

    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(shape)
            .background(track)
            .border(1.dp, edge, shape)
            .drawBehind {
                val charged = progress.value
                if (charged <= 0.001f) return@drawBehind
                val width = size.width * charged
                drawRect(
                    color = accent.copy(alpha = 0.16f),
                    size = Size(width, size.height),
                )
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(0f, size.height - 3.dp.toPx()),
                    size = Size(width, 3.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx()),
                )
            }
            .semantics {
                onLongClick(label) {
                    if (enabled) onConfirm()
                    true
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        progress.snapTo(0f)
                        val charged = coroutineScope {
                            val charge = launch {
                                progress.animateTo(1f, tween(holdMillis, easing = LinearEasing))
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            val released = tryAwaitRelease()
                            charge.cancel()
                            val complete = released && progress.value >= 0.999f
                            if (!complete) progress.animateTo(0f, tween(140))
                            complete
                        }
                        progress.snapTo(0f)
                        if (charged) onConfirm()
                    },
                )
            }
            .testTag(tag)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                ),
                color = if (enabled) accent else instrument.inkFaint,
            )
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(1.dp))
        }
    }
}
