# Mizupuku — Signing Status

13 November 2026.

**SIGNING BLOCKED. Unsigned validation artifact — NOT UPLOADABLE.** No key was created, replaced, reset or committed.

- Package `app.hydronudge`; current code `1` is the existing validation value, **not a certified unused Play version**.
- Historical HydroNudge evidence records code 1 accepted into a draft. Never upload code 1 again merely because its current record is inaccessible. Inspect the correct package's bundle history before selecting an increasing code.
- No matching authorized owner keystore/certificate is verified in the supplied projects or scoped filename searches. The owner reports these source projects have no release keys. For the new Fumipoka/Yorulune packages, confirm the actual Console has no existing signing identity before first-time upload-key setup. Mizupuku retains app.hydronudge, whose historical accepted draft requires preservation/recovery of its established signing path. A new source checkout or brand does not erase package history.
- The existing workstation debug key is QA-only. Bundle-derived APKs signed with it are not owner-signed release candidates.
- Restore the authorized owner key securely and compare its public SHA-256 certificate with the actual Console upload certificate. Keep passwords out of chat/logs/Git.
- If an upload key is lost, use Google's recovery/reset process only with specific authorization; do not replace the app-signing identity.

Sequence: correct listing/package → actual versions/upload certificate → matching authorized keystore → increasing-code signed AAB → certificate/bundle/hash verification → concrete authorized release. [Official signing guidance](https://developer.android.com/studio/publish/app-signing).
