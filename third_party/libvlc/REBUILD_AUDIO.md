# Audio-focused native rebuild / 音楽用ネイティブ再ビルド

This profile preserves audio demuxers/decoders, the equalizer, software volume, soxr resampling, and Android audio outputs. It excludes DVD/CSS/navigation, Blu-ray, FFmpeg video decoding, encoders, network shares/discovery, Lua, subtitle rendering, and modules carrying GPL metadata. The original Maven binary is retained as a rollback input, not silently relabeled.

音楽のデマルチプレクサー／デコーダー、EQ、ソフトウェア音量、soxr、Android音声出力を維持します。DVD・Blu-ray・動画デコード・エンコーダー・ネットワーク共有／探索・Lua・字幕描画とGPL表記モジュールを除外します。元のMavenバイナリは復旧用として保持し、ライセンスのラベルだけを変更しません。

## Build / ビルド

Use the pinned JNI and VLC archives in `upstream/`, a fresh dedicated Docker volume, and the official Linux x86_64 NDK r21e archive. Verify the NDK hash against the exported `audio/BUILD.json`. `--license a` selects the upstream LGPL-2.1 contrib policy; the explicit audio profile and module filtering impose additional exclusions. No prebuilt contrib package is used.

The relevant scripts are `tools/libvlc/Dockerfile`, `build-in-container.sh`, `configure_audio_build.py`, `filter_modules.py`, and `export_build.py`. The container expects the project mounted at `/workspace` and an isolated build volume at `/work`. Download the NDK archive into `build/libvlc-lgpl/android-ndk-r21e-linux-x86_64.zip` before running the build. Local signing material is never part of the source bundle.

固定JNI/VLCソースと公式NDK r21eを使用し、専用領域でcontribから構築します。`--license a`に加え、音楽用構成と最終モジュールのライセンス確認を行います。元のGPL構成のcontribを再利用しないでください。

## Integration / 組込み

`export_build.py` writes the three ARMv7 shared libraries and unmodified matching Java bindings under `audio/extracted/`. The application build defaults to these inputs. `LIBVLC_DIR` may point at another extracted build for an explicit local comparison. `build_audio_firmware.py` uses a hash-verified release firmware base (0.10.22 by default; the initial native migration used 0.10.21) and replaces both the APK and `/system/lib/{libvlc.so,libvlcjni.so,libc++_shared.so}`. It verifies every replacement and the filesystem; unrelated partition images are checked unchanged.

APKとFW内の共有ライブラリの両方を更新します。ベースFWのホイール・USB・クリック音・ロゴを引き継ぎます。ビルド成功／イメージ検証と、実機での再生・聴取確認は別の検証です。

The source bundle includes pinned upstream archives, contrib source tarballs, upstream patches, local build/profile scripts, configuration evidence and hashes. Applicable license texts and replacement/relink instructions must accompany public distribution. Existing firmware and skin publication questions are separate from this native rebuild.
