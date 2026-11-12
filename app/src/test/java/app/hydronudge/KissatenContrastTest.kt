package app.hydronudge

import app.hydronudge.ui.theme.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.lightColorScheme
import org.junit.Assert.*
import org.junit.Test

class KissatenContrastTest {
    @Test fun customAccentsStayLegibleAcrossLightDarkAndOled() {
        for (surface in listOf(Color.White, Color(0xFF211C24), Color.Black)) {
            for (accent in listOf(Color.White,Color.Black,Color.Yellow,Color(0xFFC86D85),Color(0xFFE59A44),Color(0xFF9378D8))) {
                val scheme=kissatenColors(lightColorScheme(primary=accent,surface=surface),surface==Color.Black)
                assertTrue("Primary on surface",kissatenContrast(scheme.primary,scheme.surface)>=4.5f)
                assertTrue("Button text",kissatenContrast(scheme.primary,scheme.onPrimary)>=4.5f)
                assertEquals(1f,scheme.primary.alpha)
            }
        }
    }
}
