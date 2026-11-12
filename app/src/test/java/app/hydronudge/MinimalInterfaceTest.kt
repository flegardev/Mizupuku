package app.hydronudge

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import app.hydronudge.data.local.DrinkEntry
import app.hydronudge.data.preferences.HydroSettings
import app.hydronudge.ui.HydroState
import app.hydronudge.ui.dashboard.Dashboard
import app.hydronudge.ui.history.HistoryScreen
import app.hydronudge.ui.settings.SettingsScreen
import app.hydronudge.ui.theme.HydroTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MinimalInterfaceTest {
    @get:Rule val compose=createComposeRule()

    @Test fun recentPreviewOpensFullHistoryWithoutDroppingEntries() {
        val date=LocalDate.of(2026,11,2)
        val zone=ZoneId.of("UTC")
        val start=date.atStartOfDay(zone).toInstant().toEpochMilli()
        val entries=(4 downTo 0).map {DrinkEntry(id="recent:$it",name="Water",category="WATER",amountMl=250,timestamp=start+it*1000)}
        val state=HydroState(entries=entries,date=date,zone=zone,ready=true)
        compose.setContent {
            var history by remember {mutableStateOf(false)}
            HydroTheme(state.settings) {
                if(history) HistoryScreen(state,{}, {})
                else Dashboard(state,{}, {}, {}, {}, {}, {history=true})
            }
        }
        compose.onNodeWithTag("dashboard-list").performScrollToNode(hasTestTag("drink:recent:4"))
        compose.onNodeWithTag("drink:recent:0").assertDoesNotExist()
        compose.onNodeWithTag("view-all-water").performScrollTo().performClick()
        compose.onNodeWithTag("history-list").performScrollToNode(hasTestTag("drink:recent:0"))
        compose.onNodeWithTag("drink:recent:0").assertIsDisplayed()
        assertEquals(5,state.entries.size)
    }

    @Test fun advancedQuickAmountsRequireSaveAndBackupHelpRemainsReachable() {
        var saved=HydroSettings()
        compose.setContent {
            var settings by remember {mutableStateOf(saved)}
            HydroTheme(settings) {SettingsScreen(settings,{saved=it;settings=it},{},{},{})}
        }
        compose.onNodeWithText("Save quick amounts").assertDoesNotExist()
        compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("settings:quick-add"))
        compose.onNodeWithTag("settings:quick-add").performClick()
        compose.onNodeWithText("Quick-add amounts (mL)").performScrollTo().performTextReplacement("100, 200, 300, 400")
        assertEquals(listOf(150,250,330,500),saved.quickAmounts)
        compose.onNodeWithText("Save quick amounts").performScrollTo().performClick()
        compose.runOnIdle {assertEquals(listOf(100,200,300,400),saved.quickAmounts)}
        compose.onNodeWithTag("settings-list").performScrollToNode(hasTestTag("settings:quick-add"))
        compose.onNodeWithTag("settings:quick-add").performClick()
        compose.onNodeWithText("Save quick amounts").assertDoesNotExist()
        compose.onNodeWithTag("settings-list").performScrollToNode(hasText("How backups work"))
        compose.onNodeWithText("How backups work").performClick()
        compose.onNodeWithText("Preferences stay on this device.",substring=true).assertExists()
        compose.onNodeWithText("Close").performClick()
    }
}
