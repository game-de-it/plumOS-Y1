# LibVLCのソース由来

> 0.10.22以降は[音楽用ソースビルド](../../docs/ja/libvlc-audio-build.md)を使用します。以下のMavenネイティブバイナリの説明・GPL調査は0.10.21以前の記録です。

[English](SOURCES.md)

## バイナリのライセンス調査結果

現libvlc.soにGPL実装のシンボルを確認しました。Maven／バインディングのLGPL表記だけでネイティブ結合物をLGPL限定とは扱えません。[調査結果](../../docs/ja/distribution-review.md)・[バイナリ一覧](../../docs/licensing/native-binaries.json)を参照してください。GPLを満たす配布か、LGPL 2.1限定再ビルドを選び、対応ソース・表記・再リンク資料を整える必要があります。

変更せず使用しているAndroid依存物は、[Maven Central](https://repo.maven.apache.org/maven2/org/videolan/android/libvlc-all/3.7.1/)の`org.videolan.android:libvlc-all:3.7.1`です。POMはソースとしてLibVLC JNIプロジェクト、ライセンスとしてLGPL 2.1を示しています。ARMv7のネイティブライブラリだけを使用します。

`upstream/`に収集したもの：

- MavenのJavaソースJARとPOM。ソースJARは公開SHA-1と照合済み。
- LibVLC JNIの`2fca0d2b5eb1d94e2c41e23408e8087358cb983a`を指定する[VLC Androidのリリース用レシピ](https://github.com/videolan/vlc-android/tree/c05d7a53209d309b862bf07b04896db11bc10fd6)。
- そのコミットのLibVLC JNIアーカイブ。JNIコード、パッチ、ビルド用レシピを含みます。
- JNIレシピが`libvlc/patches`適用前に指定する、VLCベース`4c608da16c33560821be7e5c1ad4e34bf5a65398`のアーカイブ。
- 上流の固定コミット、バイナリ内の表示、アーカイブのハッシュを記録する`SOURCE_PROVENANCE.json`。

実際のネイティブライブラリの表示は`3.0.24-beta1 Vetinari`、リビジョンは`3.0.23-2-650-g95458d4ad1`です。Android配布物の版3.7.1とは区別してください。JNIパッチ適用でもコミットが増えるため、表示リビジョンだけで完全な対応確認済みとは扱いません。

**公開前の残件**：実際のネイティブビルド設定と、静的リンクされた依存物・`libc++_shared.so`を含むライセンス／ソース提供条件を確認します。収集したアーカイブとレシピは準備資料であり、LGPLの配布条件をすべて満たしたとの表明ではありません。[COPYING.LIB](COPYING.LIB)と上流の帰属表記を保持してください。

再ビルド・再リンクでは開発者ガイドに従ってLibVLC AARを準備し、再構築した`classes.jar`とARMv7 JNI入力へ差し替えてアプリをビルドします。別の鍵で署名したAPKは公開版に直接上書きできません。改造端末ではFWとアプリの署名を揃えてください。プロジェクトの秘密署名鍵は配布しません。
