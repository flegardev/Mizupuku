package app.hydronudge.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.hydronudge.data.preferences.HydroSettings
import app.hydronudge.data.preferences.OceanPalette
import app.hydronudge.data.preferences.ThemeMode

data class AccentSwatch(val name: String, val hex: Long)

val CustomAccentSwatches = listOf(
    AccentSwatch("Theme Default", 0L),
    AccentSwatch("Moonlight Aqua", 0xFF6BA3D6),
    AccentSwatch("Sakura Blush", 0xFFE598A8),
    AccentSwatch("Matcha Mist", 0xFF72B088),
    AccentSwatch("Kyoto Sky", 0xFF7AAAD6),
    AccentSwatch("Lantern Amber", 0xFFE89E5A),
    AccentSwatch("Plum Ink", 0xFF9E7FB8),
    AccentSwatch("Bamboo Teal", 0xFF5E9F87)
)

/** Lofi Japanese Bathhouse & Rainy-Window brand tokens. UI consumes semantic Material colors. */
object OceanTokens {
    val Background = Color(0xFF0A1420)
    val Deep = Color(0xFF060D16)
    val Surface = Color(0xFF132233)
    val Elevated = Color(0xFF1B2E42)
    val Interactive = Color(0xFF213850)
    val Aqua = Color(0xFF82D6E3)
    val Blue = Color(0xFF6BA3D6)
    val Turquoise = Color(0xFF8BE0CD)
    val Text = Color(0xFFF3F8FC)
    val Secondary = Color(0xFFB3C7D6)
    val Muted = Color(0xFF859CAD)
    // Warm washi-paper daylight tokens
    val LightBackground = Color(0xFFF7F9FA)
    val LightSurface = Color(0xFFFFFEFC)
    val LightElevated = Color(0xFFEAF2F6)
    val LightText = Color(0xFF182C3B)
    val LightSecondary = Color(0xFF566E7D)
    val LightBlue = Color(0xFF23688E)
    val LightAqua = Color(0xFF58BDCB)
    val LightBorder = Color(0xFFD8E6EE)
}

val Ocean = OceanTokens.LightBlue
val Aqua = OceanTokens.Aqua

internal val HydroLightColors = lightColorScheme(
    primary = OceanTokens.LightBlue, onPrimary = Color.White,
    secondary = OceanTokens.LightBlue, tertiary = OceanTokens.LightAqua,
    background = OceanTokens.LightBackground, onBackground = OceanTokens.LightText,
    surface = OceanTokens.LightSurface, onSurface = OceanTokens.LightText,
    surfaceVariant = OceanTokens.LightElevated, onSurfaceVariant = OceanTokens.LightSecondary,
    primaryContainer = OceanTokens.LightElevated, onPrimaryContainer = OceanTokens.LightBlue,
    secondaryContainer = OceanTokens.LightElevated, onSecondaryContainer = OceanTokens.LightText,
    surfaceContainerLowest = OceanTokens.LightSurface, surfaceContainerLow = OceanTokens.LightBackground,
    surfaceContainer = OceanTokens.LightElevated, surfaceContainerHigh = OceanTokens.LightElevated,
    surfaceContainerHighest = OceanTokens.LightBorder,
    outline = OceanTokens.LightSecondary, outlineVariant = OceanTokens.LightBorder
)

internal val HydroDarkColors = darkColorScheme(
    primary = OceanTokens.Aqua, onPrimary = OceanTokens.Deep,
    secondary = OceanTokens.Blue, onSecondary = OceanTokens.Deep,
    tertiary = OceanTokens.Turquoise, onTertiary = OceanTokens.Deep,
    background = OceanTokens.Background, onBackground = OceanTokens.Text,
    surface = OceanTokens.Surface, onSurface = OceanTokens.Text,
    surfaceVariant = OceanTokens.Elevated, onSurfaceVariant = OceanTokens.Secondary,
    primaryContainer = OceanTokens.Interactive, onPrimaryContainer = OceanTokens.Aqua,
    secondaryContainer = OceanTokens.Elevated, onSecondaryContainer = OceanTokens.Text,
    surfaceContainerLowest = OceanTokens.Deep, surfaceContainerLow = OceanTokens.Surface,
    surfaceContainer = OceanTokens.Elevated, surfaceContainerHigh = OceanTokens.Interactive,
    surfaceContainerHighest = OceanTokens.Interactive,
    outline = OceanTokens.Muted, outlineVariant = OceanTokens.Elevated
)

