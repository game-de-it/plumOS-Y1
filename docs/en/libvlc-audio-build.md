# LibVLC audio rebuild — 0.10.22

> Release-source cleanup: 16 unreferenced stale classes were subsequently removed from the APK; all 55 current classfiles and the native libraries are unchanged. Clean APK assembly matches the published source and a silent native/EQ hardware smoke passed. The original full-format/listening results below remain native-migration evidence. Final clean-APK firmware cold boot remains R1 in [release preparation](release.md).

[日本語](../ja/libvlc-audio-build.md) | [Build profile](../../third_party/libvlc/REBUILD_AUDIO.md)

The player now uses a source-built ARMv7/API 17 LibVLC audio profile, rather than the native binaries from Maven's `libvlc-all:3.7.1`. The matching Java bindings remain unchanged. The JNI and VLC source pins, NDK r21e hash, dependency selection and native hashes are recorded in [BUILD.json](../../third_party/libvlc/audio/BUILD.json).

## Scope and retained behavior

DVD/CSS/navigation, Blu-ray, GPL-labeled VLC modules, FFmpeg GPL/GNUv3/nonfree features, video decoders, subtitles, network share/discovery integrations and Lua are excluded. The final link retains 75 LGPL-labeled modules, including local file access, audio/container demuxers, audio decoders, Android AudioTrack/OpenSL ES, soxr, EQ and NEON software volume. Two small LGPL Android display/window compatibility modules remain because the unmodified Java constructor requires their option definitions; the app still requests `--no-video`.

The 10-band EQ and existing +12 dB compensation for VLC's 0.25 dry-signal multiplier are unchanged. Normal volume and 110/120/130 percent boost are unchanged. The Android Bluetooth stack is unchanged. This work does not establish that the previously reported intermittent analog-output pop is fixed.

## Validation

- The rebuilt native library is approximately 10 MB, versus approximately 40 MB previously. The [ELF inventory](../licensing/native-audio-binaries.json) finds no dvdcss/dvdread/dvdnav/Blu-ray/x264/x265/zvbi implementation definitions in the rebuilt inputs. FFmpeg config records GPL, VERSION3 and NONFREE as zero; GME uses its LGPL Nuked core.
- Both old 0.10.21 and new 0.10.22 passed **39 playback checks**: ALAC/M4A, AAC/ADTS, AIFF, CAF, FLAC, AAC/M4A, MKA, MP3, Ogg/Vorbis, Opus, WAV, WMA and WavPack, each with EQ off, flat, and bass bands. Tests require ongoing playback and advancing timestamps; new-build tests also check native EQ acceptance.
- Four ordinary library playback checks passed: real MP3/M4A tracks through Android playback with EQ off, and LibVLC with EQ on.
- Software volume readback matched 100, 110, 120 and 130. Lowering volume returned boost to zero and the system volume below its maximum.
- A2DP was active during the checks. The user confirmed audible Bluetooth output from both versions, and no large EQ loudness loss or other major anomaly on the new build. These short checks are not an exhaustive decoder or listening-quality certification.
- The installed 0.10.22 APK and native library match the built inputs. Existing app settings, library and Bluetooth pairing are retained by an in-place update.
- The Type A firmware is based on the verified standalone 0.10.21. Both APK and all three `/system/lib` inputs are replaced/verified. Comparing **1,247 regular files** shows only the APK, `libvlc.so`, and `libvlcjni.so` changed; the copied C++ runtime already matches the old one. All other files and non-system partition images match the base. Unallocated image blocks are zeroed and filesystem checks pass.
- The user flashed the Type A firmware and rebooted. The system APK and all three system libraries match the build. A full 39-condition run plus four ordinary-track checks passed on speaker output, which the user heard. After reconnecting through native Android Bluetooth settings, three EQ checks and four ordinary MP3/M4A checks passed with A2DP connected throughout. The user confirmed normal headphone output. In-process library maps and hashes confirm the rebuilt libraries were loaded from `/system/lib`. Stock frontend remains disabled and the default USB mode remains ADB.

## Source and replacement route

The companion `plumOS-Y1-v0.10.22-libvlc-source.tar.gz` includes pinned JNI/VLC sources and patches, the 18 contrib source archives, bootstrap tool sources, local profile/build scripts, notices, checksums and configuration evidence. `tools/libvlc/rebuild.sh` performs a fresh source build without a prebuilt contrib package. Read the build profile before using an existing cache.

`build.sh` defaults to `third_party/libvlc/audio/extracted/`. To replace/relink, rebuild the native inputs, build the app, and use `tools/build_audio_firmware.py` to replace the system APK and three native copies together. A modified firmware/app pair must use one consistent signing identity. A different key cannot update an already installed APK directly; the firmware route provides the modified system app first. The publisher's private key is not supplied. Applicable LGPL/component notices and source access must accompany distribution. Stock firmware and skin-artwork conditions remain separate.
