#!/usr/bin/env python3
"""Replace APK and all three native copies in the verified standalone Y1 firmware."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import subprocess
import tempfile
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]

def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024*1024), b''): digest.update(block)
    return digest.hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base', type=Path, help='A downloaded, hash-verified release firmware ZIP')
    parser.add_argument('--base-version', default='0.10.24', choices=['0.10.21', '0.10.22', '0.10.23', '0.10.24'])
    parser.add_argument('--debugfs', type=Path, default=Path('/opt/homebrew/opt/e2fsprogs/sbin/debugfs'))
    args = parser.parse_args()
    base_name = f'plumOS-Y1-v{args.base_version}-TypeA.zip'
    args.base = args.base or ROOT/'release'/f'v{args.base_version}'/base_name
    expected = json.loads((ROOT/f'release-config/v{args.base_version}.json').read_text())['tested_binary_sha256'][base_name]
    if sha(args.base) != expected: raise SystemExit('Base firmware hash mismatch')
    version = ET.parse(ROOT/'AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName']
    out = ROOT/'output'/f'Y1_3.1.2_ADB_THEME_{version}_PLUMOS_LOGO_TypeA'
    out.mkdir(exist_ok=True)
    with zipfile.ZipFile(args.base) as archive:
        for entry in archive.infolist():
            if '/' in entry.filename or entry.filename in {'.state', 'hwparam.json', '.DS_Store'}: raise SystemExit('Unexpected base entry')
            archive.extract(entry, out)
    apk = ROOT/'dist'/f'Y1ThemePlayer-{version}.apk'
    native = ROOT/'third_party/libvlc/audio/extracted/jni/armeabi-v7a'
    replacements = [(apk, '/app/Y1ThemePlayer.apk')] + [(native/name, '/lib/'+name) for name in ['libvlc.so', 'libvlcjni.so', 'libc++_shared.so']]
    commands = []
    for src, dest in replacements:
        if not src.is_file(): raise SystemExit('Missing replacement: '+src.name)
        quoted_src = '"' + str(src).replace('\\', '\\\\').replace('"', '\\"') + '"'
        commands.extend([f'rm {dest}', f'write {quoted_src} {dest}', f'set_inode_field {dest} mode 0100644', f'set_inode_field {dest} uid 0', f'set_inode_field {dest} gid 0'])
    # debugfs command paths must remain local build products, never public sources.
    with tempfile.TemporaryDirectory(prefix='y1-native-fw-') as temp:
        file = Path(temp)/'replace.debugfs'; file.write_text('\n'.join(commands)+'\n')
        subprocess.run([str(args.debugfs), '-w', '-f', str(file), str(out/'system.img')], check=True)
        for i, (src, dest) in enumerate(replacements):
            dumped = Path(temp)/f'verify-{i}'
            subprocess.run([str(args.debugfs), '-R', f'dump {dest} "{dumped}"', str(out/'system.img')], check=True)
            if sha(src) != sha(dumped): raise SystemExit('FW replacement hash mismatch: '+dest)
    subprocess.run([str(args.debugfs.with_name('e2fsck')), '-fn', str(out/'system.img')], check=True)
    # Clear unallocated blocks so the removed, larger native libraries do not
    # remain as recoverable fragments in the raw ext4 image.
    subprocess.run(['docker', 'run', '--rm', '--platform', 'linux/amd64',
        '--mount', f'type=bind,source={ROOT},target=/workspace', 'plumos-y1-libvlc:ndk21',
        'sh', '-c', 'apt-get update -qq && apt-get install -y --no-install-recommends zerofree >/dev/null && zerofree "$1"',
        'sh', str(Path('/workspace')/(out/'system.img').relative_to(ROOT))], check=True)
    subprocess.run([str(args.debugfs.with_name('e2fsck')), '-fn', str(out/'system.img')], check=True)

    # All non-system partition images and the wheel/USB fixes remain from the tested base.
    with zipfile.ZipFile(args.base) as archive:
        for entry in archive.infolist():
            if entry.filename.endswith(('.img', '.bin')) and entry.filename != 'system.img':
                if sha(out/entry.filename) != hashlib.sha256(archive.read(entry)).hexdigest(): raise SystemExit('Unrelated partition changed')
    (out/'fw_package.ini').write_text(f'[Firmware]\nsoftware_name = plumOS Y1\nversion = {version}\ntarget_model = Y1\ntarget_type = Type A\ndescription = PLUMOSAMP peach theme; audio-focused LibVLC; standalone wheel and USB fixes\nenabled = true\n')
    (out/'LIBVLC_README_JA.md').write_text((ROOT/'release-config/FIRMWARE_LIBVLC_NOTICE.md').read_text())
    (out/'README_JA.md').write_text((ROOT/'release-config/FIRMWARE_README.md').read_text())
    shutil.copyfile(ROOT/'third_party/libvlc/audio/BUILD.json', out/'NATIVE_BUILD.json')
    sums = ''.join(f'{sha(p)}  {p.name}\n' for p in sorted(out.iterdir()) if p.is_file() and p.name != 'SHA256SUMS')
    (out/'SHA256SUMS').write_text(sums)
    release = ROOT/'release'/f'v{version}'; release.mkdir(exist_ok=True)
    target = release/f'plumOS-Y1-v{version}-TypeA.zip'
    with zipfile.ZipFile(target, 'w', zipfile.ZIP_DEFLATED) as archive:
        for p in sorted(out.iterdir()):
            if p.is_file(): archive.write(p, p.name)
    shutil.copyfile(apk, release/f'plumOS-Y1-v{version}-player.apk')
    (release/'SHA256SUMS').write_text(''.join(f'{sha(p)}  {p.name}\n' for p in sorted(release.iterdir()) if p.is_file() and p.name != 'SHA256SUMS'))
    print(target)

if __name__ == '__main__': main()
