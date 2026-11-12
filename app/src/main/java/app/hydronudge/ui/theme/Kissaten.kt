package app.hydronudge.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import app.hydronudge.R

val KissatenBody = FontFamily(Font(R.font.zen_maru_regular), Font(R.font.zen_maru_bold, FontWeight.Bold))
val KissatenEditorial = FontFamily(Font(R.font.shippori_regular))

/** Choose the higher contrast of warm ink and white using WCAG relative luminance. */
fun kissatenInk(color: Color): Color = if (color.luminance() > 0.179f) Color.Black else Color.White

fun kissatenTypography(t: Typography): Typography = t.copy(
    displayLarge=t.displayLarge.copy(fontFamily=KissatenBody), displayMedium=t.displayMedium.copy(fontFamily=KissatenBody),
    displaySmall=t.displaySmall.copy(fontFamily=KissatenBody), headlineLarge=t.headlineLarge.copy(fontFamily=KissatenBody),
    headlineMedium=t.headlineMedium.copy(fontFamily=KissatenBody), headlineSmall=t.headlineSmall.copy(fontFamily=KissatenBody),
    titleLarge=t.titleLarge.copy(fontFamily=KissatenBody), titleMedium=t.titleMedium.copy(fontFamily=KissatenBody), titleSmall=t.titleSmall.copy(fontFamily=KissatenBody),
    bodyLarge=t.bodyLarge.copy(fontFamily=KissatenBody), bodyMedium=t.bodyMedium.copy(fontFamily=KissatenBody), bodySmall=t.bodySmall.copy(fontFamily=KissatenBody),
    labelLarge=t.labelLarge.copy(fontFamily=KissatenBody), labelMedium=t.labelMedium.copy(fontFamily=KissatenBody), labelSmall=t.labelSmall.copy(fontFamily=KissatenBody)
)

fun parseKissatenAccent(value: String): Color? = runCatching {
    if (Regex("#[0-9a-fA-F]{6}").matches(value)) Color(android.graphics.Color.parseColor(value)) else null
}.getOrNull()

fun kissatenColors(s: ColorScheme, oled: Boolean = false): ColorScheme {
    val background = if (oled) Color.Black else s.surface
    val primary = kissatenTone(s.primary, background)
    val secondary = kissatenTone(s.secondary, background)
    val tertiary = kissatenTone(s.tertiary, background)
    return s.copy(
    primary = primary, secondary = secondary, tertiary = tertiary,
    onPrimary = kissatenInk(primary), onSecondary = kissatenInk(secondary), onTertiary = kissatenInk(tertiary),
    primaryContainer = lerp(s.surface, s.primary, 0.14f), onPrimaryContainer = s.onSurface,
    secondaryContainer = lerp(s.surface, s.secondary, 0.12f), onSecondaryContainer = s.onSurface,
    tertiaryContainer = lerp(s.surface, s.tertiary, 0.12f), onTertiaryContainer = s.onSurface,
    background = if (oled) Color.Black else s.background,
    surface = if (oled) Color.Black else s.surface,
    surfaceContainerLowest = if (oled) Color.Black else s.background,
    surfaceContainerLow = if (oled) Color.Black else s.surface,
    surfaceContainer = lerp(if (oled) Color.Black else s.surface, s.primary, 0.035f),
    surfaceContainerHigh = lerp(if (oled) Color.Black else s.surface, s.primary, 0.065f),
    surfaceContainerHighest = lerp(if (oled) Color.Black else s.surface, s.primary, 0.09f),
    outlineVariant = lerp(s.surface, s.onSurface, 0.16f)
)
}

fun kissatenContrast(a: Color, b: Color): Float {
    val x = a.luminance(); val y = b.luminance()
    return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
}

/** Keep the chosen hue, adjusting tone when used as small text on the surface. */
fun kissatenTone(accent: Color, background: Color): Color {
    if (kissatenContrast(accent, background) >= 4.5f) return accent
    val target = kissatenInk(background)
    for (step in 1..20) {
        val candidate = lerp(accent, target, step / 20f)
        if (kissatenContrast(candidate, background) >= 4.5f) return candidate
    }
    return target
}