internal val HydroOledDarkColors = HydroDarkColors.copy(
    background = Color.Black,
    surface = Color(0xFF07111A),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF050D14),
    surfaceVariant = Color(0xFF0D202F),
    primaryContainer = Color(0xFF0F293C)
)

internal fun paletteColorScheme(
    palette: OceanPalette,
    dark: Boolean,
    oled: Boolean = false,
    customAccentHex: Long = 0L
): ColorScheme {
    val scheme = when (palette) {
        OceanPalette.OCEAN_BLUE, OceanPalette.MATERIAL_YOU ->
            if (dark) HydroDarkColors else HydroLightColors

        OceanPalette.TRUE_OLED_BLACK ->
            if (dark) HydroOledDarkColors else HydroLightColors

        OceanPalette.MOONLIGHT_BLUE -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF8CC2F0),
                onPrimary = Color(0xFF08192B),
                secondary = Color(0xFF7AD7E6),
                tertiary = Color(0xFFB4D8F8),
                background = Color(0xFF0A1322),
                surface = Color(0xFF132036),
                surfaceVariant = Color(0xFF1C2E4A),
                primaryContainer = Color(0xFF21385A),
                onPrimaryContainer = Color(0xFFDCEEFF),
                onBackground = Color(0xFFF3F7FC),
                onSurface = Color(0xFFF3F7FC),
                onSurfaceVariant = Color(0xFFB5C7DC)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF2D6596),
                onPrimary = Color.White,
                secondary = Color(0xFF4782B5),
                tertiary = Color(0xFF69A8D9),
                background = Color(0xFFF6F8FB),
                surface = Color(0xFFFFFEFC),
                surfaceVariant = Color(0xFFE7EFF7),
                primaryContainer = Color(0xFFDCE9F7),
                onPrimaryContainer = Color(0xFF143859),
                onBackground = Color(0xFF1B2B3C),
                onSurface = Color(0xFF1B2B3C),
                onSurfaceVariant = Color(0xFF566B80),
                outline = Color(0xFF6B8196),
                outlineVariant = Color(0xFFD2E0EE)
            )
        }

        OceanPalette.SAKURA_BLUSH -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFFF2AEBE),
                onPrimary = Color(0xFF2E1019),
                secondary = Color(0xFFE8B89A),
                tertiary = Color(0xFFD5B8E8),
                background = Color(0xFF181218),
                surface = Color(0xFF251C25),
                surfaceVariant = Color(0xFF342734),
                primaryContainer = Color(0xFF452E3C),
                onPrimaryContainer = Color(0xFFFFE3EA),
                onBackground = Color(0xFFFAF3F5),
                onSurface = Color(0xFFFAF3F5),
                onSurfaceVariant = Color(0xFFD8C2CA)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFFB0566E),
                onPrimary = Color.White,
                secondary = Color(0xFFC47389),
                tertiary = Color(0xFFD695A8),
                background = Color(0xFFFCF8F6),
                surface = Color(0xFFFFFEFD),
                surfaceVariant = Color(0xFFF6EAEB),
                primaryContainer = Color(0xFFF5DFE3),
                onPrimaryContainer = Color(0xFF521E2D),
                onBackground = Color(0xFF332227),
                onSurface = Color(0xFF332227),
                onSurfaceVariant = Color(0xFF755B63),
                outline = Color(0xFF8E727A),
                outlineVariant = Color(0xFFEBD6DA)
            )
        }

        OceanPalette.MATCHA_MIST -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF94D4AA),
                onPrimary = Color(0xFF0B2616),
                secondary = Color(0xFF82C9B9),
                tertiary = Color(0xFFC2E5B8),
                background = Color(0xFF0C1813),
                surface = Color(0xFF152720),
                surfaceVariant = Color(0xFF1F382E),
                primaryContainer = Color(0xFF25473A),
                onPrimaryContainer = Color(0xFFDDF7E5),
                onBackground = Color(0xFFF2FAF5),
                onSurface = Color(0xFFF2FAF5),
                onSurfaceVariant = Color(0xFFB6D1C1)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF3B7A57),
                onPrimary = Color.White,
                secondary = Color(0xFF51916D),
                tertiary = Color(0xFF72B088),
                background = Color(0xFFF7F9F4),
                surface = Color(0xFFFFFEFC),
                surfaceVariant = Color(0xFFE7F0E6),
                primaryContainer = Color(0xFFDCEBDD),
                onPrimaryContainer = Color(0xFF163D27),
                onBackground = Color(0xFF1E3024),
                onSurface = Color(0xFF1E3024),
                onSurfaceVariant = Color(0xFF556E5E),
                outline = Color(0xFF6B8574),
                outlineVariant = Color(0xFFD2E2D4)
            )
        }

        OceanPalette.KYOTO_SKY -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF9AC5EB),
                onPrimary = Color(0xFF0B1F33),
                secondary = Color(0xFF9EB4E5),
                tertiary = Color(0xFFF0C3A5),
                background = Color(0xFF0E1624),
                surface = Color(0xFF172338),
                surfaceVariant = Color(0xFF22324D),
                primaryContainer = Color(0xFF283D5E),
                onPrimaryContainer = Color(0xFFE0F0FF),
                onBackground = Color(0xFFF4F7FB),
                onSurface = Color(0xFFF4F7FB),
                onSurfaceVariant = Color(0xFFBDCBE0)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF386C9C),
                onPrimary = Color.White,
                secondary = Color(0xFF5484B3),
                tertiary = Color(0xFF7AAAD6),
                background = Color(0xFFF8F8F6),
                surface = Color(0xFFFFFEFC),
                surfaceVariant = Color(0xFFE8EFF6),
                primaryContainer = Color(0xFFDCE8F5),
                onPrimaryContainer = Color(0xFF163654),
                onBackground = Color(0xFF1F2D3A),
                onSurface = Color(0xFF1F2D3A),
                onSurfaceVariant = Color(0xFF5B6D7E),
                outline = Color(0xFF728596),
                outlineVariant = Color(0xFFD5E1ED)
            )
        }

        OceanPalette.LANTERN_AMBER -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFFF5B97A),
                onPrimary = Color(0xFF2E1904),
                secondary = Color(0xFFEFA078),
                tertiary = Color(0xFF88C9D4),
                background = Color(0xFF17120E),
                surface = Color(0xFF261E18),
                surfaceVariant = Color(0xFF362B22),
                primaryContainer = Color(0xFF4A3828),
                onPrimaryContainer = Color(0xFFFFE8D1),
                onBackground = Color(0xFFFAF5F0),
                onSurface = Color(0xFFFAF5F0),
                onSurfaceVariant = Color(0xFFD9C6B6)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFFB36524),
                onPrimary = Color.White,
                secondary = Color(0xFFC97B3A),
                tertiary = Color(0xFFE09A5C),
                background = Color(0xFFFCF8F2),
                surface = Color(0xFFFFFEFC),
                surfaceVariant = Color(0xFFF5EAE0),
                primaryContainer = Color(0xFFF5E2CE),
                onPrimaryContainer = Color(0xFF4F2706),
                onBackground = Color(0xFF332418),
                onSurface = Color(0xFF332418),
                onSurfaceVariant = Color(0xFF755F4D),
                outline = Color(0xFF8C7460),
                outlineVariant = Color(0xFFEADACB)
            )
        }

        OceanPalette.INDIGO_NIGHT -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF9EB3F0),
                onPrimary = Color(0xFF101B3B),
                secondary = Color(0xFF82C4E6),
                tertiary = Color(0xFFF2C18D),
                background = Color(0xFF090E1C),
                surface = Color(0xFF121B30),
                surfaceVariant = Color(0xFF1B2845),
                primaryContainer = Color(0xFF24355C),
                onPrimaryContainer = Color(0xFFE0E8FF),
                onBackground = Color(0xFFF2F5FC),
                onSurface = Color(0xFFF2F5FC),
                onSurfaceVariant = Color(0xFFB6C2DC)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF364B8A),
                onPrimary = Color.White,
                secondary = Color(0xFF4F66A8),
                tertiary = Color(0xFF6C82C4),
                background = Color(0xFFF6F7FB),
                surface = Color(0xFFFFFEFC),
                surfaceVariant = Color(0xFFE6EAF5),
                primaryContainer = Color(0xFFDCE2F5),
                onPrimaryContainer = Color(0xFF16234D),
                onBackground = Color(0xFF1D243B),
                onSurface = Color(0xFF1D243B),
                onSurfaceVariant = Color(0xFF58617D),
                outline = Color(0xFF6E7894),
                outlineVariant = Color(0xFFD4DAEC)
            )
        }

        OceanPalette.RAINY_TOKYO -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF7ADBE8),
                onPrimary = Color(0xFF062329),
                secondary = Color(0xFF9EB2F0),
                tertiary = Color(0xFFF2A7C3),
                background = Color(0xFF0B121A),
                surface = Color(0xFF14202E),
                surfaceVariant = Color(0xFF1E2F42),
                primaryContainer = Color(0xFF213C52),
                onPrimaryContainer = Color(0xFFD9F7FC),
                onBackground = Color(0xFFF2F7FA),
                onSurface = Color(0xFFF2F7FA),
                onSurfaceVariant = Color(0xFFB4C6D4)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF237482),
                onPrimary = Color.White,
                secondary = Color(0xFF3B8D9C),
                tertiary = Color(0xFF5BC0CE),
                background = Color(0xFFF4F8F9),
                surface = Color(0xFFFFFEFC),
                surfaceVariant = Color(0xFFE3EFF2),
                primaryContainer = Color(0xFFD6EBF0),
                onPrimaryContainer = Color(0xFF0B3B42),
                onBackground = Color(0xFF192B30),
                onSurface = Color(0xFF192B30),
                onSurfaceVariant = Color(0xFF546C73),
                outline = Color(0xFF6A838A),
                outlineVariant = Color(0xFFCFE0E5)
            )
        }

        OceanPalette.PLUM_INK -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFFC9ADE3),
                onPrimary = Color(0xFF241336),
                secondary = Color(0xFFE3B2C8),
                tertiary = Color(0xFFE8C89E),
                background = Color(0xFF130F1A),
                surface = Color(0xFF1F192B),
                surfaceVariant = Color(0xFF2D243D),
                primaryContainer = Color(0xFF3B2E52),
                onPrimaryContainer = Color(0xFFEEE2FA),
                onBackground = Color(0xFFF7F4FA),
                onSurface = Color(0xFFF7F4FA),
                onSurfaceVariant = Color(0xFFC9BDD6)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF6A4C82),
                onPrimary = Color.White,
                secondary = Color(0xFF83649C),
                tertiary = Color(0xFF9E7FB8),
                background = Color(0xFFFAF7F5),
                surface = Color(0xFFFFFEFC),
                surfaceVariant = Color(0xFFEEE8F2),
                primaryContainer = Color(0xFFE6DDF0),
                onPrimaryContainer = Color(0xFF2E1B40),
                onBackground = Color(0xFF292033),
                onSurface = Color(0xFF292033),
                onSurfaceVariant = Color(0xFF665A73),
                outline = Color(0xFF7D708C),
                outlineVariant = Color(0xFFDDD4E6)
            )
        }

        OceanPalette.BAMBOO_GREEN -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF84CCB3),
                onPrimary = Color(0xFF07261C),
                secondary = Color(0xFF78BFCC),
                tertiary = Color(0xFFD6CC98),
                background = Color(0xFF0A1714),
                surface = Color(0xFF132622),
                surfaceVariant = Color(0xFF1C3731),
                primaryContainer = Color(0xFF22473F),
                onPrimaryContainer = Color(0xFFD7F7EC),
                onBackground = Color(0xFFF1FAF7),
                onSurface = Color(0xFFF1FAF7),
                onSurfaceVariant = Color(0xFFB3CFC7)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF2E6E59),
                onPrimary = Color.White,
                secondary = Color(0xFF458771),
                tertiary = Color(0xFF5E9F87),
                background = Color(0xFFF6F9F6),
                surface = Color(0xFFFFFEFC),
                surfaceVariant = Color(0xFFE4EFEA),
                primaryContainer = Color(0xFFD8EAE3),
                onPrimaryContainer = Color(0xFF0E382A),
                onBackground = Color(0xFF1A2E27),
                onSurface = Color(0xFF1A2E27),
                onSurfaceVariant = Color(0xFF526B62),
                outline = Color(0xFF688278),
                outlineVariant = Color(0xFFCEE0D9)
            )
        }

        OceanPalette.SOFT_CREAM -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFFE3C19D),
                onPrimary = Color(0xFF2B1D0E),
                secondary = Color(0xFF9AC5CC),
                tertiary = Color(0xFFD9A7A0),
                background = Color(0xFF161412),
                surface = Color(0xFF24201C),
                surfaceVariant = Color(0xFF332E28),
                primaryContainer = Color(0xFF453C33),
                onPrimaryContainer = Color(0xFFFCECD9),
                onBackground = Color(0xFFFAF6F0),
                onSurface = Color(0xFFFAF6F0),
                onSurfaceVariant = Color(0xFFD4C9BC)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF7D5D3B),
                onPrimary = Color.White,
                secondary = Color(0xFF4F7A82),
                tertiary = Color(0xFFA6825B),
                background = Color(0xFFFAF6EE),
                surface = Color(0xFFFFFDF9),
                surfaceVariant = Color(0xFFF0E8DA),
                primaryContainer = Color(0xFFEBE0CE),
                onPrimaryContainer = Color(0xFF382612),
                onBackground = Color(0xFF2B231B),
                onSurface = Color(0xFF2B231B),
                onSurfaceVariant = Color(0xFF695D4F),
                outline = Color(0xFF827465),
                outlineVariant = Color(0xFFE2D6C5)
            )
        }

        OceanPalette.SKY_BLUE -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF8CCBFF),
                onPrimary = Color(0xFF061E36),
                secondary = Color(0xFF72DDF7),
                tertiary = Color(0xFFA8D8FF),
                background = Color(0xFF091626),
                surface = Color(0xFF11233A),
                surfaceVariant = Color(0xFF19314F),
                primaryContainer = Color(0xFF1A3A5E),
                onPrimaryContainer = Color(0xFFD9EEFF)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF2576C4),
                onPrimary = Color.White,
                secondary = Color(0xFF3E8EDE),
                tertiary = Color(0xFF66B6FF),
                background = Color(0xFFF3F8FF),
                surface = Color.White,
                surfaceVariant = Color(0xFFE4F0FF),
                primaryContainer = Color(0xFFDCEBFF),
                onPrimaryContainer = Color(0xFF0D3D70),
                outline = Color(0xFF567494),
                outlineVariant = Color(0xFFCFE2FA)
            )
        }

        OceanPalette.LAVENDER -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFFCBB2FF),
                onPrimary = Color(0xFF1E1038),
                secondary = Color(0xFFB392F0),
                tertiary = Color(0xFFF0B6E8),
                background = Color(0xFF110D1F),
                surface = Color(0xFF1C1632),
                surfaceVariant = Color(0xFF282046),
                primaryContainer = Color(0xFF312659),
                onPrimaryContainer = Color(0xFFEDE2FF),
                onBackground = Color(0xFFF8F4FF),
                onSurface = Color(0xFFF8F4FF),
                onSurfaceVariant = Color(0xFFC9BEE0)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF6D4BC3),
                onPrimary = Color.White,
                secondary = Color(0xFF8665D8),
                tertiary = Color(0xFFB57EDC),
                background = Color(0xFFF8F5FF),
                surface = Color.White,
                surfaceVariant = Color(0xFFEDE6FF),
                primaryContainer = Color(0xFFE6DCFF),
                onPrimaryContainer = Color(0xFF2E1768),
                onBackground = Color(0xFF22163E),
                onSurface = Color(0xFF22163E),
                onSurfaceVariant = Color(0xFF635685),
                outline = Color(0xFF756799),
                outlineVariant = Color(0xFFDDD1FA)
            )
        }

        OceanPalette.MINT -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF80F2CA),
                onPrimary = Color(0xFF04261B),
                secondary = Color(0xFF64E3D4),
                tertiary = Color(0xFFA8F7DC),
                background = Color(0xFF071A15),
                surface = Color(0xFF0F2B23),
                surfaceVariant = Color(0xFF173D32),
                primaryContainer = Color(0xFF194B3D),
                onPrimaryContainer = Color(0xFFD2FFEE),
                onBackground = Color(0xFFF0FFF9),
                onSurface = Color(0xFFF0FFF9),
                onSurfaceVariant = Color(0xFFB1D8CA)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF188363),
                onPrimary = Color.White,
                secondary = Color(0xFF259E7B),
                tertiary = Color(0xFF47C4A1),
                background = Color(0xFFF1FCF8),
                surface = Color.White,
                surfaceVariant = Color(0xFFDFF7EE),
                primaryContainer = Color(0xFFD4F5E8),
                onPrimaryContainer = Color(0xFF084733),
                onBackground = Color(0xFF0E3529),
                onSurface = Color(0xFF0E3529),
                onSurfaceVariant = Color(0xFF4C7567),
                outline = Color(0xFF5B8778),
                outlineVariant = Color(0xFFC8EDE0)
            )
        }

        OceanPalette.PEACH -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFFFFB599),
                onPrimary = Color(0xFF331307),
                secondary = Color(0xFFFFCBA4),
                tertiary = Color(0xFFFF9EAA),
                background = Color(0xFF1C110E),
                surface = Color(0xFF2B1B17),
                surfaceVariant = Color(0xFF3D2721),
                primaryContainer = Color(0xFF4E3028),
                onPrimaryContainer = Color(0xFFFFE5DB),
                onBackground = Color(0xFFFFF6F3),
                onSurface = Color(0xFFFFF6F3),
                onSurfaceVariant = Color(0xFFE0C0B6)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFFC25430),
                onPrimary = Color.White,
                secondary = Color(0xFFD96B45),
                tertiary = Color(0xFFF08F6C),
                background = Color(0xFFFFF7F3),
                surface = Color.White,
                surfaceVariant = Color(0xFFFFE9DE),
                primaryContainer = Color(0xFFFFE0D1),
                onPrimaryContainer = Color(0xFF5C1E09),
                onBackground = Color(0xFF3B1A0E),
                onSurface = Color(0xFF3B1A0E),
                onSurfaceVariant = Color(0xFF7D5A4E),
                outline = Color(0xFF946D60),
                outlineVariant = Color(0xFFF8D6C6)
            )
        }

        OceanPalette.ROSE -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFFFFA8C5),
                onPrimary = Color(0xFF360B1B),
                secondary = Color(0xFFF794C0),
                tertiary = Color(0xFFD8B4F8),
                background = Color(0xFF1B0E14),
                surface = Color(0xFF2B1721),
                surfaceVariant = Color(0xFF3D212F),
                primaryContainer = Color(0xFF4F283B),
                onPrimaryContainer = Color(0xFFFFE0EC),
                onBackground = Color(0xFFFFF4F8),
                onSurface = Color(0xFFFFF4F8),
                onSurfaceVariant = Color(0xFFE0B8C7)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFFB83D68),
                onPrimary = Color.White,
                secondary = Color(0xFFD15480),
                tertiary = Color(0xFFE87EA4),
                background = Color(0xFFFFF5F8),
                surface = Color.White,
                surfaceVariant = Color(0xFFFFE4EE),
                primaryContainer = Color(0xFFFFD9E7),
                onPrimaryContainer = Color(0xFF59122C),
                onBackground = Color(0xFF3A1221),
                onSurface = Color(0xFF3A1221),
                onSurfaceVariant = Color(0xFF7D5263),
                outline = Color(0xFF946476),
                outlineVariant = Color(0xFFF7CEDD)
            )
        }

        OceanPalette.DEEP_SEA -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF48CAE4),
                secondary = Color(0xFF00B4D8),
                tertiary = Color(0xFF90E0EF),
                background = Color(0xFF030B14),
                surface = Color(0xFF091D30),
                primaryContainer = Color(0xFF10324E),
                onPrimaryContainer = Color(0xFFADE8F4)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF005F87),
                secondary = Color(0xFF0077B6),
                primaryContainer = Color(0xFFCAF0F8),
                onPrimaryContainer = Color(0xFF03045E)
            )
        }

        OceanPalette.ARCTIC_ICE -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFFA8DADC),
                secondary = Color(0xFF72EFDD),
                tertiary = Color(0xFFCAF0F8),
                background = Color(0xFF0A1923),
                surface = Color(0xFF132938),
                primaryContainer = Color(0xFF1D3E53),
                onPrimaryContainer = Color(0xFFE0FBFC)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF1D6587),
                background = Color(0xFFF4FAFC),
                primaryContainer = Color(0xFFE0F4F9),
                onPrimaryContainer = Color(0xFF0F3D54)
            )
        }

        OceanPalette.TROPICAL_AQUA -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF64DFDF),
                secondary = Color(0xFF48BFE3),
                tertiary = Color(0xFF80FFDB),
                background = Color(0xFF04181C),
                surface = Color(0xFF0B2E36),
                primaryContainer = Color(0xFF124652),
                onPrimaryContainer = Color(0xFF80FFDB)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF007F86),
                secondary = Color(0xFF0096C7),
                background = Color(0xFFEDF9F9),
                primaryContainer = Color(0xFFD4F5F5),
                onPrimaryContainer = Color(0xFF004E52)
            )
        }

        OceanPalette.MIDNIGHT_WAVE -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF8ECAE6),
                secondary = Color(0xFF729BFA),
                tertiary = Color(0xFF7EDCEB),
                background = Color(0xFF070F22),
                surface = Color(0xFF111E3B),
                primaryContainer = Color(0xFF1B2E58),
                onPrimaryContainer = Color(0xFFCAE9FF)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF214E8A),
                secondary = Color(0xFF3A6EA5),
                background = Color(0xFFF1F5FB),
                primaryContainer = Color(0xFFDCE7F9),
                onPrimaryContainer = Color(0xFF132E57)
            )
        }

        OceanPalette.SOFT_MINT -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFF7AE5C6),
                secondary = Color(0xFF64DFDF),
                tertiary = Color(0xFFA8F5DC),
                background = Color(0xFF051814),
                surface = Color(0xFF0E2C25),
                primaryContainer = Color(0xFF164238),
                onPrimaryContainer = Color(0xFFB7F7E3)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF116D58),
                secondary = Color(0xFF1D8A72),
                background = Color(0xFFF0FAF7),
                primaryContainer = Color(0xFFD7F3EA),
                onPrimaryContainer = Color(0xFF094436)
            )
        }

        OceanPalette.CORAL_SUNSET -> if (dark) {
            HydroDarkColors.copy(
                primary = Color(0xFFFF9F7E),
                secondary = Color(0xFF7EDCEB),
                tertiary = Color(0xFFFFCB77),
                background = Color(0xFF121524),
                surface = Color(0xFF1E243B),
                primaryContainer = Color(0xFF2F3654),
                onPrimaryContainer = Color(0xFFFFD6C7)
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFFB84A27),
                secondary = Color(0xFF126A90),
                background = Color(0xFFFCF6F3),
                primaryContainer = Color(0xFFFCE2D8),
                onPrimaryContainer = Color(0xFF6E250E)
            )
        }

        OceanPalette.MINIMAL_WHITE -> if (dark) {
            HydroOledDarkColors.copy(
                primary = Color(0xFFE2F1F8),
                secondary = Color(0xFF98C1D9),
                primaryContainer = Color(0xFF182834),
                onPrimaryContainer = Color.White
            )
        } else {
            HydroLightColors.copy(
                primary = Color(0xFF0F293A),
                secondary = Color(0xFF284B63),
                background = Color.White,
                surface = Color(0xFFF8FBFD),
                primaryContainer = Color(0xFFEAF2F6),
                onPrimaryContainer = Color(0xFF0F293A)
            )
        }
    }

    val oledAdjusted = if (oled && dark) {
        scheme.copy(
            background = Color.Black,
            surface = Color(0xFF080C12),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF05080D)
        )
    } else {
        scheme
    }

    return if (customAccentHex != 0L) {
        val accent = Color(customAccentHex)
        if (dark) {
            oledAdjusted.copy(
                primary = accent,
                secondary = accent.copy(alpha = 0.85f),
                primaryContainer = accent.copy(alpha = 0.22f),
                onPrimaryContainer = Color.White
            )
        } else {
            oledAdjusted.copy(
                primary = accent,
                secondary = accent.copy(alpha = 0.85f),
                primaryContainer = accent.copy(alpha = 0.16f)
            )
        }
    } else {
        oledAdjusted
    }
}

