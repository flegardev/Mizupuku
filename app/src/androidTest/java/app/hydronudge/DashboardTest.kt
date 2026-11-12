package app.hydronudge

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import org.junit.Rule
import org.junit.Test

class DashboardTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun dashboardAndWaterSheetAreAccessible() {
        compose.waitUntil(15000) { compose.onAllNodesWithText("Custom amount").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Custom amount").performScrollTo().performClick()
        compose.onNodeWithTag("drink-editor").assertIsDisplayed()
        compose.onNodeWithText("Milk").assertDoesNotExist()
        compose.onNodeWithText("Protein shake").assertDoesNotExist()
    }
}
