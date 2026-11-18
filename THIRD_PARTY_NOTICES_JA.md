# 第三者コンポーネントの表記


[English](THIRD_PARTY_NOTICES.md) | [トップ](README_JA.md)

現在の開発ツリーの構成を示します。FW全体の再配布許諾を示すものではありません。独自コードと文書は[MITライセンス](LICENSE)です。

| コンポーネント | 配布元と条件 | 同梱表記 |
| --- | --- | --- |
| Androidキーレイアウト | Android Open Source Project、Apache License 2.0 | [本文](third_party/android/LICENSE-2.0.txt)。元ヘッダーも保持 |
| Chicago Kare | [Duane King](https://github.com/KingDuane/Chicago-Kare)、Copyright 2024、MIT | [ライセンス](assets/fonts/chicago/LICENSE.txt)・[表記](assets/fonts/chicago/NOTICE.txt) |
| Source Sans 3 | [Adobe](https://github.com/adobe-fonts/source-sans)、SIL OFL 1.1 | [ライセンス](assets/fonts/source-sans/LICENSE.txt)・[表記](assets/fonts/source-sans/NOTICE.txt) |
| TeX Gyre Heros | [GUST](https://www.gust.org.pl/projects/e-foundry/tex-gyre/heros)、LPPLを基にしたGUST Font License | [ライセンス](assets/fonts/heros/LICENSE.txt)・[LPPL](assets/fonts/heros/LPPL-1.3c.txt)・[表記](assets/fonts/heros/NOTICE.txt) |
| U-M+ビットマップフォント | [Pyxel](https://github.com/kitao/pyxel)から取得。Pyxel本体のコードライセンスだけでなく、フォント固有のM+条件を保持 | [ライセンス](assets/fonts/LICENSE_MPLUS_BITMAP.txt)・[表記](assets/fonts/NOTICE.txt) |
| LibVLC音楽用ビルド／Java 3.7.1 | [ソース・構成・実機検証](docs/ja/libvlc-audio-build.md)。LGPL 2.1と依存物固有の条件 | [LGPL本文](third_party/libvlc/COPYING.LIB)・[表記](assets/licenses/libvlc/)・[ELF調査](docs/licensing/native-audio-binaries.json) |

U-M+のBDF元データと実行時用変換データは`assets/fonts`に保持しています。Chicago Kareやカラーテーマ用フォントは再現・代替であり、Appleから抽出したフォントではありません。AppleのWeb版iPod体験の素材は取り込んでいません。

## バイナリ公開前に確認する項目

- **純正FW**：[コミュニティ開発者ガイド](https://innioasis.app/developers.html)はカスタムROMの作成・公開を明示的に案内しています。現3.1.2のベンダー部分の条件とOSS／対応ソースの棚卸は引き続き確認します。メーカー個別許諾だけが唯一の方法ではなく、適用ライセンスの確認やベンダーバイトを含めないローカル組立方式も選択肢です。
- **PLUMOSAMPスキン**：開発中のテーマには、作者提供の`PLUMOSAMP-pixel-redesign`の必要部分と、新規の桃のドット絵を使用します。未使用の画像・カーソル・第三者の解説文は取り込みません。元デザインを参考にしたことだけで画像の流用とは判断しません。来歴と配布物ごとの確認事項は`assets/skins/winamp/NOTICE.txt`と[配布条件整理](docs/ja/distribution-review.md)に記録しています。
- **LibVLC**：0.10.22ではソース再ビルドを実施し、旧MavenネイティブのGPL実装を除外。対応ソース・構成・依存物表記を同梱します。公開時はソースアーカイブをバイナリと併せて提供し、改造版の再リンク・導入手順も維持してください。別署名の改造FWを新規に書き込む経路は文書化済みですが、別の署名鍵による実書込み試験は未実施です。

[詳細な調査結果・公開前タスク](docs/ja/distribution-review.md)を参照してください。

提供された起動ロゴと依頼された実機写真を、このリリース用の素材として収録します。独自コード・文書のMITライセンスは第三者素材の条件を変更しません。

製品名はデザイン参考元・対応機種を示し、提携や第三者商標の所有を示しません。音楽ファイル、無関係な私的スクリーンショット、個人のFWバックアップ、署名鍵は公開ソースパッケージに含めません。

Aviatorのデモスクリーンショットに表示される音楽情報・ジャケットの権利は各権利者に帰属し、プロジェクトのMITライセンスの対象ではありません。

NDKランタイム／コンパイラーと静的Android supportのBSD／MIT等の表記は[NDK-NOTICE.txt](third_party/libvlc/licenses/NDK-NOTICE.txt)と[ANDROID-SUPPORT-NOTICE.txt](third_party/libvlc/licenses/ANDROID-SUPPORT-NOTICE.txt)に保持し、リリースにもRUNTIME_NOTICES.txtとして添付します。