object Space {
    val base = 4.dp
    val small = 8.dp
    val gap = 12.dp
    val medium = 16.dp
    val page = 20.dp
    val large = 24.dp
    val major = 32.dp
}

@Composable
fun HydroTheme(settings: HydroSettings, content: @Composable () -> Unit) {
    val isOled = settings.theme == ThemeMode.OLED ||
        (settings.palette == OceanPalette.TRUE_OLED_BLACK && settings.theme != ThemeMode.LIGHT)
    val dark = when {
        isOled -> true
        settings.theme == ThemeMode.SYSTEM -> isSystemInDarkTheme()
        settings.theme == ThemeMode.DARK -> true
        else -> false
    }
    val context = LocalContext.current
    val baseColors = paletteColorScheme(
        palette = settings.palette,
        dark = dark,
        oled = isOled,
        customAccentHex = settings.customAccentHex
    )
    val colors = if ((settings.dynamicColors || settings.palette == OceanPalette.MATERIAL_YOU) && Build.VERSION.SDK_INT >= 31) {
        val device = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (settings.palette == OceanPalette.MATERIAL_YOU) {
            if (isOled) device.copy(background = Color.Black, surface = Color(0xFF080C12), surfaceContainerLowest = Color.Black)
            else device
        } else {
            baseColors.copy(outline = device.outline, outlineVariant = device.outlineVariant)
        }
    } else {
        baseColors
    }
    SideEffect {
        (context as? android.app.Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = kissatenColors(colors, isOled),
        typography = kissatenTypography(Typography(
            displayLarge = TextStyle(fontFamily = KissatenBody, fontWeight = FontWeight.Light, fontSize = 48.sp, lineHeight = 56.sp, letterSpacing = (-2).sp),
            headlineLarge = TextStyle(fontFamily = KissatenBody, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
            headlineMedium = TextStyle(fontFamily = KissatenBody, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 34.sp),
            titleLarge = TextStyle(fontFamily = KissatenBody, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontFamily = KissatenBody, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
            labelLarge = TextStyle(fontFamily = KissatenBody, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
            labelMedium = TextStyle(fontFamily = KissatenBody, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp)
        )),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(16.dp),
            small = RoundedCornerShape(20.dp),
            medium = RoundedCornerShape(24.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(36.dp)
        ),
        content = content
    )
}
