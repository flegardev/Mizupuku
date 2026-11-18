param([Parameter(Mandatory=$true)][string]$Serial)
# Read-only device inspection. No install, clear, uninstall, signing or secret access.
. "$PSScriptRoot/_common.ps1"
$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
if ((& $adb -s $Serial get-state 2>$null) -ne 'device') { throw 'Selected serial is not an online device.' }
$manufacturer = & $adb -s $Serial shell getprop ro.product.manufacturer
$model = & $adb -s $Serial shell getprop ro.product.model
Write-Host "Selected device: $manufacturer $model"
$paths = @(& $adb -s $Serial shell pm path com.tiniflegar.mizupuku)
$base = $paths | Where-Object { $_ -match '/base.apk\s*$' } | Select-Object -First 1
if (-not $base) { throw 'HydroNudge base APK not found. No device changes made.' }
$remote = ($base -replace '^package:', '').Trim()
$dest = Join-Path $script:ProjectRoot 'qa-reports/closed-test/installed-certificate'
New-Item -ItemType Directory -Force -Path $dest | Out-Null
$apk = Join-Path $dest 'installed-base.apk'
& $adb -s $Serial pull $remote $apk
if ($LASTEXITCODE -ne 0) { throw 'Read-only APK pull failed.' }
$signer = Get-ChildItem -Path (Join-Path $env:ANDROID_HOME 'build-tools') -Filter apksigner.bat -Recurse | Sort-Object FullName -Descending | Select-Object -First 1
if (-not $signer) { throw 'SDK apksigner unavailable.' }
& $signer.FullName verify --verbose --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw 'Installed APK signature verification failed.' }
Write-Host 'Compare this public signer SHA-256 with Play Console > App integrity > App signing key certificate. No app data was modified.'
