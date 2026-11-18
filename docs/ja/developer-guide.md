# 開発者ガイド

[English](../en/developer-guide.md) | [トップ](../../README_JA.md)

## 構成

純正3.1.2 FWを加工する方式です。完全なベンダーソースからAndroidを新規ビルドするものではありません。アプリIDは更新互換性のため`com.kroot.y1themes`を維持しています。480×360画面をJava／Android Canvasで描画し、テーマはHTML／JavaScriptではなくJSONで定義します。

| 場所 | 役割 |
| --- | --- |
| `src/com/kroot/y1themes/MainActivity.java` | 画面・入力・再生・端末設定 |
| 同ディレクトリの`Library.java`・`LibraryCache.java` | バックグラウンド検索とSQLiteキャッシュ |
| 同ディレクトリの`ApkUpdates.java` | APK選択・内部領域へのコピー・アプリID／署名／バージョン確認 |
| 同ディレクトリの`Theme.java`・`BdfFont.java` | テーマ読込、フォント、ビットマップ文字 |
| 同ディレクトリの`DeviceSetupReceiver.java` | システム設定と純正FE抑止 |
| `assets/themes`・`assets/fonts` | テーマ定義とライセンス付きフォント |
| `firmware/keys`・`firmware/branding` | ホイール対応と起動画像 |
| `tools/` | フォント変換・FW組立ツール |

システムアプリが起動時とアプリ開始時に`com.innioasis.y1`を無効化します。純正APK自体は残します。SystemUIのUSBストレージActivityは有効に保ってください。無効化するとUSB関連のクラッシュが発生しました。通常USBはADBのみ、明示的なディスク共有中だけストレージ＋ADBにし、終了後にADBへ戻します。

ホイール対応は端末のキーレイアウトに組み込むことで、PC経由の入力変換なしでAndroid設定を操作できます。古いFWにAPKだけを更新しても、後から追加したシステム権限は付与できません。

## APKとネイティブのビルド

基準はアプリ0.10.24（versionCode 46）、Y1 Type A、Android API 17です。Java 8ソース指定に対応するJDK、Android SDK platform 34／build-tools 35.0.1、Python 3、curl、unzip、zipとDockerが必要です。ANDROID_HOMEでSDKを指定できます。

Maven AARはJavaバインディング入力です。付属の旧.soを公開版へ戻さず、音楽用ネイティブを別途構築します。

```sh
mkdir -p third_party/libvlc/extracted
curl -fL https://repo.maven.apache.org/maven2/org/videolan/android/libvlc-all/3.7.1/libvlc-all-3.7.1.aar -o third_party/libvlc/libvlc-all-3.7.1.aar
echo 'bc1e14d4344c90e4931ad524671024e9605d599d8ca22a894f709e3c0887fb48  third_party/libvlc/libvlc-all-3.7.1.aar' | shasum -a 256 -c -
unzip -o third_party/libvlc/libvlc-all-3.7.1.aar -d third_party/libvlc/extracted
sh tools/libvlc/rebuild.sh
sh build.sh
```

NDK r21eのハッシュはスクリプトで検証します。既存のビルドコンテナがある場合は自動で削除せず停止するため、必要ならLIBVLC_BUILD_CONTAINERとLIBVLC_BUILD_VOLUMEで新しい専用名を指定します。元のGPL構成のcontribは流用しません。生成されたaudio/extractedの3本とJavaを使い、dist/Y1ThemePlayer-0.10.24.apkを構築します。[構成・実機証跡](libvlc-audio-build.md)と[詳細手順](../../third_party/libvlc/REBUILD_AUDIO.md)を参照してください。

## 署名と端末への更新

SIGNING_KEYSTORE、SIGNING_STORE_PASSWORD、任意のSIGNING_KEY_PASSWORD・SIGNING_ALIASを環境変数で指定します。省略時はローカル開発鍵を生成するため、既存配布版を上書きできるとは限りません。秘密鍵やパスワードをソース・アーカイブに含めないでください。配布証明書のハッシュはrelease-configに記録しています。

```sh
adb devices
adb -s YOUR_DEVICE_SERIAL install -r dist/Y1ThemePlayer-0.10.24.apk
```

対象端末は明示し、別作業のADBサーバーを停止しないでください。署名違いを回避するためのアンインストールはデータ消失につながります。独自鍵の改造版は、FW側のsystem APKとその後の更新APKで鍵を統一します。既存署名／データとの移行制約を含めた別署名の実機試験は[残件V4](distribution-review.md)で管理しています。

## FW組立

