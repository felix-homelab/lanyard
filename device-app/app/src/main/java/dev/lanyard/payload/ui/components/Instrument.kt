package dev.lanyard.payload.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lanyard.payload.ui.theme.LocalInstrument

@Composable
fun InstrumentCard(
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    content: @Composable () -> Unit,
) {
    val instrument = LocalInstrument.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            if (emphasized) 1.dp else 1.dp,
            if (emphasized) instrument.hairlineStrong else instrument.hairline,
        ),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalInstrument.current.inkFaint,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun Mono(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = LocalInstrument.current.ink,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontFamily = LocalInstrument.current.monospace),
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign ?: TextAlign.Start,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalInstrument.current.hairline),
    )
}

@Composable
fun Led(
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 9.dp,
) {
    Box(modifier.size(diameter + 8.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(diameter + 7.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
        )
        Box(Modifier.size(diameter).clip(CircleShape).background(color))
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
fun FieldBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalInstrument.current.ink,
) {
    Column(modifier.fillMaxWidth()) {
        Eyebrow(label)
        Spacer(Modifier.height(3.dp))
        Mono(value, color = valueColor)
    }
}

@Composable
fun FieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LocalInstrument.current.ink,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalInstrument.current.inkMuted,
            maxLines = 1,
            modifier = Modifier.padding(top = 1.dp),
        )
        Mono(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Eyebrow(title, color = LocalInstrument.current.inkMuted)
        trailing?.invoke()
    }
}
