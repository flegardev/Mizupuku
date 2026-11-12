package app.hydronudge.qa

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.work.*
import app.hydronudge.*
import app.hydronudge.domain.*
import app.hydronudge.reminders.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReminderDeliveryTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun genuineBackgroundWorkManagerReminderPostsWhenNaturallyEligible() {
        val app=compose.activity.application as HydroApplication
        val original=runBlocking {app.preferences.settings.first()}
        val settings=original.copy(reminders=true,intervalMinutes=15,wakeMinute=0,sleepMinute=0,skipAtGoal=false,pausedUntil=0)
        val now=ZonedDateTime.now()
        val entries=runBlocking {app.repository.all()}
        val total=entries.filter {it.isWater() && localDay(it.timestamp,now.zone)==now.toLocalDate()}.sumOf {it.amountMl.toLong()}
        val last=entries.filter {it.timestamp<=now.toInstant().toEpochMilli()}.maxOfOrNull {it.timestamp}
        assumeTrue("Recent real drinks suppress reminders; never alter user records to force eligibility",ReminderPolicy.shouldRemind(settings,now,total,last))
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val originalGranted=Build.VERSION.SDK_INT<33 || app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
        val manager=WorkManager.getInstance(app)
        val name="qa-reminder:${UUID.randomUUID()}"
        val notifications=app.getSystemService(NotificationManager::class.java)
        try {
            if(Build.VERSION.SDK_INT>=33 && !originalGranted) automation.grantRuntimePermission(app.packageName,Manifest.permission.POST_NOTIFICATIONS)
            runBlocking {app.preferences.save(settings)}
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
            val request=OneTimeWorkRequestBuilder<ReminderWorker>().addTag("hydronudge-qa").build()
            manager.enqueueUniqueWork(name,ExistingWorkPolicy.KEEP,request).result.get()
            compose.waitUntil(60000) {manager.getWorkInfoById(request.id).get()?.state==WorkInfo.State.SUCCEEDED}
            assertTrue(notifications.activeNotifications.any {it.id==2})
            QaArtifacts.note("PASS genuine ReminderWorker executed through Android WorkManager with the app backgrounded and posted its notification; no fake timestamps or modified user records")
            QaArtifacts.note("LIMITATION one-time QA work exercises the actual periodic worker class; periodic/Doze timing is still best effort and not asserted")
        } finally {
            manager.cancelUniqueWork(name).result.get()
            notifications.cancel(2)
            runBlocking {app.preferences.save(original)}
            if(!originalGranted) File(QaArtifacts.folder,"restore-notification-permission.txt").writeText("revoke")
            QaArtifacts.shell("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n app.hydronudge/.MainActivity")
            QaArtifacts.logcat("reminder-worker")
        }
    }
}
