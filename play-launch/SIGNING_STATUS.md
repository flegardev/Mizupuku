# Mizupuku — Signing Status

13 November 2026.

**SIGNING BLOCKED. Unsigned validation artifact — NOT UPLOADABLE.** No key was created, replaced, reset or committed.

- Proposed package `com.tiniflegar.mizupuku` reported available; code `1` is a local validation value, not a Console-confirmed upload.
- Historical HydroNudge evidence concerns the separate `app.hydronudge` identity and must not be transferred to this new package. No authorized owner keystore/certificate for the new identity was found; the checked-in project key paths are absent.
- After owner certifications and app-entry creation, verify the package record and use first-time Play App Signing/upload-key setup. The existing workstation debug key remains QA-only.
- The existing workstation debug key is QA-only. Bundle-derived APKs signed with it are not owner-signed release candidates.
- Restore the authorized owner key securely and compare its public SHA-256 certificate with the actual Console upload certificate. Keep passwords out of chat/logs/Git.
- If an upload key is lost, use Google's recovery/reset process only with specific authorization; do not replace the app-signing identity.

Sequence: correct listing/package → actual versions/upload certificate → matching authorized keystore → increasing-code signed AAB → certificate/bundle/hash verification → concrete authorized release. [Official signing guidance](https://developer.android.com/studio/publish/app-signing).
