# Developer Guide

[日本語](../ja/developer-guide.md) | [Home](../../README.md)

## Architecture

This is a modification of stock 3.1.2 firmware, not an Android build from a complete vendor source tree. The player package is `com.kroot.y1themes` (an Android identifier retained for update compatibility). Java/Android Canvas renders the 480×360 interface; themes are JSON data, not HTML/JavaScript.

| Location | Role |
| --- | --- |
| `src/com/kroot/y1themes/MainActivity.java` | Screens, input, audio, and device settings |
| `Library.java`, `LibraryCache.java` in the same directory | Background discovery and SQLite metadata cache |
| `ApkUpdates.java` in the same directory | Local APK browsing, private staging, and package/signature/version checks |
| `Theme.java`, `BdfFont.java` in the same directory | Theme parsing, typefaces, and bitmap text |
| `DeviceSetupReceiver.java` in the same directory | System-app setup and stock frontend suppression |
| `assets/themes`, `assets/fonts` | Built-in theme definitions and licensed fonts |
| `firmware/keys`, `firmware/branding` | Wheel mapping and boot-branding inputs |
| `tools/` | Font conversion and firmware assembly helpers |

The system app disables `com.innioasis.y1` at boot and app startup. Its APK remains in the vendor image. Keep SystemUI's USB storage activity enabled: disabling it caused a USB-related crash. Idle USB is ADB; explicit disk sharing temporarily switches to mass storage plus ADB and returns to ADB afterwards.

The wheel mapping belongs in the device key-layout files so Android settings works without a host-side input bridge. An APK-only update cannot grant newly introduced system permissions on an older firmware base.

## APK and native build

The baseline is player 0.10.24 (versionCode 46), Y1 Type A, Android API 17. Requirements: a JDK supporting Java 8 source/target, Android SDK platform 34/build-tools 35.0.1, Python 3, curl, unzip, zip and Docker. Set ANDROID_HOME for a non-default SDK.

Maven supplies matching Java bindings. Do not restore its old native .so files into the release; rebuild the audio native profile separately.

```sh
mkdir -p third_party/libvlc/extracted
curl -fL https://repo.maven.apache.org/maven2/org/videolan/android/libvlc-all/3.7.1/libvlc-all-3.7.1.aar -o third_party/libvlc/libvlc-all-3.7.1.aar
echo 'bc1e14d4344c90e4931ad524671024e9605d599d8ca22a894f709e3c0887fb48  third_party/libvlc/libvlc-all-3.7.1.aar' | shasum -a 256 -c -
unzip -o third_party/libvlc/libvlc-all-3.7.1.aar -d third_party/libvlc/extracted
sh tools/libvlc/rebuild.sh
sh build.sh
```

The script verifies NDK r21e. It does not delete an existing container: choose fresh dedicated LIBVLC_BUILD_CONTAINER/LIBVLC_BUILD_VOLUME names when needed. Do not reuse GPL prebuilt contribs. The app build uses the three libraries and Java under audio/extracted and produces dist/Y1ThemePlayer-0.10.24.apk. See the [profile/device evidence](libvlc-audio-build.md) and [native rebuild details](../../third_party/libvlc/REBUILD_AUDIO.md).

## Signing and device installation

Set SIGNING_KEYSTORE, SIGNING_STORE_PASSWORD and optional SIGNING_KEY_PASSWORD/SIGNING_ALIAS through the environment. Otherwise a local development key is generated, which may not update an existing release. Never include a private key/password in sources or archives. The release-config records the signing certificate hash.

```sh
adb devices
adb -s YOUR_DEVICE_SERIAL install -r dist/Y1ThemePlayer-0.10.24.apk
```

Select the device explicitly; do not terminate another task's ADB server. Uninstalling to bypass a different signer can delete app data. A modified firmware/app pair needs one consistent key for its system APK and later updates. Existing-signature/data migration and independently signed hardware installation remain [V4](distribution-review.md).

## Firmware assembly

