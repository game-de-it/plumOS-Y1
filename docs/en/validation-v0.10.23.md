# Final hardware validation — 0.10.23

[日本語](../ja/validation-v0.10.23.md)

2026-11-18. **Primary final checks passed on Y1 Type A.** Stock 3.1.2/Android 4.2.2, player 0.10.23/versionCode 45. Nothing has been published to GitHub.

## Tested artifacts

- Firmware SHA-256: `714bd22f2fdb3b09c08ed2ec5585a3bcbb69e0896080a163789a9891c3f1a146`
- APK SHA-256: `3b32275d067491764ba2ae42476f51843648883ca95e8fb84be8e73310d14ca2`

The system APK after flashing and the active APK after the final shutdown/restart matched the release. Neither firmware nor APK was changed during final validation.

## Results

| Area | Result |
| --- | --- |
| Boot/version/stock frontend | User confirmed logo/player. System APK version/hash matched; stock frontend disabled. |
| Formats/EQ | 13 formats x EQ off/flat/bass: 39/39. Ordinary MP3/M4A through Android/LibVLC: 4/4. Automated playback validation used volume zero. |
| Native libraries | All three in-process library hashes matched the audio source build; confirmed actual loading from system/lib after flashing. |
| Volume/boost | 100/110/120/130% readbacks and boost removal passed. Playback was paused before high system-volume readbacks; original volume restored. |
| Bluetooth/speaker | A2DP stayed connected during automated checks. User heard headphones and, with headphones off, speaker playback. |
| Physical controls | User confirmed wheel/Center/ring and standalone Android Settings navigation. |
| Click/vibration | User confirmed both follow OFF/ON. |
| USB disk | Japanese-name MP3/M4A and the update APK copied through the actual Mac-mounted disk; readback SHA256 matched. Safe computer eject, sharing stop and Android remount passed. |
| Added tracks/cache | Library grew 809 to 811; added tracks played on both engines. Removing temporary music and generated metadata restored 809. |
| On-device APK update | Settings file picker -> same-version confirmation -> install -> automatic restart passed. Key events drove the device UI rather than substituting adb install. Verified updated APK hash, theme, click/vibration and library retention. |
| Post-update playback | Native EQ 3/3 and newly USB-transferred MP3/M4A Android/LibVLC 4/4 passed. |
| USB cable reconnect | User confirmed normal operation with no stock frontend/storage prompt/System UI error. |
| Shutdown/restart | User confirmed cancellation, real power-off and Center-held restart. After reconnect both saved/live USB configuration were adb; storage mounted. |

After the final restart Android selected the byte-identical system APK and removed the duplicate data APK; settings were retained. This is not a cross-version on-device upgrade test.

The first automated run had a state-read error and was not counted as passed. A focused probe and full 39-case rerun passed without changing the app.

## Assembly/distribution checks

Fresh public-source Java/APK assembly matched all 87 non-signature APK entries using verified native inputs; this was not a second full native rebuild. Of 1311 system files, only the app APK changed; other partition images matched. Verified 116 native source-manifest SHA256 entries, 262 public-source inputs, local links, 33 EXIF-free images, and exclusion of private paths, keys, music and device logs.

## Scope and known limits

Type B/Y2 unsupported. This is not a fresh test of every codec variant, headset or theme/language combination. Checked PLUMOSAMP Home/list/player and the listed paths; earlier evidence for other themes is retained. Intermittent analog-output pops remain unresolved. The English USB screen's secondary Back row still uses the Japanese label 戻る; navigation works.

The independently signed modified-library firmware/app installation workflow is documented; separate-key hardware installation has not been tested. When publishing, supply binaries, both corresponding source archives, notices and SHA256SUMS together and verify public downloads.
