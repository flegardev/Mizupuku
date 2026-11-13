param(
    [ValidateSet('list','connect','install','launch','screenshot','crashlog')]
    [string] $Action = 'list',
    [string] $Endpoint,
    [string] $Serial
)
. (Join-Path $PSScriptRoot '_common.ps1')
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
$package = 'com.tiniflegar.mizupuku'
$apk = Join-Path $script:ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'
$outDir = Join-Path $script:ProjectRoot 'qa-reports\local-device'

switch ($Action) {
    'list' {
        & $adb devices -l
        Write-Host "For wireless discovery run: `"$adb`" mdns services"
    }
    'connect' {
        if (-not $Endpoint -or $Endpoint -notmatch '^\[[0-9a-fA-F:]+\]:\d+$|^[0-9.]+:\d+$') {
            throw 'Supply the Samsung wireless-debugging connection endpoint as -Endpoint ip:port (shown by adb mdns services or Android Studio).'
        }
        & $adb connect $Endpoint
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $adb devices -l
    }
    default {
        if (-not $Serial) { throw "Supply the selected phone's ADB serial using -Serial. Use Action list to see it." }
        $online = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '^\S+\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
        if ($Serial -notin $online) { throw "Selected ADB device is not online: $Serial" }
        switch ($Action) {
            'install' {
                if (-not (Test-Path -LiteralPath $apk)) { throw 'Debug APK is missing. Run scripts/build-debug.ps1 first.' }
                & $adb -s $Serial install -r $apk
                if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                Write-Host 'Install used adb install -r. It does not uninstall the app or clear its data.'
            }
            'launch' {
                & $adb -s $Serial shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$package/.MainActivity"
                if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
            }
            'screenshot' {
                New-Item -ItemType Directory -Path $outDir -Force | Out-Null
                & $adb -s $Serial shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$package/.MainActivity" | Out-Host
                $foreground = (& $adb -s $Serial shell dumpsys activity activities) -join "`n"
                if ($foreground -notmatch 'app\.hydronudge') { throw 'HydroNudge is not confirmed in the foreground; screenshot cancelled.' }
                $path = Join-Path $outDir ("HydroNudge-{0:yyyyMMdd-HHmmss}.png" -f (Get-Date))
                $psi = [Diagnostics.ProcessStartInfo]::new($adb, "-s `"$Serial`" exec-out screencap -p")
                $psi.UseShellExecute = $false; $psi.RedirectStandardOutput = $true; $psi.CreateNoWindow = $true
                $proc = [Diagnostics.Process]::Start($psi)
                $stream = [IO.File]::Create($path)
                try { $proc.StandardOutput.BaseStream.CopyTo($stream); $stream.Flush() } finally { $stream.Dispose() }
                $proc.WaitForExit()
                if ($proc.ExitCode -ne 0) { Remove-Item -LiteralPath $path -Force; throw 'ADB screenshot capture failed.' }
                Write-Host "Saved device screenshot: $path (HydroNudge foreground confirmed)"
            }
            'crashlog' {
                New-Item -ItemType Directory -Path $outDir -Force | Out-Null
                $path = Join-Path $outDir ("HydroNudge-crashlog-{0:yyyyMMdd-HHmmss}.txt" -f (Get-Date))
                $lines = & $adb -s $Serial logcat -d -t 10000 -v threadtime | Select-String -Pattern 'Process: app\.hydronudge|app\.hydronudge.*(FATAL|Exception|Error|crash)' -CaseSensitive:$false
                if ($lines) { $lines | ForEach-Object { $_.Line } | Set-Content -LiteralPath $path -Encoding utf8; Write-Host "Saved matching HydroNudge crash lines: $path" }
                else { 'No matching HydroNudge crash lines found in the current logcat buffer.' | Set-Content -LiteralPath $path -Encoding utf8; Write-Host "No matching crash lines. Saved result: $path" }
            }
        }
    }
}
