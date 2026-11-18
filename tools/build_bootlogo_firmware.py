#!/usr/bin/env python3
"""Convert the supplied logo and package only the Y1 logo/system partitions."""
from pathlib import Path
import hashlib,struct,subprocess,zlib,zipfile,array,sys,re
from PIL import Image
root=Path(__file__).resolve().parents[1]
base=root/'output/Y1_3.1.2_ADB_THEME_0.10.21_STANDALONE_WHEEL_TypeA'
out=root/'output/Y1_3.1.2_ADB_THEME_0.10.21_PLUMOS_LOGO_TypeA'
assets=root/'firmware/branding'
out.mkdir(exist_ok=True)
for p in base.iterdir():
 if p.is_file() and p.name not in ['README_JA.md','SHA256SUMS','fw_package.ini','.state','hwparam.json','.DS_Store']:
  subprocess.run(['cp','-c',str(p),str(out/p.name)],check=True)
image=Image.open(assets/'plumos-logo-source.png').convert('RGB')
assert image.width*3==image.height*4
image=image.resize((480,360),Image.Resampling.LANCZOS)
image.save(assets/'plumos-logo-480x360.png')
pixels=array.array('H',(((r>>3)<<11)|((g>>2)<<5)|(b>>3) for r,g,b in image.getdata()))
if sys.byteorder!='little':pixels.byteswap()
raw=pixels.tobytes();assert len(raw)==480*360*2
(assets/'boot_logo').write_bytes(raw)
# Decode our RGB565 payload for visual comparison.
Image.frombytes('RGB',(480,360),raw,'raw','BGR;16').save(assets/'plumos-logo-rgb565-preview.png')
original=(base/'logo.bin').read_bytes()
assert struct.unpack_from('<I',original)[0]==0x58881688
n,size=struct.unpack_from('<II',original,512)
assert n==39 and size==len(original)-512
old_offsets=struct.unpack_from('<'+'I'*n,original,520)
entries=[original[512+old_offsets[i]:512+(old_offsets[i+1] if i+1<n else size)] for i in range(n)]
original_entries=list(entries)
for i in [0,38]:
 assert len(zlib.decompress(entries[i]))==len(raw)
 entries[i]=zlib.compress(raw,9)
offsets=[];position=8+4*n
for data in entries:offsets.append(position);position+=len(data)
payload=struct.pack('<II',n,position)+struct.pack('<'+'I'*n,*offsets)+b''.join(entries)
header=bytearray(original[:512]);struct.pack_into('<I',header,4,len(payload))
(out/'logo.bin').write_bytes(header+payload)
assert len(header+payload)<0x300000
for i,entry in enumerate(entries):
 if i in (0,38):assert zlib.decompress(entry)==raw
 else:assert entry==original_entries[i]
with zipfile.ZipFile(assets/'bootanimation.zip','w',compression=zipfile.ZIP_STORED) as archive:
 archive.writestr('desc.txt','480 360 1\np 0 0 part0\n')
 archive.write(assets/'plumos-logo-480x360.png','part0/00000.png')
commands=assets/'branding.debugfs'
commands.write_text(f'rm /media/bootanimation.zip\nwrite {assets}/bootanimation.zip /media/bootanimation.zip\nset_inode_field /media/bootanimation.zip mode 0100644\nset_inode_field /media/bootanimation.zip uid 0\nset_inode_field /media/bootanimation.zip gid 0\nrm /media/images/boot_logo\nwrite {assets}/boot_logo /media/images/boot_logo\nset_inode_field /media/images/boot_logo mode 0100644\nset_inode_field /media/images/boot_logo uid 0\nset_inode_field /media/images/boot_logo gid 0\n')
debug='/opt/homebrew/opt/e2fsprogs/sbin/debugfs'
subprocess.run([debug,'-w','-f',str(commands),str(out/'system.img')],check=True)
subprocess.run(['/opt/homebrew/opt/e2fsprogs/sbin/e2fsck','-fn',str(out/'system.img')],check=True)
scatter=out/'MT6572_Android_scatter.txt'
text=scatter.read_text();text,count=re.subn(r'(partition_name: LOGO\n\s+file_name: logo.bin\n\s+is_download: )false',r'\g<1>true',text)
assert count==1
scatter.write_text(text)
blocks=text.split('- partition_index:')[1:]
assert [re.search(r'partition_name: (\S+)',b)[1] for b in blocks if 'is_download: true' in b]==['LOGO','ANDROID']
(out/'fw_package.ini').write_text('[Firmware]\nsoftware_name = plumOS Y1 Boot Logo\nversion = 0.10.21-logo1\ntarget_model = Y1\ntarget_type = Type A\ndescription = Theme Player 0.10.21 plus plumOS first-stage and Android boot logo\nenabled = true\n')
(out/'README_JA.md').write_text('''# Y1 Type A plumOSロゴ版 (アプリ0.10.21)

0.10.21_STANDALONE_WHEELを基に起動ロゴだけ変更。
提供画像plumos-logo-y1.png(1024x768)を縦横比維持で480x360へ縮小。

- logo.binの画像0と38を置換。残り37画像の圧縮データは元と完全一致。
- /system/media/images/boot_logoを同じRGB565画像へ変更。
- /system/media/bootanimation.zipを同じ画像の静止表示へ変更。
- scatterはLOGOとANDROIDのみ書込み対象。userdata/boot/preloaderは変更なし。
- Theme Player APKは0.10.21とバイト単位で同一。

充電表示等は維持。起動方向・色・実機での切替タイミングは未検証。
元FWと元ロゴは以前の0.10.21_STANDALONE_WHEELフォルダに保存済み。
Type A専用。UpdaterからこのZIPを選択する。
以前のANDROIDのみ書くscatterでは初期ロゴは更新されない。
''')
with (out/'SHA256SUMS').open('w') as file:
 for p in sorted(out.iterdir()):
  if p.is_file() and p.name!='SHA256SUMS':file.write(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n')
archive=out.parent/(out.name+'.zip')
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(out.iterdir()):
  if p.is_file():z.write(p,p.name)
print(archive)
subprocess.run([sys.executable, str(root/'tools/package_release.py')], check=True)
