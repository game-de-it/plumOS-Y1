# Release preparation and distribution basis — 0.10.24

[日本語](../ja/distribution-review.md) | [Release preparation](release.md)

Updated 2026-11-18. This is a local release candidate; release location: [GitHub Releases](https://github.com/game-de-it/plumOS-Y1/releases/tag/v0.10.24); no external inquiry has occurred.


0.10.24 changes only the USB translation and version metadata. Full hardware results below belong to 0.10.23; record the targeted new checks in [current validation](release-validation.md).

## Recorded project decisions

- **Stock-derived firmware:** follow the user's decision to use the [community developer publishing workflow](https://innioasis.app/developers.html) as the publishing basis. Additional individual manufacturer permission is not a mandatory outstanding task. Retain existing component terms/notices; do not describe the whole firmware as MIT.
- **Derivative attribute:** the workflow explicitly contemplates stock-derived custom firmware. The attribute also controls Keep Data family matching, so catalogue registration needs data-compatibility validation. Registration is separate from installation from a file.
- **PLUMOSAMP:** use the creator-provided artwork for this project. Visual reference alone and changed-pixel counts are not rejection/permission tests. Retain only required regions and new peach icons; omit lightning marks, slogan rows, unused sheets, cursors and tutorial text. Do not use the old 0.10.22 artwork-containing release assets for the initial publication.
- **LibVLC:** reuse the unchanged source-built 0.10.22 audio libraries with corresponding sources, patches, licenses and NDK/Android support notices for 75 modules and 18 contribs. See the [native build evidence](libvlc-audio-build.md) and [ELF audit](../licensing/native-audio-binaries.json).

## Final checks

| Area | Status |
| --- | --- |
| Updated firmware flash / cold boot | Final 0.10.23 firmware flash, boot, power-off and restart verified. |
| USB transfer / playback after eject / on-device APK update | Transfer, sharing stop, added-track playback and on-device APK update passed. |
| Controls / click / vibration / Android settings / audio | User physical/listening checks and automated 39 + 4 cases passed. |
| Notices / sources / hashes / privacy | Release hash/source/notice/privacy checks passed; existing component terms remain applicable. |
| Modified-library installation with an independent key | Developer workflow documented; separate-key hardware test not performed. |
| Public links / catalogue | Verify binary and both source downloads when publication is authorized. Catalogue PR is optional. |

Old artwork and Maven-native investigations are historical evidence. Use RELEASE_NOTICES for current component statements; keep validation-process details out of the user-facing README.