完全なベンダーAndroidソースからのビルドではありません。配布する0.10.24 Type A ZIPを取得し、そのバイトをrelease-configのハッシュと照合してベースにします。通常の利用者にはこの工程は不要です。

```sh
python3 tools/build_audio_firmware.py --base release/v0.10.24/plumOS-Y1-v0.10.24-TypeA.zip
```

APKとsystem/libの3本を同時に更新し、e2fsck・ダンプ照合・非systemパーティション一致を確認。未使用ブロックもゼロ化します。macOSではHomebrew e2fsprogs、Dockerと上記のネイティブ用イメージが必要です。debugfsのパスは--debugfsで指定できます。出力先はoutputとreleaseで、変更した候補は再び実機確認してください。旧0.10.21ベースを使う場合は--base-version 0.10.21を指定します。build_wheel_firmware.py／build_bootlogo_firmware.pyは過去の組立用で、現在の標準経路ではありません。

## テーマとフォント

内蔵IDは`first`・`third`・`gen4`・`fifth`・`gen6`・`nano5`・`nano6`・`touch`・`winamp`・`himd`です。初代／第3／第4世代にはChicago Kare、第5世代にはSource Sans 3、それ以降のカラーテーマにはTeX Gyre Herosを使用します。Appleから抽出したフォントではなく再現・近似です。Hi-MDはU-M+ビットマップフォントです。主フォントにない文字はフォールバックで表示します。

[example/theme.json](../../example/theme.json)を基に、UTF-8、`schema: 1`で作成します。IDは半角小文字・数字・`_`・`-`による1〜40文字、名前は40文字以内です。内蔵IDと廃止済み`chicago`は使わないでください。JSONは64 KiB以内です。

| 項目 | 意味 |
| --- | --- |
| `name`・`nameEn` | 通常名・英語名 |
| `layout` | `mono`・`mono3`・`classic`・`color`・`split`・`artfull`・`grid`・`touch`・`winamp`・`himd` |
| `colors` | 背景・ヘッダーのグラデーション・文字・補助文字・選択色と文字・区切り・進捗色。例を参照 |
| `fontSize` | 14〜26。範囲外は補正 |
| `rowHeight` | 32〜64。範囲外は補正 |

端末を指定してJSONを追加します。

```sh
python3 install-theme.py --serial YOUR_DEVICE_SERIAL example/theme.json
```

Y1のテーマ一覧から選びます。必要に応じてアプリを開き直してください。この補助ツールが転送するのはJSONだけです。フォント付きテーマは、音楽ディスク直下のMusicと並ぶ`Y1Themes/<テーマID>/`に`theme.json`と任意の`font.ttf`・`font-cjk.ttf`を置けます。各フォントは4,000,000バイト未満にします。USBストレージでコピーし、安全に取り出して共有終了後にアプリを開き直してください。非システムAPKでの開発時だけは、本体側の`/data/local/tmp/y1-themes`も検索対象です。FW組み込みのシステムアプリはその場所を読みません。JSON追加ツールはフォントを転送しません。日本語・英数字・記号・長いタイトル・選択行・再生画面・設定・シャットダウン確認を実機で確認してください。新しい描画レイアウトや独自の文字カバレッジ制御にはJava側の変更が必要です。

## 公開用ソースと配布ファイル

アプリ・文書のソースと、ネイティブ依存物の対応ソースは別アーカイブです。バイナリ配布時は両方を提供します。READMEは一般ユーザー向けに限定し、試験履歴・未再試験経路・公開条件の作業状況は[リリース準備](release.md)・[配布条件整理](distribution-review.md)へ置きます。

```sh
python3 tools/package_release.py
python3 tools/prepare_release.py
```

package_release.pyは既存FWの説明・本文・内部ハッシュを更新します。パーティションを変えません。入力を明示する場合は--firmwareを使用。prepare_release.pyはrelease-config/v0.10.24.jsonの固定済みAPK／FW／ネイティブソース、証明書とパーティションハッシュを検証し、許可リストからpublic/plumOS-Y1を生成します。既存生成ツリーへの手修正・追加ファイルがあれば上書きしません。リンク・個人パス・画像EXIF・鍵等を検査し、リリースノート、全体マニフェストとSHA256SUMSを生成します。GitHub公開や問い合わせは行いません。

現在の文書整理は関連する[plumOS Pixel2](https://github.com/game-de-it/plumOS-pixel2_v2)を参考にしています。実機で通った項目と旧版から引き継ぐ証跡を区別し、未実施を合格扱いにしません。
