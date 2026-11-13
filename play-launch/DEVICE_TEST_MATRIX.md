# Mizupuku — Device Test Matrix

13 November 2026.

Minimum API **26**, target API **36**, compile API **37**. No physical/OEM results invented.

Fresh API 37 instrumentation and actual picker/timer/upgrade results are linked in TEST_RESULTS.md. New disposable API 24/26/35 AVDs expand coverage where boot/runtime checks pass; absent or explicitly pending records are not PASS. Read `multi-api-results.json`, `hot-config-results.json` and `long-timer.json` under evidence for completed cases.

The previous standard/small/large/tablet-like, themes, 150%-font and offline captures remain bounded evidence where the shown UI is unchanged. Multiple layouts on one emulator are not multiple physical devices.

Earlier Mizupuku hot-resize input ANR and Fumipoka system-server/display watchdog are retained. Root cause is not established. New controlled non-reproduction cannot erase them or assign blame without evidence.

## Owner physical-device checklist

Use a correctly signed update and export first; do not uninstall or clear an existing personal app. Check update/data retention, keyboard and large text, TalkBack/switch navigation, system bars/OLED, CRUD/undo, fictional backup round-trip, widget add/resize/actions/removal, denied notifications, real reboot/Doze/battery behavior and a full focus session. Record actual model/API/version. Never run data-mutating instrumentation on a personal phone.
