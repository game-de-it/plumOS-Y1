# Release preparation — 0.10.24

[日本語](../ja/release.md) | [Distribution basis](distribution-review.md) | [Final validation](release-validation.md)

2026-11-18. Initial release candidate: 0.10.24/versionCode 46, Y1 Type A only. Preserve 0.10.22 for recovery/comparison. Release location: [GitHub Releases](https://github.com/game-de-it/plumOS-Y1/releases/tag/v0.10.24).

The candidate incorporates required PLUMOSAMP regions and new peach pixels, omitting lightning marks, slogan rows, unused sheets/cursors/tutorial text. Keep theme ID winamp for existing settings; display PLUMOSAMP. Retain audio code, rebuilt LibVLC, wheel/USB fixes, system permissions and boot logo.


This update changes only the English USB Back translation and version metadata. Retain the complete 0.10.23 hardware results and perform a targeted English/Japanese APK rendering check on 0.10.24. The updated 0.10.24 firmware has not been reflashed; do not relabel earlier results.

## Contents

Type A firmware ZIP, same-signer player APK, application source, corresponding LibVLC source, installation-first English/Japanese/combined release bodies, validation status, MIT LICENSE, component notices, manifest and SHA256SUMS. Unchanged native binaries use unchanged corresponding source content.

## Before publication

1. Verify final assets, hashes, privacy, notices and corresponding sources.
2. Retained complete 0.10.23 hardware results; completed [targeted 0.10.24 checks](release-validation.md). The 0.10.24 firmware has not been reflashed.
3. Recorded results against exact versions/hashes; update documentation/manifest/sources while preserving tested firmware/APK bytes.
4. When publication is authorized, create the repository/release and verify public binary and both source downloads.

Use the community custom-firmware publishing workflow as the basis; individual stock-firmware permission confirmation is not an additional mandatory task. Retain applicable notices, licenses and required corresponding source. Document the independent-signing modified-library installation route and the separate-key hardware test not performed.

Keep README focused on features, installation, controls and theme pictures. Developer documents hold internal validation status. Generate sources from the allowlist; omit keys, private host paths, device logs, music, original photos and EXIF.
