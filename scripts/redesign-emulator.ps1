param(
    [string] $Serial = 'emulator-5554',
    [string] $TestClasses = 'app.hydronudge.StorageTest,app.hydronudge.DashboardTest,app.hydronudge.qa.EmulatorRedesignTest',
    [switch] $SkipBuild
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/_common.ps1"
$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
if ($Serial -notmatch '^emulator-\d+$') { throw 'This script accepts emulator serials only. Physical phones are prohibited.' }
$isEmulator = & $adb -s $Serial shell getprop ro.kernel.qemu
if ($LASTEXITCODE -ne 0 -or "$isEmulator".Trim() -ne '1') { throw 'The selected target is not a verified running emulator.' }
$devices = & $adb devices -l
$devices | Write-Output
if ((& $adb -s $Serial get-state).Trim() -ne 'device' -or (& $adb -s $Serial shell getprop sys.boot_completed).Trim() -ne '1') { throw 'Selected emulator is not online and booted.' }
if (-not $SkipBuild) { Invoke-HydroGradle -GradleArgs @('assembleDebug', 'assembleDebugAndroidTest') }
& $adb -s $Serial install -r (Join-Path $script:ProjectRoot 'app/build/outputs/apk/debug/app-debug.apk')
if ($LASTEXITCODE -ne 0) { throw 'App update failed.' }
& $adb -s $Serial install -r (Join-Path $script:ProjectRoot 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')
if ($LASTEXITCODE -ne 0) { throw 'Test APK update failed.' }
$result = & $adb -s $Serial shell am instrument --user 0 -w -r -e disableAnalytics true -e class $TestClasses com.tiniflegar.mizupuku.test/androidx.test.runner.AndroidJUnitRunner
$result | Write-Output
if ($LASTEXITCODE -ne 0 -or ($result -join "`n") -notmatch 'OK \(\d+ tests?\)' -or ($result -join "`n") -match 'FAILURES!!!|INSTRUMENTATION_FAILED') {
    throw 'Instrumentation did not report a passing result. Inspect the retained runner output.'
}

