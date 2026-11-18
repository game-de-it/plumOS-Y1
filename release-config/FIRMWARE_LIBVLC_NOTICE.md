# LibVLC source and notices / LibVLCのソースと表記 — 0.10.24

The player uses a source-built ARMv7/API17 LibVLC audio profile with matching VideoLAN Java bindings 3.7.1. DVD/CSS/navigation, Blu-ray, GPL-labeled VLC modules, FFmpeg GPL/GNUv3/nonfree features and video decoders are excluded. Audio decoders/containers, Android AudioTrack/OpenSL ES, soxr, EQ and software volume remain. Two LGPL Android display/window compatibility modules supply option definitions required by the Java constructor; the player still runs with --no-video.

The JNI and VLC source pins, NDK hash and native hashes are recorded in NATIVE_BUILD.json. Publish both plumOS-Y1-v0.10.24-source.tar.gz and plumOS-Y1-v0.10.24-libvlc-source.tar.gz with the binaries. The native archive includes matching core/JNI/Java sources, patches, 18 contrib archives, bootstrap sources, local recipes, notices and configuration evidence. See the application source's developer guide for rebuilding/relinking and integrating all three system-library copies. Modified firmware/app pairs use a consistent signing identity; the publisher's private key is not supplied. A separately signed installation route still awaits independent hardware validation.

LGPL-2.1 and component-specific terms apply; the C++ runtime has Apache 2.0 with LLVM Exceptions. The application APK contains dependency notices. GPL texts also cover source/build-tool or separate stock components; their presence does not label the whole firmware GPL or the whole firmware MIT. See RELEASE_NOTICES.md. Exact public source availability must be maintained when distributing binaries.

音楽用のARMv7／API17 LibVLCをソースから構築し、対応するJava 3.7.1を使用します。DVD/CSS・Blu-ray・GPL表記VLCモジュール・FFmpegのGPL／GNUv3／nonfree・動画デコードを除外。音楽形式、AudioTrack／OpenSL ES、soxr、EQ、音量処理を維持しています。Java起動時の設定項目のためLGPLのAndroid display/window互換モジュール2本は残し、--no-videoで動作します。

NATIVE_BUILD.jsonに固定ソース、NDKとライブラリのハッシュを記録。配布時はplumOS-Y1-v0.10.24-source.tar.gzとplumOS-Y1-v0.10.24-libvlc-source.tar.gzの両方を提供してください。対応するソース、パッチ、18依存物、構築用ソース、レシピ、表記、構成を収録しています。再ビルド・再リンクとsystem側3本の組込みはアプリソースの開発者ガイドを参照。自身の改造FW／APKには一貫した署名鍵を使い、元の秘密鍵は配布しません。別の署名による実機導入経路の追加確認は残っています。

LGPL 2.1と依存物固有の条件、C++にはApache 2.0＋LLVM Exceptionsが適用されます。APK内にも依存物表記を収録。ソース・構築用ツールや別の純正部品のためGPL本文が含まれても、FW全体がGPLやMITという意味ではありません。RELEASE_NOTICES.mdを参照し、公開時は対応ソースへの実アクセスを維持してください。