This is not an Android build from complete vendor source. Obtain the 0.10.24 Type A release ZIP and verify its bytes against release-config as the base. Ordinary users do not need this developer workflow.

```sh
python3 tools/build_audio_firmware.py --base release/v0.10.24/plumOS-Y1-v0.10.24-TypeA.zip
```

The helper replaces the APK and all three system/lib copies together, verifies dumps and e2fsck, and checks non-system partition images unchanged. It zeroes unused blocks. On macOS it requires Homebrew e2fsprogs, Docker and the native-build image above; override the debugfs path with --debugfs. Outputs go to output/release. Validate a modified candidate again on hardware. An older 0.10.21 base requires --base-version 0.10.21. build_wheel_firmware.py/build_bootlogo_firmware.py are historical assembly helpers, not the current standard route.

## Themes and fonts

Built-in IDs are `first`, `third`, `gen4`, `fifth`, `gen6`, `nano5`, `nano6`, `touch`, `winamp`, and `himd`. First/third/fourth-generation themes use Chicago Kare, fifth-generation uses Source Sans 3, and later color themes use TeX Gyre Heros. These are reproductions or visual approximations, not extracted Apple fonts. Hi-MD uses U-M+ bitmap fonts. Unsupported primary-font characters use fallback text rendering.

Start from [example/theme.json](../../example/theme.json). Use UTF-8, `schema: 1`, a unique ID of 1–40 lowercase letters/digits/underscores/hyphens, and a name up to 40 characters. Do not reuse built-in IDs or the retired `chicago` ID. The JSON limit is 64 KiB.

| Field | Meaning |
| --- | --- |
| `name`, `nameEn` | Default and English display names |
| `layout` | `mono`, `mono3`, `classic`, `color`, `split`, `artfull`, `grid`, `touch`, `winamp`, or `himd` |
| `colors` | Background, header gradient, text, muted text, selection gradient/text, divider, and progress colors; see example |
| `fontSize` | 14–26; out-of-range values are clamped |
| `rowHeight` | 32–64; out-of-range values are clamped |

Install JSON with the helper, explicitly selecting the device:

```sh
python3 install-theme.py --serial YOUR_DEVICE_SERIAL example/theme.json
```

Open Themes on Y1 and select the added theme; reopen the app if needed. The helper sends JSON only. For themes with fonts, use `Y1Themes/<your-theme-id>/` at the root of the exported music disk, alongside Music, containing `theme.json`, optional `font.ttf`, and optional `font-cjk.ttf`. Each font must be smaller than 4,000,000 bytes. Copy through USB disk mode, safely eject, end sharing, and reopen the app. For non-system APK development only, `/data/local/tmp/y1-themes` is another search location; the firmware system app does not load that location. The JSON helper does not transfer fonts. Check Japanese, Latin, symbols, long titles, selected rows, playback, settings, and shutdown confirmation on hardware. A new rendering layout or custom font-coverage policy requires Java changes.

## Public source and release files

Application/documentation source and matching native-dependency source are separate archives; supply both with binaries. README is for ordinary users. Test history, paths not repeated on the current version, and publication work belong in [release preparation](release.md) and the [distribution review](distribution-review.md).

```sh
python3 tools/package_release.py
python3 tools/prepare_release.py
```

package_release.py updates existing ZIP explanations, texts and inner checksums without changing partitions; use --firmware for an explicit input. prepare_release.py verifies frozen APK/FW/native-source hashes, certificate and partition hashes in release-config/v0.10.24.json, then creates public/plumOS-Y1 from an allowlist. It refuses to overwrite manual edits or extra files in the old generated tree. Links, private paths, image EXIF and key material are audited. Release notes, manifest and full SHA256SUMS are generated locally; no GitHub publication or external inquiry occurs.

Documentation organization follows the related [plumOS Pixel2](https://github.com/game-de-it/plumOS-pixel2_v2). Distinguish current hardware proof from earlier-version evidence; never mark unperformed tests as passed.
