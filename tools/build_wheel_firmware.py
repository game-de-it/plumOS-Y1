#!/usr/bin/env python3
"""Build the standalone FE firmware on the verified 0.10.6 wheel image."""
from pathlib import Path
import subprocess,hashlib,zipfile
root=Path(__file__).resolve().parents[1]
base=root/'output/Y1_3.1.2_ADB_THEME_0.10.6_WHEEL_TypeA'
out=root/'output/Y1_3.1.2_ADB_THEME_0.10.21_STANDALONE_WHEEL_TypeA'
out.mkdir(exist_ok=True)
for p in base.iterdir():
 if p.is_file() and p.name not in ('README_JA.md','SHA256SUMS','fw_package.ini','.state','hwparam.json','.DS_Store'):
  subprocess.run(['cp','-c',str(p),str(out/p.name)],check=True)
apk=root/'dist/Y1ThemePlayer-0.10.21.apk'
commands=root/'firmware/keys/standalone.debugfs'
subprocess.run(['/opt/homebrew/opt/e2fsprogs/sbin/debugfs','-R',f'dump /build.prop {root}/firmware/keys/build.prop.usb',str(out/'system.img')],check=True)
prop=root/'firmware/keys/build.prop.usb'
prop.write_text(prop.read_text().replace('persist.sys.usb.config=mass_storage,adb','persist.sys.usb.config=adb'))
commands.write_text(f'rm /build.prop\nwrite {prop} /build.prop\nset_inode_field /build.prop mode 0100644\nset_inode_field /build.prop uid 0\nset_inode_field /build.prop gid 0\nrm /app/Y1ThemePlayer.apk\nwrite {apk} /app/Y1ThemePlayer.apk\nset_inode_field /app/Y1ThemePlayer.apk mode 0100644\nset_inode_field /app/Y1ThemePlayer.apk uid 0\nset_inode_field /app/Y1ThemePlayer.apk gid 0\n')
subprocess.run(['/opt/homebrew/opt/e2fsprogs/sbin/debugfs','-w','-f',str(commands),str(out/'system.img')],check=True)
subprocess.run(['/opt/homebrew/opt/e2fsprogs/sbin/e2fsck','-fn',str(out/'system.img')],check=True)
(out/'fw_package.ini').write_text('[Firmware]\nsoftware_name = Y1 Theme Player Standalone Wheel\nversion = 0.10.21\ntarget_model = Y1\ntarget_type = Type A\ndescription = Standalone FE, idle ADB only, explicit USB transfer, SystemUI crash fix\nenabled = true\n')
(out/'README_JA.md').write_text('''# Y1 Type A 0.10.21 Standalone Wheel

0.10.6_WHEELを基にAPKを0.10.21へ更新しbuild.propの既定USBをadbへ変更。Winamp/Hi-MDを含む10テーマを維持。
ホイールの上下キーマップ2本も維持。boot/userdata/preloaderは変更なし。

変更点:
- USBでコピーしたAPKを設定のアプリ更新から選択。アプリID・署名・バージョンを確認してシステムAPIで内部領域へ上書きする。
- 設定を曲表示・ライブラリ、音・操作、画面・言語、転送・接続、時刻・システム・電源の順に整理。
- 起動時とアプリ起動時にcom.innioasis.y1全体を無効化し、純正FEの常駐・起動を停止。
- com.android.systemui.usb.UsbStorageActivityを有効に戻してSystem UIのクラッシュを修正。
- 通常USBはadb専用。自作アプリでUSB転送開始を選んだ時だけmass_storage,adbへ切替、終了後adbへ戻す。
- USB転送は自作アプリの設定から実施。
- クリック音をSoundPoolで直接再生。クリックON/OFFとバイブON/OFFは独立。
- フォルダ読み取りはバックグラウンドで実行。古い読み取り結果は破棄する。
- 全曲スキャン時の並べ替えとアルバム集計もバックグラウンドで行う。
- スキャン中は全テーマに状態表示し、既存リストは操作可能。
- SQLiteキャッシュを先に表示し、サイズと更新日時が変わった曲だけタグ再取得。
- 設定に音楽再スキャン/キャッシュ削除・再構築を追加。キャッシュ削除は音楽ファイルを削除しない。
- Chicago Kareテーマを追加。収録外の文字はAndroid標準フォントへfallback。
- INSTALL_PACKAGES権限を追加。旧FWへのAPK更新だけでは権限が付かないため、初回はこのFWの適用が必要。
- 新規権限CHANGE_COMPONENT_ENABLED_STATEとMANAGE_USBはAndroid4.2.2のsignatureOrSystem権限。
  旧system APKへのADB更新だけではMANAGE_USB権限が付かないため、このFWの適用が必要。

元に戻すADB操作（FW変更は不要）:
  adb -s YOUR_DEVICE_SERIAL shell pm enable com.innioasis.y1
  adb -s YOUR_DEVICE_SERIAL shell pm enable com.android.systemui/.usb.UsbStorageActivity
0.10.21を再起動すると再び無効化するため、復旧時は0.10.6以前のアプリへ戻すこと。

注意: 純正FEに含まれる写真/電子書籍/電卓等も起動不可になる。
FMは自作アプリから別パッケージcom.mediatek.FMRadioを使用。

検証状態: APKビルド/署名、イメージe2fsck、FW内のAPKとキー設定の照合。
実機にはADB更新APKと手動無効化を適用。FW自体のフラッシュ・再起動後の権限付与、
クリック音の実音、USB抜き差しと転送は別途実機確認が必要。

Type A専用。UpdaterのファイルからインストールでZIPを指定。
scatterはANDROIDのみ書込み指定。
''')
(out/'LICENSE').write_text((root/'LICENSE').read_text())
with (out/'SHA256SUMS').open('w') as f:
 for p in sorted(out.iterdir()):
  if p.is_file() and p.name!='SHA256SUMS': f.write(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n')
archive=out.parent/(out.name+'.zip')
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(out.iterdir()):
  if p.is_file():z.write(p,p.name)
print(archive)
