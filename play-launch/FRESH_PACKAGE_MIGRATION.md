# Mizupuku fresh package migration

The proposed Play package is `com.tiniflegar.mizupuku`. The source previously built as `app.hydronudge`; that name remains the Kotlin/Android resource namespace so Room, DataStore, app-private file names, and existing source imports keep their established identities. The new Play application ID is separate from those internal identifiers.

## Moving personal records

Android treats the two application IDs as separate apps. Mizupuku cannot read the old installation's private database or preferences directly. Before removing or replacing the old installation, open its **Backup & Restore** screen and export a full JSON backup to a location you control. Install the new Mizupuku package, open **Backup & Restore**, select that JSON through the document picker, inspect the preview, and confirm import. Keep the export until the new app's records and settings look correct. The app also retains CSV export for spreadsheet use; CSV is not a full restore format.

The existing full-backup codec remains unchanged: it accepts schema v1 (entries and saved servings) and schema v2 (those records plus settings), validates the file, and merges matching/new identifiers transactionally. Room schema v1, DataStore keys and app behavior remain unchanged. Manual export is the user-controlled bridge; OS alarms, notification permission and launcher widget instances are scoped by Android to an installation and should be reviewed/reconfigured after installing the new package.

## Release gate

Play Console reported this package name available on 13 November 2026. Console app creation remains pending the owner review of the required policy and US export attestations. The live privacy URL still needs owner review and publication of the matching package statement before a release. No claim is made that package creation, signing, closed testing or publication is complete.
