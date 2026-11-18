# Component notices / コンポーネント表記 — 0.10.24

## English

Original plumOS-Y1 code/documentation and the new peach pixel drawing: Copyright (c) 2026 plumOS contributors, MIT (LICENSE). This does not relicense the complete firmware or third-party components.

The package modifies stock Innioasis Y1 Type A firmware 3.1.2. Its original component terms and notices remain applicable. The project follows the community custom-firmware publishing workflow at https://innioasis.app/developers.html; an additional individual manufacturer-permission inquiry is not a release prerequisite. This community publishing basis is not described as relicensing every bundled component.

PLUMOSAMP uses creator-supplied PLUMOSAMP-pixel-redesign artwork, provided for this project. Only required regions are retained, with new peach icons replacing the lightning marks. Unused sprite sheets, cursors, tutorial text and slogan rows are excluded. The theme ID remains winamp for saved-setting compatibility. The original Webamp/Winamp artwork is not described as MIT-licensed.

The ARMv7/API17 LibVLC audio libraries and their corresponding source are unchanged from the source-built 0.10.22 profile: 75 LGPL-labeled modules and 18 source-built contribs; the old Maven DVD/GPL implementations are excluded. Retain LGPL-2.1, upstream copyrights and component licenses. The C++ runtime uses Apache 2.0 with LLVM Exceptions. RUNTIME_NOTICES.txt and ANDROID_SUPPORT_NOTICES.txt preserve the NDK/runtime and statically linked support notices. The APK includes dependency notices under assets/licenses/libvlc.

Publish both matching source archives alongside the binaries:

- plumOS-Y1-v0.10.24-source.tar.gz — application, skin composition recipe, fonts, notices and documentation.
- plumOS-Y1-v0.10.24-libvlc-source.tar.gz — unchanged native/JNI sources, patches, 18 contrib archives, bootstrap sources, build configuration, notices and source manifest.

The native source archive retains its original build identity because it corresponds to the unchanged libraries. A modified firmware/app pair must use a consistent signing key; the publisher's private key is not distributed. See the developer guide for rebuilding and installing modified libraries. No legal clearance is inferred from pixel-difference counts or the manifest's derivative field.

## 日本語

独自コード・文書・新しい桃のドット絵はCopyright (c) 2026 plumOS contributors、MIT（LICENSE）です。FW全体や第三者部品をMITへ変更するものではありません。

純正Innioasis Y1 Type A FW 3.1.2を加工し、既存の部品の条件・表記を維持します。https://innioasis.app/developers.html のコミュニティCFW公開手順を根拠として公開準備を進め、メーカーへの追加の個別許諾問い合わせは公開の前提条件にしません。この方針が同梱部品すべての再ライセンスを意味するとは説明しません。

PLUMOSAMPは、本プロジェクト向けに作者から提供されたPLUMOSAMP-pixel-redesignの必要部分を使用します。稲妻は新しい桃に置き換え、未使用画像・カーソル・解説文・スローガン行は取り込みません。保存設定の互換性のためテーマIDはwinampを維持します。元のWebamp／Winamp画像をMITとは表記しません。

ARMv7/API17用LibVLCと対応ソースは、0.10.22でソースから構築した音楽用プロファイルと同一です。75のLGPL表記モジュールと18依存物を使用し、旧Maven版のDVD／GPL実装は除外しました。LGPL-2.1、上流の著作権・各部品の条件を維持してください。C++ランタイムはApache 2.0 with LLVM Exceptions。NDK／ランタイムと静的Android supportの表記はRUNTIME_NOTICES.txtとANDROID_SUPPORT_NOTICES.txt、APK内の依存物表記はassets/licenses/libvlcにあります。

バイナリと共に、plumOS-Y1-v0.10.24-source.tar.gz（アプリ・テーマ構成手順・フォント・表記・文書）とplumOS-Y1-v0.10.24-libvlc-source.tar.gz（ネイティブ／JNI・パッチ・18依存物・構築用ソース・構成・表記・目録）を公開してください。ネイティブのバイトを変更しないため、対応ソース内の構築時の識別情報も維持します。改造FW／APKは利用者自身の同一鍵で統一し、公開者の秘密鍵は配布しません。再ビルド・改造版導入は開発者ガイドを参照してください。画素の変更率やderivative属性を法的な許諾そのものとは扱いません。
