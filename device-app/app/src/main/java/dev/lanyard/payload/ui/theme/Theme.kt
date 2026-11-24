package dev.lanyard.payload.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Extra roles Material 3 has no slot for: the hairline rules, the LED colours,
 * and the muted ink the instrument labels use.
 */
@Immutable
data class InstrumentColors(
    val hairline: Color,
    val hairlineStrong: Color,
    val signal: Color,
    val onSignal: Color,
    val ok: Color,
    val warn: Color,
    val danger: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val raised: Color,
    val sunken: Color,
    val buttonInk: Color,
    val monospace: FontFamily,
)

private val DarkInstrument = InstrumentColors(
    hairline = Color(0xFF232E3B),
    hairlineStrong = Color(0xFF364454),
    signal = Color(0xFFF5A742),
    onSignal = Color(0xFF1B1206),
    ok = Color(0xFF46D08A),
    warn = Color(0xFFE9C46A),
    danger = Color(0xFFFF6B5E),
    ink = Color(0xFFE8EEF6),
    inkMuted = Color(0xFF93A2B4),
    inkFaint = Color(0xFF76858F),
    raised = Color(0xFF111821),
    sunken = Color(0xFF070A0E),
    buttonInk = Color(0xFF170B06),
    monospace = FontFamily.Monospace,
)

private val LightInstrument = InstrumentColors(
    hairline = Color(0xFFDCE3EA),
    hairlineStrong = Color(0xFFC2CDD8),
    signal = Color(0xFFA85F00),
    onSignal = Color(0xFFFFFFFF),
    ok = Color(0xFF11794A),
    warn = Color(0xFF8A5A00),
    danger = Color(0xFFB3261E),
    ink = Color(0xFF0B1117),
    inkMuted = Color(0xFF57646F),
    inkFaint = Color(0xFF5E6B76),
    raised = Color(0xFFF7F9FB),
    sunken = Color(0xFFE9EEF3),
    buttonInk = Color(0xFF170B06),
    monospace = FontFamily.Monospace,
)

val LocalInstrument = staticCompositionLocalOf { DarkInstrument }

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFF5A742),
    onPrimary = Color(0xFF1B1206),
    primaryContainer = Color(0xFF2B1E0B),
    onPrimaryContainer = Color(0xFFFFDCA8),
    secondary = Color(0xFF93A2B4),
    onSecondary = Color(0xFF0A0E13),
    background = Color(0xFF0A0E13),
    onBackground = Color(0xFFE8EEF6),
    surface = Color(0xFF0E141B),
    onSurface = Color(0xFFE8EEF6),
    surfaceVariant = Color(0xFF151D27),
    onSurfaceVariant = Color(0xFF93A2B4),
    surfaceContainerLowest = Color(0xFF070A0E),
    surfaceContainerLow = Color(0xFF0C1218),
    surfaceContainer = Color(0xFF111821),
    surfaceContainerHigh = Color(0xFF161F2A),
    surfaceContainerHighest = Color(0xFF1B2531),
    outline = Color(0xFF263140),
    outlineVariant = Color(0xFF1A222D),
    error = Color(0xFFFF6B5E),
    onError = Color(0xFF200806),
    errorContainer = Color(0xFF2A1310),
    onErrorContainer = Color(0xFFFFDAD5),
    inverseSurface = Color(0xFFE8EEF6),
    inverseOnSurface = Color(0xFF10161D),
    scrim = Color(0xFF000000),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFA85F00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE7C4),
    onPrimaryContainer = Color(0xFF3A2400),
    secondary = Color(0xFF57646F),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF2F5F8),
    onBackground = Color(0xFF0B1117),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B1117),
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = Color(0xFF57646F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FB),
    surfaceContainer = Color(0xFFF1F4F8),
    surfaceContainerHigh = Color(0xFFEBEFF4),
    surfaceContainerHighest = Color(0xFFE5EAF0),
    outline = Color(0xFFD7DEE6),
    outlineVariant = Color(0xFFE7ECF1),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCE9E7),
    onErrorContainer = Color(0xFF410E0B),
    inverseSurface = Color(0xFF0B1117),
    inverseOnSurface = Color(0xFFF2F5F8),
    scrim = Color(0xFF000000),
)

private val LanyardShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp),
)

private val Sans = FontFamily.SansSerif

private val LanyardTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp,
    ),
)

@Composable
fun LanyardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val instrument = if (darkTheme) DarkInstrument else LightInstrument
    androidx.compose.runtime.CompositionLocalProvider(LocalInstrument provides instrument) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = LanyardTypography,
            shapes = LanyardShapes,
            content = content,
        )
    }
}
