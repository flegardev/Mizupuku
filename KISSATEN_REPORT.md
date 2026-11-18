> Current launch follow-up (13 November 2026): [release preparation](play-launch/RELEASE_READINESS.md). This report retains the earlier transformation evidence; use play-launch for current artifacts, tests and blockers.

> Repository handoff: source, assets and reviewed QA summaries are prepared for GitHub synchronization on 12 November 2026. The audit below records the preceding local verification run. Its statements about no remote push describe that earlier run. Source publication does not constitute an Android or Play release. Original recovery archives and raw runtime records remain private on the workstation.

# Mizupuku — implementation and verification

Status: **BLOCKED for production**. Source implementation and local QA are reviewable. Owner upload credentials and the published signing identity were not available in these supplied projects; this is not a signed Play release. Branding remains provisional. No publication, repository rename, remote push, key creation/replacement, app uninstall or personal-phone test was performed.

## Real changes

- Rebranded visible application, widget, reminder and launcher identity. Preserved app.hydronudge, the existing Room v1 schema, preference keys, technical action names and legacy backup header.
- Introduced local Zen Maru Gothic/Shippori Mincho typography, original café/glass artwork, adaptive/monochrome/density launcher marks, original store icon and feature graphic, shared semantic color normalization and contrast-adjusted accents.
- Restructured the small-screen dashboard so quick servings are visible immediately. Kept the detailed goal/progress card, history, settings, favorites, backup and reminder capabilities. Kept the original optional-goal default rather than inventing a recommended daily amount.
- Added an explicit review for custom/edit/quick amounts above 1000 mL. This is a usability confirmation, not medical advice; existing widget/tile paths retain their existing direct-entry behavior.
- Made the palette studio scrollable at large font scales, with a fixed, reachable Done control. Applied palette-aware semantics to the native Glance widget.
- Updated the test runtime for the installed complete JBR 25, SDK 37 and Robolectric 4.17. Repaired a test fixture that incorrectly assumed a nonzero default goal, and added meaningful quick-entry, large-amount and palette-studio UI regressions.

The family design system and scope are documented in [BRAND_GUIDE](verification/BRAND_GUIDE.md). Original source recovery snapshots and SHA-256 manifests are in `../kissaten/baseline`. Retired unused assets remain recoverable there. These directories arrived without Git metadata; no history was initialized or replaced.

## Verification results

| Check | Result | Evidence |
|---|---|---|
| Latest host build task batch | Exit 0 | [exact command/log metadata](verification/evidence/font-final.json) |
| Host unit/Robolectric tests | 42 tests, 0 failures/errors | [suite totals](verification/evidence/host-tests.json) |
| Debug Android lint | 0 errors; 29 warnings retained | [individual findings](verification/evidence/lint-summary.json) |
| Device instrumentation | 12 tests; passed=True | [runner result](verification/evidence/instrumentation/OceanFunctionalTest.json) |
| Package, namespace, preference/action identifiers | Preserved | [compatibility audit](verification/evidence/compatibility.json) |
| Release bundle | Built; unsigned=True | [hash, native ELF and signature audit](verification/evidence/artifact-audit.json) |
| Runtime dependency inventory | 92 resolved Maven artifacts | [coordinates, purposes and licenses](verification/evidence/dependency-inventory.json) |
| OSV Maven lookup | 0 returned matches; database coverage is limited | [response summary](verification/evidence/osv-audit.json) |
| Store phone screenshots | 7 genuine selected captures | [provenance](play-store/SCREENSHOT_PROVENANCE.json) |
| process-death | PASS | [JSON](verification/evidence/process-death.json) |
| upgrade-data | PASS | [JSON](verification/evidence/upgrade-data.json) |
| document-flow | PASS | [JSON](verification/evidence/document-flow.json) |

Host suite details:

| Suite | Tests | Failures/errors |
|---|---|---|
| app.hydronudge.AppFlowTest | 1 | 0 |
| app.hydronudge.BackupTest | 6 | 0 |
| app.hydronudge.DailyTotalTest | 2 | 0 |
| app.hydronudge.HistoryReportTest | 1 | 0 |
| app.hydronudge.HydrationTest | 8 | 0 |
| app.hydronudge.HydroNudge2FeaturesTest | 9 | 0 |
| app.hydronudge.KissatenContrastTest | 1 | 0 |
| app.hydronudge.LocalStorageTest | 3 | 0 |
| app.hydronudge.MinimalInterfaceTest | 2 | 0 |
| app.hydronudge.QuickAmountsTest | 4 | 0 |
| app.hydronudge.ReminderPolicyTest | 4 | 0 |
| app.hydronudge.SchedulerTest | 1 | 0 |

