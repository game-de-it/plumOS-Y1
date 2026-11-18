# LibVLC 3.7.1

> 0.10.22以降は[音楽用ソースビルド](../../docs/ja/libvlc-audio-build.md)を使用します。以下のMavenネイティブバイナリの説明・GPL調査は0.10.21以前の記録です。

[English](README.md)

VideoLANの未変更の`libvlc-all:3.7.1` AARとARMv7ライブラリを使用します。ビルド前に`classes.jar`と`jni/armeabi-v7a/*.so`を`extracted/`へ展開してください。

- 配布物：https://repo.maven.apache.org/maven2/org/videolan/android/libvlc-all/3.7.1/libvlc-all-3.7.1.aar
- SHA-256：`bc1e14d4344c90e4931ad524671024e9605d599d8ca22a894f709e3c0887fb48`
- POM／バインディングのライセンス表記：LGPL 2.1。[COPYING.LIB](COPYING.LIB)を同梱。**現ネイティブバイナリにGPL実装があり、結合物をLGPL限定とは扱いません。**
- 収集ソースとネイティブ依存物の確認残件：[SOURCES_JA.md](SOURCES_JA.md)。

Android配布物3.7.1のネイティブVLCコアは3.0.24-beta1と表示されます。異なる層のバージョン番号です。
