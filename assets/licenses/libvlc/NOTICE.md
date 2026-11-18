# LibVLC audio build for plumOS-Y1 / 音楽用LibVLC

The player uses an ARMv7 Android API 17 source build from the pinned VideoLAN JNI/VLC sources. Contribs are built from source with the upstream `--license a` policy and an explicit audio profile. DVD/CSS/navigation, Blu-ray, video decoding, network share/discovery integrations, Lua, and subtitle/video processing modules are excluded (the small LGPL Android display/window compatibility modules remain because the unmodified Java bindings require their option definitions) from the native player. Every retained VLC module must carry the upstream LGPL-2.1 metadata; GPL-labeled modules are filtered before static linking. The standard Android Bluetooth stack is unchanged.

音楽用にARMv7・Android API 17向けにソースから再ビルドしています。DVD/CSS・Blu-ray・動画デコード・ネットワーク共有／探索・Lua・字幕／動画処理モジュールを除外。既存Javaバインディングが起動時に参照する設定項目のため、LGPLのAndroid display/window互換モジュールのみ維持。GPL表記のVLCモジュールは静的リンク前に除外しています。Bluetoothは従来のAndroid音声出力経路を利用します。

Audio codecs/demuxers, Android AudioTrack/OpenSL ES, soxr resampling, the 10-band equalizer, and 100/110/120/130 percent software volume remain available. The application's existing +12 dB equalizer dry-signal compensation is retained. No new automatic attenuation or gain policy is introduced.

音楽のデコード、AudioTrack／OpenSL ES、soxr、10バンドEQ、100／110／120／130％の音量を維持。EQの既存の音量補正も維持し、自動的なゲイン低下は追加していません。

`BUILD.json` identifies source pins, toolchain, dependency selection and library hashes. The companion `plumOS-Y1-v0.10.22-libvlc-source.tar.gz` includes upstream sources, contrib tarballs, patches, local scripts, notices and configuration evidence. Java bindings remain the unmodified matching Maven 3.7.1 classes; their sources are included. See `third_party/libvlc/REBUILD_AUDIO.md` in the application source tree for rebuild and integration details. LGPL-2.1 and component-specific notices apply; the application's own MIT license does not replace them.

`BUILD.json`にソース、NDK、依存物、ハッシュを記録。対応ソースアーカイブにソース・依存物・パッチ・スクリプト・ライセンス表記・構成資料を同梱します。Javaバインディングは対応する3.7.1版を維持し、そのソースも同梱。改造版は利用者の鍵で一貫したFW/APKを構築して導入できます。元のプロジェクトの秘密署名鍵は配布しません。

Validation / 検証: consult the separate v0.10.22 device regression report. Build success and image integrity checks are distinct from physical audio listening and firmware flash verification. Existing stock firmware and Winamp artwork distribution questions remain separate.