Exact latest full host-check wrapper command (Windows):

```powershell
Set-Location -LiteralPath '<workspace>\hydronudge (1)'
& "<workspace>\hydronudge (1)\gradlew.bat" --no-daemon --no-configuration-cache --console plain --max-workers=1 -I <workspace>\kissaten\tools\qa.init.gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest :app:bundleRelease
```

The QA init script reuses this workstation's existing debug certificate and optionally suppresses unavailable release signing for validation only. It never generates or changes a key. Source signing blocks are byte-for-byte unchanged. The Java test runtime is complete Android Studio JBR 25, Robolectric 4.17; compileSdk 37, targetSdk 36 and original minSdk/versionCode are preserved. `--no-configuration-cache`, one worker and nonpersistent daemons avoid the observed build-tool/cache/file-lock failures. Earlier failed build/test logs remain in evidence; only the successful latest batch supports the final build claim.

Device commands from the family workspace:

```powershell
Set-Location -LiteralPath '<workspace>'
python kissaten/tools/device.py install hydronudge
python kissaten/tools/instrument.py hydronudge app.hydronudge.qa.OceanFunctionalTest,app.hydronudge.qa.QuickAmountValidationTest,app.hydronudge.qa.DeviceAccessibilityTest,app.hydronudge.qa.EmulatorBackupFlowTest,app.hydronudge.qa.EmulatorWidgetTest,app.hydronudge.qa.ReminderDeliveryTest
python kissaten/tools/matrix.py hydronudge
python kissaten/tools/runtime_data.py hydronudge
python kissaten/tools/upgrade.py hydronudge
```

Device evidence is bounded to `emulator-5554`, boot-completed `HydroNudge_Pixel10a_API37`, Android API 37. Tests use direct install-r/instrument, not a connected Gradle task that might uninstall the app. Resolution/density/font changes simulate several layouts on this one emulator. They do not establish minimum-API, OEM, physical-tablet or production-certificate behavior. See [device matrix](verification/evidence/device-matrix.json), per-run logs and [gallery](verification/GALLERY.html).

Mizupuku also passed two focused checks on the final header refinement at 150% font: quick-serving visibility and the scrollable theme studio with a fixed Done action. The final matrix refreshed standard/150%-font/offline captures; unchanged normal-font small/large/tablet captures are retained and marked in the JSON.

Runtime license/notice copies and the inventory are packaged in the final APK/AAB assets as well as the source. The [license audit](verification/research/DEPENDENCY_LICENSES.md) distinguishes Yorulune's GPL-with-Classpath-Exception backport library. The [runtime limits](verification/research/RUNTIME_LIMITS.md) retain the system/display failures and investigation. A successful later test is bounded evidence, not a declaration that those causes are solved.

## Data compatibility

- Version 1, identity `6d2a6bf2d81f17a2f411a2fbae918876`: JSON equality.

The original compiled APK and transformed debug APK are round-tripped with install-r, with fixture Room row counts/digests and SQLite integrity checks recorded. This verifies source-version fixture compatibility under the workstation QA certificate. It does not prove an update from every historical store version or the owner's production signing key. Supplied older schema exports are incomplete; available migration fixtures are tested without inventing missing historical artifacts. Raw fixture databases/JSON are in private-runtime evidence and excluded from store assets. The existing Hydro UI fixture temporarily replaces emulator Room rows under snapshot/finally-restore; fixture-only document restores also replace or merge emulator records. No adb clear-data action or production/personal database reset occurred.

## Screenshot review

The original compact dashboard placed quick logging below the first viewport. The revised small-screen capture exposes four serving controls. The theme studio originally lost its Done control at 150% font; its fixed header and scroll area were corrected. The initial black launch capture and older preexisting QA pulls are historical evidence and excluded from the delivery gallery. A hot display-resize input ANR remains recorded; it is not erased by subsequent successful cold launches.

