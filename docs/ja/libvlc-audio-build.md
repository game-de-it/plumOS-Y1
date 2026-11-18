# LibVLC音楽用再ビルド — 0.10.22

> 公開ソース検査後、APKから未参照の古い16クラスを除去しました。現行55クラスとネイティブは不変。クリーンAPKのソース組立一致と無音ネイティブ／EQ実機スモークが成功。下記の形式・試聴結果は元のネイティブ移行版の証跡です。最終クリーンAPK版FWの起動は[公開前R1](release.md)に残しています。

[English](../en/libvlc-audio-build.md) | [ビルド構成](../../third_party/libvlc/REBUILD_AUDIO.md)

Mavenの`libvlc-all:3.7.1`に含まれるネイティブバイナリを、ARMv7／API 17向けの音楽用ソースビルドへ変更しました。対応するJavaバインディングは維持しています。[BUILD.json](../../third_party/libvlc/audio/BUILD.json)にJNI／VLCの固定ソース、NDK r21eのハッシュ、依存物と各ライブラリのハッシュを記録しています。

## 変更範囲と維持した機能

DVD/CSS・DVDナビゲーション、Blu-ray、GPL表記のVLCモジュール、FFmpegのGPL／GNUv3／nonfree機能、動画デコード、字幕、ネットワーク共有／探索、Luaを除外。最終リンクにはLGPL表記の75モジュールを残し、ローカルファイル、音楽／コンテナの読み取り、音楽デコード、AudioTrack／OpenSL ES、soxr、EQ、NEON音量処理を維持しています。既存Javaコンストラクターが起動時に参照する設定項目のため、小さなLGPLのAndroid display/window互換モジュール2本は維持。アプリは従来どおり`--no-video`を指定します。

10バンドEQとVLCの0.25倍のドライ信号に対する既存の+12 dB補正、通常音量と110／120／130％ブーストを維持。AndroidのBluetoothスタックも変更していません。以前報告されたアナログ出力の断続的ノイズが直ったとする検証ではありません。

## 検証結果

- ネイティブ本体は約40MBから約10MBへ縮小。[ELF調査](../licensing/native-audio-binaries.json)でdvdcss／dvdread／dvdnav／Blu-ray／x264／x265／zvbi実装の定義を検出していません。FFmpegのGPL・VERSION3・NONFREEはすべて0、GMEはLGPLのNukedコアです。
- 旧0.10.21と新0.10.22の両方で、**13形式×EQ 3条件＝39条件**が成功。対象はALAC/M4A、AAC/ADTS、AIFF、CAF、FLAC、AAC/M4A、MKA、MP3、Ogg/Vorbis、Opus、WAV、WMA、WavPack。EQオフ・フラット・低音設定で再生継続と再生位置の進行を確認し、新版ではネイティブEQの受付結果も確認しました。
- 実際のライブラリ内のMP3／M4Aについて、EQオフのAndroid再生と、EQオンのLibVLC再生の計4条件も成功。
- ソフトウェア音量の読戻しが100／110／120／130と一致。音量を下げるとブースト0に戻り、システム音量も最大値から下がることを確認。
- 試験中はA2DPが有効。ユーザーが新旧両方のBluetooth実音を確認し、新版はEQによる大きな音量低下・大きな異常なしとの回答。短時間の試験であり、全形式・全音源・音質の網羅的保証ではありません。
- 実機の0.10.22 APK／ネイティブライブラリとビルド入力の一致を確認。上書き更新によりアプリ設定・ライブラリ・BTペアリングを維持。
- FWは検証済み0.10.21を基に、APKと`/system/lib`の3本を同時に置換・照合。**通常ファイル1,247個**の比較で変更はAPK、libvlc.so、libvlcjni.soだけで、C++ランタイムは従来と同一です。他のファイルとsystem以外のイメージは元と一致。未使用ブロックをゼロ化し、ファイルシステム検査も成功。
- ユーザーがType A FWを書き込み、再起動済み。system APKと共有ライブラリ3本のハッシュ一致を確認。スピーカーで39条件と通常曲4条件が成功し、ユーザーも実音を確認。AndroidのBluetooth設定から再接続後、A2DP接続を維持したEQ 3条件と通常MP3/M4A 4条件も成功し、ヘッドホンの正常な実音を確認しました。プロセス自身のメモリマップとハッシュで`/system/lib`の新ライブラリを実ロードしていることを確認。純正FE無効化・通常USBのADB設定も維持しています。

## 対応ソースと差し替え

`plumOS-Y1-v0.10.22-libvlc-source.tar.gz`に固定JNI／VLCソースとパッチ、18依存物のソース、ブートストラップ用ツールのソース、独自構成・ビルドスクリプト、表記、チェックサム、構成資料を同梱。`tools/libvlc/rebuild.sh`は既成contribを使わずソースから構築します。既存キャッシュを使う前にビルド構成を確認してください。

`build.sh`は`third_party/libvlc/audio/extracted/`を使用します。改造版はネイティブを再構築し、APKをビルドした上で、`tools/build_audio_firmware.py`でsystem APKと共有ライブラリ3本をまとめて置換してください。FW／APKには一貫した署名鍵を使います。別の鍵で既存APKへ直接上書きはできないため、まずFW側へ自身の鍵で署名したアプリを入れます。元の配布者の秘密鍵は含みません。配布時には適用されるLGPL等の表記とソースへのアクセスを提供します。純正FW・スキン画像の配布条件は別の確認事項です。
