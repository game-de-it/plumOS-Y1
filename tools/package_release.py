#!/usr/bin/env python3
"""Stage release assets with public plumOS names without changing partition payloads."""
from pathlib import Path
import hashlib
import shutil
import zipfile
import copy
import argparse
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def stage_firmware(source, target):
    """Remove host/device flash-session metadata, preserving partition payload bytes."""
    excluded = {'.state', 'hwparam.json', '.DS_Store'}
    overrides = {
        'README_JA.md': ROOT / 'release-config/FIRMWARE_README.md',
        'LIBVLC_README_JA.md': ROOT / 'release-config/FIRMWARE_LIBVLC_NOTICE.md',
        'COPYING.LGPL-2.1': ROOT / 'third_party/libvlc/COPYING.LIB',
        'COPYING.GPL-2.0': ROOT / 'third_party/libvlc/licenses/GPL-2.0.txt',
        'COPYING.GPL-3.0': ROOT / 'third_party/libvlc/licenses/GPL-3.0.txt',
        'COPYING.LGPL-3.0': ROOT / 'third_party/libvlc/licenses/LGPL-3.0.txt',
        'COPYING.LLVM': ROOT / 'third_party/libvlc/licenses/LLVM-LICENSE.txt',
        'RELEASE_NOTICES.md': ROOT / 'release-config/RELEASE_NOTICES.md',
        'RUNTIME_NOTICES.txt': ROOT / 'third_party/libvlc/licenses/NDK-NOTICE.txt',
        'ANDROID_SUPPORT_NOTICES.txt': ROOT / 'third_party/libvlc/licenses/ANDROID-SUPPORT-NOTICE.txt',
    }
    hashes = {}
    with zipfile.ZipFile(source) as original, zipfile.ZipFile(target, 'w') as clean:
        checksum_info = None
        for entry in original.infolist():
            name = entry.filename
            if Path(name).name in excluded or Path(name).name.startswith('._'):
                continue
            if name == 'SHA256SUMS':
                checksum_info = copy.copy(entry)
                continue
            if name in overrides:
                content = overrides[name].read_bytes()
                clean.writestr(copy.copy(entry), content)
                hashes[name] = hashlib.sha256(content).hexdigest()
                continue
            h = hashlib.sha256()
            with original.open(entry) as src, clean.open(copy.copy(entry), 'w') as dst:
                for block in iter(lambda: src.read(1024 * 1024), b''):
                    h.update(block); dst.write(block)
            hashes[name] = h.hexdigest()
        for name, path in sorted(overrides.items()):
            if name in hashes: continue
            content = path.read_bytes()
            entry = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            entry.compress_type = zipfile.ZIP_DEFLATED
            clean.writestr(entry, content)
            hashes[name] = hashlib.sha256(content).hexdigest()
        content = ''.join(f'{hashes[name]}  {name}\n' for name in sorted(hashes))
        clean.writestr(checksum_info or 'SHA256SUMS', content)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--firmware', type=Path, help='Repackage a known firmware ZIP; partition bytes are preserved')
    args = parser.parse_args()
    version = ET.parse(ROOT / 'AndroidManifest.xml').getroot().attrib[ANDROID + 'versionName']
    legacy = ROOT / 'output' / f'Y1_3.1.2_ADB_THEME_{version}_PLUMOS_LOGO_TypeA.zip'
    firmware = args.firmware or (legacy if legacy.is_file() else ROOT / 'release' / f'v{version}' / f'plumOS-Y1-v{version}-TypeA.zip')
    assets = [
        (firmware,
         f'plumOS-Y1-v{version}-TypeA.zip'),
        (ROOT / 'dist' / f'Y1ThemePlayer-{version}.apk',
         f'plumOS-Y1-v{version}-player.apk'),
    ]
    for source, _ in assets:
        if not source.is_file():
            raise SystemExit(f'Missing build input: {source}')
    destination = ROOT / 'release' / f'v{version}'
    destination.mkdir(parents=True, exist_ok=True)
    checksums = []
    for source, name in assets:
        target = destination / name
        temporary = destination / (name + '.tmp')
        if source.suffix == '.zip':
            stage_firmware(source, temporary)
        else:
            shutil.copyfile(source, temporary)
        digest = sha256(temporary)
        if source.suffix != '.zip' and digest != sha256(source):
            temporary.unlink()
            raise SystemExit(f'Copy verification failed: {name}')
        temporary.replace(target)
        checksums.append(f'{digest}  {name}\n')
        print(target.relative_to(ROOT))
    temporary = destination / 'SHA256SUMS.tmp'
    temporary.write_text(''.join(checksums), encoding='utf-8')
    temporary.replace(destination / 'SHA256SUMS')
    print((destination / 'SHA256SUMS').relative_to(ROOT))


if __name__ == '__main__':
    main()
