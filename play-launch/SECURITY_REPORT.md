# Mizupuku — Security Report

13 November 2026.

Bounded engineering audit, not a security certification. Review the merged RELEASE manifest: Internet permission absent, automatic backup disabled, components/URI grants, immutable PendingIntents and optional notifications narrowly scoped.

Fresh 13 November OSV query returned no matches for the unchanged resolved Maven runtime inventory. Coverage excludes vulnerabilities outside that database, platform and unanalysed code. [Query](evidence/osv-audit.json).

Official bundletool validates the AAB. Its QA-only universal APK is checked with SDK zipalign `-P 16 -c -v 4`, alongside native ELF LOAD alignment inspection. Alignment is not by itself a 16 KiB runtime test. Separate measured-16-KiB API 35 smoke results are included in the device matrix. [Bundle evidence](evidence/bundle-validation.json), DEVICE_TEST_MATRIX.md.

Room/DataStore and recovery snapshots stay local. Imports remain byte/depth bounded and transaction-protected; exports are plaintext and provider destinations are user-controlled. Backup exclusions do not promise control over every OS/provider transfer. No added encryption invented.

SIL OFL font notices and original runtime notices remain bundled. Yorulune includes desugar_jdk_libs GPL v2 with the Classpath Exception; final redistribution/source-compliance review is required. Do not describe all dependencies as permissive. [Notices](../THIRD_PARTY_NOTICES.md).

Keys/passwords, account transaction IDs, raw private device records, tester identities and caches stay out of Git. The previously retired unused Orbit Google configuration is not restored; provider-side assessment/rotation requires owner authorization if exposure is established.

Yorulune's confirmed alarm/notification collision has before-failure and after-pass regression evidence. Task-specific data URIs/tags preserve independence, with legacy alarms retired on reschedule. Technical action strings and Room identities are unchanged.
