# Mizupuku — provisional Kissaten edition

**13 November release preparation:** [current readiness, checks and owner actions](play-launch/RELEASE_READINESS.md). [Public privacy policy](https://hydronudge-privacy.vercel.app/mizupuku/). Production remains blocked; the older verification figures below describe the earlier source checkpoint.

Previously HydroNudge. The existing offline Android application, updated in place. Application IDs, Room schema identities, existing preference/action identifiers and source signing configuration are retained.

**Production status: BLOCKED.** This repository contains source and review materials. No Android/Play release is implied. Branding clearance, owner signing identity, Console/health declarations and broader stability, physical-device, minimum-API and accessibility checks remain open.

## Review

- [Engineering report](KISSATEN_REPORT.md): real changes, file list, exact historical commands, test results, preservation checks and blockers.
- [Store assets and metadata drafts](play-store/), [actual screenshots](verification/GALLERY.html), [contact sheet](verification/contact-sheet.jpg).
- [Verification evidence](verification/evidence/), [brand guide](verification/BRAND_GUIDE.md), [name investigation](verification/research/BRAND_CLEARANCE.md), [runtime limitations](verification/research/RUNTIME_LIMITS.md).
- [Licenses](THIRD_PARTY_NOTICES.md) and original artifact notices in `licenses/`.

The local verification run recorded **42 passing host tests**, **12 passing instrumented tests** and zero lint errors. Lint warnings are retained. All runtime checks used one explicitly gated disposable API 37 emulator. Room row digests and existing preferences survived an original-source APK → transformed APK upgrade under the existing workstation QA certificate; production-certificate compatibility is unverified.

## Build locally

Use the checked-in wrapper, a complete JDK 25 and Android SDK 37. Configure your own local SDK path. Existing source signing paths remain unchanged and may reference credentials absent from a new checkout.

The review-only init script reuses an **already-existing** `~/.android/debug.keystore`; it refuses to create or replace one. To reproduce unsigned local validation from the repository root in PowerShell:

```powershell
.\gradlew.bat --no-daemon --no-configuration-cache --max-workers=1 -I verification/tools/qa.init.gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest :app:bundleRelease -PkissatenUnsignedRelease=true
```

This creates a QA-signed debug APK and an unsigned validation bundle. Do not upload the unsigned AAB or replace an installed personal app with a differently signed build. Owner upload/app-signing identity and an eligible increasing version code must be verified for a real release.

Device tests may modify emulator fixtures. Inspect cleanup behavior and use only a boot-completed disposable emulator; never run potentially data-destructive connected tests on a personal phone. No phone data, owner signing keys or production records were changed during this work.

## Evidence boundaries

The `verification/` folder contains reviewed summaries, successful runner transcripts and genuine fixture-only screenshots. Machine-specific workspace prefixes in copied evidence are replaced by `<workspace>`; test outcomes and binary hashes are unchanged. Raw database exports, private runtime files, source recovery ZIPs, workstation build caches, credentials and generated APK/AAB outputs remain local. The historical build metadata links to exact commands from that run; the portable command above is the clone-oriented equivalent, not a claim that a second run occurred here.

Timer recovery reopens paused for explicit resume. Android can delay reminders; no exact-delivery or exhaustive accessibility guarantee is made. See the retained runtime-failure investigation before treating later emulator passes as a stability certification.

Family repositories: [Mizupuku](https://github.com/flegardev/Mizupuku), [Fumipoka](https://github.com/flegardev/Fumipoka), [Yorulune](https://github.com/flegardev/Yorulune).
