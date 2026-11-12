# Dependency and license inventory

The resolved release inventories contain 92 artifacts for Mizupuku, 84 for Fumipoka, and 83 for Yorulune. Yorulune's count includes its 81 release-runtime-classpath artifacts plus the Java API backport library and D8 backport configuration resolved through coreLibraryDesugaring. A classpath-only inventory would omit that packaged backport code.

Each app has a machine-readable coordinate/version/purpose/license inventory, a dated OSV Maven query summary, THIRD_PARTY_NOTICES.md, full Apache-2.0 text, and original LICENSE/NOTICE texts extracted from distributed cached JAR/AAR artifacts with provenance. The 11 November lookup returned no matches for the queried coordinates. That result does not establish absence of vulnerabilities in the app, native code, platform, build tooling or databases outside OSV's Maven coverage.

Most declared component licenses are Apache-2.0 or BSD-3-Clause. **Yorulune also includes desugar_jdk_libs 2.1.5 under GPL version 2 with the Classpath Exception.** Its original license text is among the extracted artifact notices. The separate desugaring configuration declares BSD-3-Clause. The [upstream license](https://github.com/google/desugar_jdk_libs/blob/master/LICENSE) describes the exception for linking with independent modules; it is not a blanket waiver of the library's own license or source/notice obligations. Complete the version-specific redistribution/source-compliance review before publication. Do not describe the entire inventory as having only permissive licenses.

The small Guava listenablefuture artifact lacks a local POM license entry; Apache-2.0 was verified from [Google Guava's upstream license](https://github.com/google/guava/blob/master/LICENSE). Missing metadata is recorded rather than silently attributed to all unknown artifacts. No other missing POM license entry remained in these inventories.

Unmodified Zen Maru Gothic and Shippori Mincho fonts are distributed under their accompanying SIL OFL notices, bundled in the apps. Download origins and font hashes are in assets/fonts. Original café/launcher/promotional artwork was generated locally from geometry, rather than downloaded from a stock-art collection. Unused older photos/fonts were retired into source recovery storage.

No advertising, subscription/billing, remote AI, cloud database or external analytics SDK surfaced in these release inventories or the inspected runtime source. Profile/tracing dependencies support local Android performance instrumentation; they are not evidence of remote analytics. Fumipoka's inherited unused Moshi dependency remains listed as a removal candidate, not hidden from the inventory.

These are engineering inventory and preparation results. Final redistribution compliance and legal approval remain owner/reviewer release gates.
