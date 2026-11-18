# Hardware validation and change scope — 0.10.24

[日本語](../ja/release-validation.md)

2026-11-18. Player 0.10.24/versionCode 46 fixes the English USB Back label; Japanese remains unchanged. Release location: [GitHub Releases](https://github.com/game-de-it/plumOS-Y1/releases/tag/v0.10.24).

## Targeted checks

- Installed the signed 0.10.24 APK on Y1 and rendered the actual USB page: English Back and Japanese 戻る passed. Restored the original language. This display check did not activate USB sharing.
- Of 55 Java classfiles, only UiText.class changed. APK changes are limited to DEX and version metadata; audio/USB/wheel code, images and native libraries are retained.
- Verified APK/native replacement dumps and e2fsck for the firmware; retained flashing targets and other partition images.
- Firmware SHA-256: `1acc8ba8e4c88d04ed67eef5a6555887d834866c8fb62864565347cfcd324902`
- APK SHA-256: `8c2d6e19ff49c45f84f12cf31030c91903e52651a5a3a844a7513de6e7ebf0dd`

## Retained hardware evidence

Keep the version/hash-scoped [0.10.23 full validation](validation-v0.10.23.md): 39 format/EQ cases, MP3/M4A on both engines, Bluetooth/speaker, USB transfer/eject/remount, on-device APK install, standalone controls, click/vibration and shutdown/restart passed on 0.10.23. These are not a fresh 0.10.24 firmware flash. On 0.10.24, verified the installed APK display and firmware assembly; the updated firmware has not been reflashed/cold-booted.

## Distribution and limits

Include application/native corresponding sources, notices and checksums. Native libraries and their complete source content are unchanged. Type B/Y2 unsupported; earlier intermittent analog-output pops unresolved. Independent-key modified-library firmware installation is documented, but not separately tested on hardware. Verify actual binary and both source downloads when publishing.