/** Original path artwork. No downloaded images, network calls or animation required. */
@Composable
fun CafeIllustration(modifier: Modifier = Modifier, product: Int = 0, quiet: Boolean = false) {
    val c = MaterialTheme.colorScheme
    Canvas(modifier) {
        scale(size.width / 320f, size.height / 180f, pivot = Offset.Zero) {
            val ink = c.primary.copy(alpha = if (quiet) 0.09f else 0.65f)
            val fill = c.primary.copy(alpha = if (quiet) 0.035f else 0.10f)
            drawRoundRect(fill, Offset(104f, 12f), Size(168f, 132f), CornerRadius(48f,48f))
            drawRoundRect(ink, Offset(108f, 16f), Size(160f, 128f), CornerRadius(44f,44f), style = Stroke(2f))
            drawLine(ink, Offset(188f, 18f), Offset(188f, 142f), 2f)
            drawLine(ink, Offset(110f, 84f), Offset(266f, 84f), 2f)
            drawLine(ink, Offset(58f, 150f), Offset(290f, 150f), 2f)
            drawOval(c.tertiary.copy(alpha=if(quiet) 0.05f else 0.20f), Offset(226f, 34f), Size(24f,24f))
            drawLine(ink, Offset(72f, 10f), Offset(72f, 44f), 1.5f)
            drawRoundRect(c.tertiary.copy(alpha=if(quiet) 0.04f else 0.24f), Offset(57f,44f), Size(30f,40f), CornerRadius(14f,14f))
            drawLine(ink, Offset(64f,48f), Offset(80f,48f), 2f)
            when (product) {
                0 -> {
                    val glass = Path().apply { moveTo(142f,99f); lineTo(148f,145f); quadraticTo(168f,151f,188f,145f); lineTo(194f,99f); close() }
                    drawPath(glass,c.secondary.copy(alpha=if(quiet) 0.04f else 0.23f))
                    drawPath(glass,ink,style=Stroke(2.5f))
                    drawLine(ink,Offset(146f,116f),Offset(190f,116f),2f)
                    val drop = Path().apply { moveTo(166f,70f); cubicTo(146f,93f,158f,103f,166f,103f); cubicTo(178f,103f,184f,90f,166f,70f); close() }
                    drawPath(drop,c.primary.copy(alpha=if(quiet) 0.04f else 0.55f))
                }
                1 -> {
                    drawRoundRect(c.secondary.copy(alpha=if(quiet) 0.03f else 0.18f),Offset(126f,104f),Size(98f,42f),CornerRadius(5f,5f))
                    drawRoundRect(ink,Offset(126f,104f),Size(98f,42f),CornerRadius(5f,5f),style=Stroke(2f))
                    drawLine(ink,Offset(174f,106f),Offset(174f,144f),2f)
                    val ribbon=Path().apply { moveTo(195f,103f); lineTo(206f,103f); lineTo(206f,135f); lineTo(200f,130f); lineTo(195f,135f); close() }
                    drawPath(ribbon,c.primary.copy(alpha=if(quiet) 0.05f else 0.55f))
                }
                else -> {
                    drawCircle(c.tertiary.copy(alpha=if(quiet) 0.04f else 0.30f),28f,Offset(179f,111f))
                    drawOval(ink,Offset(130f,100f),Size(100f,26f),style=Stroke(2.5f))
                    val star=Path().apply { moveTo(179f,91f); lineTo(184f,105f); lineTo(198f,110f); lineTo(184f,115f); lineTo(179f,129f); lineTo(174f,115f); lineTo(160f,110f); lineTo(174f,105f); close() }
                    drawPath(star,c.primary.copy(alpha=if(quiet) 0.04f else 0.7f))
                }
            }
            for ((x,y) in listOf(Offset(34f,114f),Offset(286f,61f),Offset(244f,160f))) {
                drawOval(c.primary.copy(alpha=if(quiet) 0.03f else 0.22f),Offset(x,y),Size(10f,5f))
            }
        }
    }
}

@Composable
fun KissatenWelcome(message: String, product: Int) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(message, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            CafeIllustration(Modifier.fillMaxWidth().height(108.dp), product)
            Text(if (product == 0) "A sip. A pause. A little kindness." else "One small step, at your own pace.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
