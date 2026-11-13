# Mizupuku — Test Results

13 November 2026.

Final wrapper validation exit: **0**. Host unit/Robolectric tests: **43**; failures/errors: **0**. Lint: `{"debug": {"available": true, "errors": 0, "warnings": 29}, "release": {"available": true, "errors": 0, "warnings": 29}}`.

The initial fresh batch ran clean, assembleDebug, testDebugUnitTest, lintDebug, lintRelease, assembleDebugAndroidTest and bundleRelease. After final contrast fixes, Fumipoka/Yorulune reran all those tasks except clean; unchanged tasks may be up-to-date. Existing QA init and explicit unsigned mode were used. JBR 25, compile SDK 37, target SDK 36. Clean removed generated build output only. [Exact command/fingerprint](evidence/launch-clean-20261113.json), [suite totals](evidence/host-results.json).

Modern emulator instrumentation passed: **True**. [Runner metadata](evidence/instrumentation.json). Original failed tests/logs are retained locally, never silently discarded. Raw private device records are not public evidence.

New isolated host-database stress tests: Mizupuku 5,000 drink rows with reopen/duplicate replay/exact totals; Fumipoka 2,000 checkpoints with project links/reopen/rollback/cascade; Yorulune 5,000 tasks with reopen/rollback plus independent alarms and notification actions for hash-colliding task IDs. Each project runs its relevant test only. These are host/Robolectric SQLite checks, not a claim of physical-device performance.

The controlled hot-configuration and five-minute timer/document checks used the fresh initial launch candidate. Fumipoka/Yorulune then received text-contrast-only UI changes; final-binary navigation/instrumentation, upgrade retention, policy access and release-bundle multi-API checks are recorded separately. Those earlier checks are not relabelled as running on the final APK.

Installation of the Fumipoka instrumentation APK timed out twice on the long-running API 37 AVD before the test runner started. A normal reboot preserved all three Room databases; post-reboot results are recorded separately. The fresh API 35 image also showed an preinstalled Messages ANR dialog during Settings radio restoration. Failed harness/diagnostic records are retained locally. A System UI ANR also interrupted final Yorulune API 37 navigation after reboot. Its diagnostics remain retained and the production stability gate stays BLOCKED. ActivityManager lastanr reported no ANR since boot despite the visible dialog; no stack-level cause is asserted. Later passes do not establish those causes or certify an OEM device.

Existing migration and backup assertions remain. Available-source upgrade checks do not prove every historical production version or certificate. See DEVICE_TEST_MATRIX.md for actual runtime scope and pending coverage. No real tester feedback or 14-day closed-test completion is claimed.
