package app.hydronudge.qa

import app.hydronudge.HydroApplication
import kotlinx.coroutines.runBlocking
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class EmulatorWidgetSizesTest {
    @Test fun renderActualRemoteViewsAtAllResponsiveSizes() {
        assumeTrue(Build.MODEL.contains("sdk",true))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val app=context.applicationContext as HydroApplication
        for((width,height) in listOf(110 to 110,240 to 150,280 to 240)) {
            val intent = Intent(context,WidgetHostActivity::class.java).putExtra("width",width).putExtra("height",height)
            ActivityScenario.launch<WidgetHostActivity>(intent).use {
                assertTrue("Widget did not render",device.wait(Until.hasObject(By.text(if(width<240) "WATER" else "Mizupuku")),15000))
                Thread.sleep(500)
                check(device.takeScreenshot(File(QaArtifacts.folder,"widget-before-$width.png")))
                if(width>=240) {
                    val action=checkNotNull(device.findObject(By.text("+ 250 mL")))
                    assertTrue("Medium action is clipped",action.visibleBounds.height() / context.resources.displayMetrics.density >= 10 && maxOf(action.visibleBounds.height(),action.parent?.visibleBounds?.height() ?: 0) / context.resources.displayMetrics.density >= 40)
                }
                if(width>=240) {
                    for(ml in listOf(250,500)) {
                        val before=runBlocking {app.repository.all()}.map {it.id}.toSet()
                        try {
                            checkNotNull(device.wait(Until.findObject(By.text("+ $ml mL")),5000)).click()
                            val end=System.currentTimeMillis()+10000
                            while(System.currentTimeMillis()<end && runBlocking {app.repository.all()}.none {it.id !in before}) Thread.sleep(100)
                            val fresh=runBlocking {app.repository.all()}.filter {it.id !in before}
                            assertEquals(1,fresh.size);assertEquals(ml,fresh.single().amountMl);assertEquals("WATER",fresh.single().category)
                        } finally {
                            runBlocking {app.repository.all().filter {it.id !in before && it.id.startsWith("widget:")}.forEach {app.repository.delete(it.id)}}
                        }
                    }
                }
                Thread.sleep(500)
                val mode=if(context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES) "dark" else "light"
                check(device.takeScreenshot(File(QaArtifacts.folder,"widget-$mode-${width}x$height.png")))
            }
        }
        QaArtifacts.note("PASS real native AppWidgetHost rendered 110x110, 240x150 and 280x240 RemoteViews; each newly allocated host ID deleted")
    }
}