Use the [gallery](verification/GALLERY.html) for standard/small/large/tablet, 150% font, theme, offline restart and secondary-screen captures actually present. Historical prototypes and old pulled screenshots are excluded. Store captures are 1080x1920 RGB PNGs with source hashes and pixel equality after RGB encoding; no UI compositing or feature simulation. Icon: 512x512 RGB; feature graphic: 1024x500 RGB. See [play-store](play-store) and [asset provenance](play-store/ASSET_PROVENANCE.md).

## Privacy, dependencies and security

Merged debug/release manifests were inspected. Release permissions: `android.permission.POST_NOTIFICATIONS`, `android.permission.VIBRATE`, `android.permission.WAKE_LOCK`, `android.permission.ACCESS_NETWORK_STATE`, `android.permission.RECEIVE_BOOT_COMPLETED`, `app.hydronudge.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Internet permission is absent. Automatic backup is disabled and cloud/device-transfer rules exclude local app data. Core persistence remains private Room/DataStore. User-created exports are unencrypted; a document provider selected by the user may store them remotely, as disclosed in the app and privacy draft.

No advertising, billing, login, remote AI, cloud database, external analytics or network-client SDK surfaced in the runtime inventory/source inspection. A redacted source-pattern scan found no private-key/token signatures, destructive Room fallback, mutable pending intents or the checked unbounded document-read pattern. This is a bounded audit, not a security certification. [Manifest components](verification/evidence/manifest-audit.json), [source scan](verification/evidence/source-security-scan.json), [licenses](THIRD_PARTY_NOTICES.md) and [original artifact notices](licenses/notice-provenance.json) remain reviewable. Font SIL OFL notices are bundled; source font files are unmodified.

All included 64-bit native ELF LOAD segments meet the measured 16 KiB alignment check. Complete installed-APK/Play validation is a separate release gate. Zero OSV matches means none returned for the queried Maven coordinates, not that vulnerabilities cannot exist. Lint warnings remain visible, including inherited hardcoded-string/deprecation/update recommendations; no lint baseline was added to hide them.

## Remaining release blockers

- Verify owner upload/app-signing certificates, obtain the existing signing credentials securely, choose an eligible increasing versionCode and build/verify the production-signed artifact. Current AAB is unsigned and must not be uploaded as release-ready.
- Complete Google Play/name/logo similarity and jurisdiction-specific trademark clearance. Exact searches and zero returned Apple Slovenia results are limited evidence. See [brand investigation](verification/research/BRAND_CLEARANCE.md).
- Supply legal owner/support details and a controlled public privacy-policy URL; review real Data safety, audience, content rating and all declarations. Draft placeholders are not submission-ready.
- Verify the owner's actual Play Console state, account eligibility and required testing. No testers, opt-ins, feedback, review approval or production access are claimed. Mizupuku additionally needs the health/wellness declaration and the existing account-eligibility question resolved.
- Repeat display/font stress on a stable platform and physical devices. The Hydro input ANR and later Android system-server/display transition failure remain in the evidence; their root cause is not established by this run. Cold-profile successes do not erase them.
- Expand minimum-API/OEM, TalkBack/switch-access, landscape/foldable, extreme text/locale, long-running timer/process death, periodic reminder/Doze/reboot/exact-alarm permission and full historical production-upgrade coverage. Check every relevant secondary screen/palette/widget action before making an exhaustive claim.
- Complete final legal notice/redistribution review and installed APK/Play native-page-size validation. Runtime artifact metadata and source license inventory are prepared, not a legal certification.

No owner-only approval was requested because no publication, signing-identity change or account mutation was attempted. Technical and owner gates keep the final status **BLOCKED**.

## Changed files

Machine-readable [changed-files.json](verification/evidence/changed-files.json) compares each retained source file against the pre-edit SHA-256 manifest, excluding build caches. Retired files are recoverable from the original snapshot.

### changed

- `.gitignore`
- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/java/app/hydronudge/qa/DeviceAccessibilityTest.kt`
- `app/src/androidTest/java/app/hydronudge/qa/EmulatorWidgetSizesTest.kt`
- `app/src/androidTest/java/app/hydronudge/qa/EmulatorWidgetTest.kt`
- `app/src/androidTest/java/app/hydronudge/qa/OceanFunctionalTest.kt`
- `app/src/androidTest/java/app/hydronudge/qa/QuickAmountValidationTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/app/hydronudge/MainActivity.kt`
- `app/src/main/java/app/hydronudge/data/preferences/HydroPreferences.kt`
- `app/src/main/java/app/hydronudge/notifications/HydroNotifications.kt`
- `app/src/main/java/app/hydronudge/reminders/ReminderPolicy.kt`
- `app/src/main/java/app/hydronudge/ui/HydroApp.kt`
- `app/src/main/java/app/hydronudge/ui/HydroViewModel.kt`
- `app/src/main/java/app/hydronudge/ui/components/OceanComponents.kt`
- `app/src/main/java/app/hydronudge/ui/components/ProgressRing.kt`
- `app/src/main/java/app/hydronudge/ui/dashboard/Dashboard.kt`
- `app/src/main/java/app/hydronudge/ui/drinks/DrinkEditor.kt`
- `app/src/main/java/app/hydronudge/ui/history/HistoryScreen.kt`
- `app/src/main/java/app/hydronudge/ui/settings/LegalDocumentDialog.kt`
- `app/src/main/java/app/hydronudge/ui/settings/NotificationSettings.kt`
- `app/src/main/java/app/hydronudge/ui/settings/SettingsDisclosure.kt`
- `app/src/main/java/app/hydronudge/ui/settings/SettingsScreen.kt`
- `app/src/main/java/app/hydronudge/ui/theme/Theme.kt`
- `app/src/main/java/app/hydronudge/widgets/HydroWidget.kt`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_launcher_monochrome.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/mipmap-hdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp`
- `app/src/main/res/values/strings.xml`
- `gradle.properties`
- `gradle/gradle-daemon-jvm.properties`
- `gradle/libs.versions.toml`
### added

- `KISSATEN_REPORT.md`
- `THIRD_PARTY_NOTICES.md`
- `app/src/main/assets/licenses/ShipporiMincho-OFL.txt`
- `app/src/main/assets/licenses/ZenMaruGothic-OFL.txt`
- `app/src/main/assets/third_party/THIRD_PARTY_NOTICES.md`
- `app/src/main/assets/third_party/dependency-inventory.json`
- `app/src/main/assets/third_party/licenses/Apache-2.0.txt`
- `app/src/main/assets/third_party/licenses/artifact-notices/4b9abebc4338048a7c2dc184e9f800deb349366bdf28eb23c2677a77b4c87726.txt`
- `app/src/main/assets/third_party/licenses/artifact-notices/809fa1ed21450f59827d1e9aec720bbc4b687434fa22283c6cb5dd82a47ab9c0.txt`
- `app/src/main/assets/third_party/licenses/notice-provenance.json`
- `app/src/main/java/app/hydronudge/ui/theme/Kissaten.kt`
- `app/src/main/res/font/shippori_regular.ttf`
- `app/src/main/res/font/zen_maru_bold.ttf`
- `app/src/main/res/font/zen_maru_regular.ttf`
- `app/src/test/java/app/hydronudge/KissatenContrastTest.kt`
- `licenses/Apache-2.0.txt`
- `licenses/artifact-notices/4b9abebc4338048a7c2dc184e9f800deb349366bdf28eb23c2677a77b4c87726.txt`
- `licenses/artifact-notices/809fa1ed21450f59827d1e9aec720bbc4b687434fa22283c6cb5dd82a47ab9c0.txt`
- `licenses/notice-provenance.json`
- `play-store/ASSET_PROVENANCE.md`
- `play-store/SCREENSHOT_PROVENANCE.json`
- `play-store/feature-1024x500.png`
- `play-store/icon-512.png`
- `play-store/icon-source.svg`
- `play-store/listing-en.json`
- `play-store/privacy-policy-draft.md`
- `play-store/release-notes-en.txt`
- `play-store/screenshots/phone/01-light-home.png`
- `play-store/screenshots/phone/02-history.png`
- `play-store/screenshots/phone/03-settings.png`
- `play-store/screenshots/phone/04-custom-entry.png`
- `play-store/screenshots/phone/05-theme-studio.png`
- `play-store/screenshots/phone/06-dark-home.png`
- `play-store/screenshots/phone/07-oled-home.png`
- `play-store/screenshots/phone/README.md`
### retired


