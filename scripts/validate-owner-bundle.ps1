param(
    [Parameter(Mandatory=$true)][string]$BundlePath,
    [Parameter(Mandatory=$true)][string]$ExpectedUploadSha256,
    [Parameter(Mandatory=$true)][string]$BundletoolPath
)
# Read-only preflight; public fingerprint only. Never signs or uploads.
. "$PSScriptRoot/_common.ps1"
$bundle = (Resolve-Path -LiteralPath $BundlePath).Path
$tool = (Resolve-Path -LiteralPath $BundletoolPath).Path
$expected = ($ExpectedUploadSha256 -replace '[:\s]','').ToLowerInvariant()
if ($expected -notmatch '^[a-f0-9]{64}$') { throw 'Expected upload SHA-256 must be a public 64-digit certificate fingerprint.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($bundle)
try {
    if (-not ($zip.Entries | Where-Object { $_.FullName -match '^META-INF/[^/]+\.(RSA|DSA|EC)$' })) { throw 'BLOCKED: bundle is unsigned.' }
    $entry = $zip.GetEntry('base/assets/privacy-policy.txt')
    if (-not $entry) { throw 'Privacy policy asset missing.' }
    $reader = [IO.StreamReader]::new($entry.Open())
    try { $policy = $reader.ReadToEnd() } finally { $reader.Dispose() }
    if ($policy -match 'OWNER ACTION|INTERNAL RELEASE DRAFT') { throw 'BLOCKED: finalize policy and rebuild before owner upload.' }
} finally { $zip.Dispose() }
$cert = & "$env:JAVA_HOME/bin/keytool.exe" '-J-Duser.language=en' -printcert -jarfile $bundle 2>&1 | Out-String
$actual = [regex]::Matches($cert,'SHA256:\s*([A-Fa-f0-9:]+)') | ForEach-Object {($_.Groups[1].Value -replace ':','').ToLowerInvariant()} | Select-Object -Unique
if (@($actual).Count -ne 1 -or $actual -ne $expected) { throw 'BLOCKED: upload certificate does not match the owner-verified expected fingerprint.' }
if ($actual -eq 'de560ba9d0e82ec344baf368d1e4c9a9ec0cf1a996c48bc92c121fb9cf60b1f1' -or $cert -match 'CN=Android Debug') { throw 'BLOCKED: debug certificate is not an upload identity.' }
& "$env:JAVA_HOME/bin/jarsigner.exe" -verify $bundle
if ($LASTEXITCODE -ne 0) { throw 'JAR signature verification failed.' }
& "$env:JAVA_HOME/bin/java.exe" -jar $tool validate "--bundle=$bundle"
if ($LASTEXITCODE -ne 0) { throw 'Bundletool validation failed.' }
$manifest = & "$env:JAVA_HOME/bin/java.exe" -jar $tool dump manifest "--bundle=$bundle" --module=base 2>$null
if ($LASTEXITCODE -ne 0) { throw 'Manifest extraction failed.' }
[xml]$xml = $manifest -join "`n"
if ($xml.manifest.package -ne 'com.tiniflegar.mizupuku') { throw 'Unexpected package identity.' }
if ($manifest -match 'android.permission.INTERNET|com.android.vending.BILLING|permission.AD_ID|android:debuggable="true"|<instrumentation') { throw 'Unexpected release manifest content; investigate.' }
Get-FileHash -LiteralPath $bundle -Algorithm SHA256
Write-Host 'PASS local preflight. Owner must still verify versionCode history, current policy/account eligibility, Console certificates and review the actual Console release before authorizing submission.'
