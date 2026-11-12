# Drink and full backup schemas (v1 & v2)

Export/import use Android's document picker. No storage permission is requested. The user chooses the destination/provider, which may store files in the cloud independently of HydroNudge.

JSON root fields:

- `format`: `HydroNudge`
- `schemaVersion`: integer `2` (full backup including preferences) or `1` (legacy drink and favorite records only)
- `exportedAt`: ISO 8601 UTC string, informational
- `entries`: array containing `id`, `name`, `category` (enum name), `amountMl` (positive integer), `timestamp` (UTC epoch milliseconds), `note`, `enteredUnit` (`ML` or `US_OZ`)
- `favorites`: array containing `id`, `name`, `category`, `amountMl`
- `settings` (required when `schemaVersion` is `2`): object containing validated user preferences (`goalMl`, `unit`, `reminders`, `intervalMinutes`, `startMinute`, `endMinute`, `smartStop`, `sound`, `vibration`, `theme`, `quickAmounts`, `palette`, `vesselStyle`, `widgetPreset`, `widgetStyle`, `widgetQuickAmounts`, `activeDaysCsv`, `weekendStartMinute`, `weekendEndMinute`, `quietHoursEnabled`, `quietStartMinute`, `quietEndMinute`, `adaptiveReminders`, `customReminderMessage`, `dayStartMinute`, `quantityRoundingMl`, `accidentalEntryLimitMl`, and display toggles).

Volumes are canonical integer mL. US fluid ounces use 29.5735295625 mL per ounce and round once when entered. Prepared drinks are counted once. Categories do not discount recorded volume. Timestamps represent instants; local day assignment uses the current device zone and configured `dayStartMinute`.

Import validates the whole file (including every setting field when `schemaVersion == 2`), then merges drinks and favorites in a single Room transaction (`database.withTransaction`) and restores preferences atomically when present. Existing IDs are retained unchanged. Importing the same backup again does not duplicate records. Unsupported schemas, malformed values, out-of-range settings, and duplicate IDs within a file are rejected. Files over 32 MiB are rejected to avoid exhausting device memory during JSON parsing; the error leaves local data unchanged. Legacy `schemaVersion: 1` backups remain fully supported and leave existing device preferences unchanged.

The app database is schema version 1 with exported schema under `app/schemas`. Future schema changes must include explicit, tested Room migrations. Destructive migration fallbacks are intentionally absent.

Optional CSV export (`BackupCodec.encodeCsv`) writes an RFC 4180 CSV file of recorded drink entries for personal spreadsheet analysis. Optional restore preview (`BackupCodec.preview`) inspects a JSON backup file before merging to display total entries, new vs existing IDs, saved favorites, and whether preferences are included (`includesPreferences`) without modifying local storage.
