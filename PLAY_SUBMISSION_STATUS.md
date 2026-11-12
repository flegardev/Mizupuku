# HydroNudge Play closed-test status

**Updated:** 2026-11-11. The existing `app.hydronudge` app, Play App Signing enrollment, and Alpha track are confirmed. No bundle has been uploaded and no test release has been submitted.

## Current release candidate

- Version: 1.0.0 (versionCode 1); optimized local AAB: `app/build/outputs/bundle/release/app-release.aab`.
- Engineering safeguards completed in code:
  - **Complete Backup Safety (Schema v2 + v1 compatibility)**: `BackupCodec.encodeFull` / `decodeFull` exports and restores drink history, saved servings, AND full `HydroSettings` preferences (goals, quick-log amounts, units, themes, reminder schedules) with strict validation, restore preview (`BackupCodec.preview`), duplicate ID protection, and atomic Room transaction persistence while remaining backward-compatible with `schemaVersion: 1` backups.
  - **Reminder & Widget Reliability**: Fixed hardcoded `250/500 mL` filters in `DrinkActionReceiver` and `HydroWidget.WidgetDrinkAction` so custom quick-add amounts work everywhere; added configurable widget quick-log amounts (`widgetQuickAmounts`), 12 widget presets, and non-judgmental missed-reminder feedback (`ReminderPolicy.missedReminderFeedback`) without claiming exact alarm delivery.
  - **Health & Youth/Caregiver Safeguards**: Added configurable accidental large-volume entry confirmation (`accidentalEntryLimitMl`: 1,000 / 1,500 / 2,000 mL), optional goal guidance without medical claims, and caregiver/youth wellness wording in Settings, DrinkEditor, and `privacy-policy.txt`.
- Store copy and current genuine screenshots: `release-assets/listing/` and `release-assets/minimal-update/`. Published policy: https://hydronudge-privacy.vercel.app/.

## Owner and policy gates (External Play Console actions required before upload)

- **Personal Account Health Eligibility**: The account is Personal. A human support question about health-app eligibility was submitted; owner must verify Google Play Support's written response before submitting a release.
- **Target Audience & IARC Questionnaire**: Intended audience includes children, teenagers, and adults, or owner-selected age bands in Play Console. In-app caregiver guidance and accidental-entry confirmation are now implemented in code; owner must complete the IARC content rating questionnaire, Target Audience selection, and Data Safety form in Play Console.
- **Upload Keystore Signing**: The Play upload certificate and matching owner keystore must be configured via `HYDRO_STORE_FILE` / `HYDRO_KEY_ALIAS` environment variables (see `docs/RELEASE_SIGNING_GUIDE.md`) when producing the final owner-signed upload AAB. Never upload a debug-signed or unsigned release.

## Test release state

The last authenticated Console inspection (2026-11-02) recorded no uploaded bundle, no active tester link, and no opted-in testers. The real tester email list and test countries have not been provided. Release notes are in `play-submission/testing/release-notes-en-US.txt`; owner must confirm the feedback contact and countries. No invitation or release submission has occurred.

## Essential records

- Console evidence: `play-submission/CONSOLE_INSPECTION_20261102.md`
- Declaration review: `play-submission/DECLARATIONS_OWNER_REVIEW.md`
- Signing guide: `docs/RELEASE_SIGNING_GUIDE.md`
- Backup format specification: `docs/BACKUP_FORMAT.md`
