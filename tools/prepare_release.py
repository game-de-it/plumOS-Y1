#!/usr/bin/env python3
"""Prepare an audited local source tree and bilingual release assets; never publish."""
from pathlib import Path
import ast
import datetime
import configparser
import zipfile
import gzip
import hashlib
import json
import os
import re
import shutil
import subprocess
import tarfile
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
TOP = ['AndroidManifest.xml', 'LICENSE', 'NOTICE.md', 'NOTICE_JA.md', 'README.md',
       'README_JA.md', 'THIRD_PARTY_NOTICES.md', 'THIRD_PARTY_NOTICES_JA.md',
       'build.sh', 'install-theme.py']
HELPERS = ['build_wheel_firmware.py', 'build_bootlogo_firmware.py',
           'build_audio_firmware.py', 'package_release.py', 'prepare_release.py',
           'pack_bdf.py', 'audit_native_licenses.py', 'build_plumosamp_skin.py']
KEY_LAYOUTS = ['Generic.original.kl', 'mtk-tpd.kl', 'mtk-tpd-kpd.kl']


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda: f.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def selected_files(version):
    paths = {ROOT / name for name in TOP}
    for folder in ['src', 'assets', 'example', 'docs']:
        paths.update(p for p in (ROOT / folder).rglob('*') if p.is_file())
    paths.update(ROOT / 'tools' / name for name in HELPERS)
    paths.update(p for p in (ROOT / 'tools/libvlc').rglob('*') if p.is_file())
    paths.update(ROOT / 'firmware/keys' / name for name in KEY_LAYOUTS)
    paths.add(ROOT / 'firmware/branding/plumos-logo-source.png')
    paths.update(p for p in (ROOT / 'third_party/libvlc').glob('*') if p.is_file()
                 and p.suffix == '.md')
    paths.add(ROOT / 'third_party/libvlc/COPYING.LIB')
    paths.update(p for p in (ROOT / 'third_party/libvlc/licenses').iterdir() if p.is_file())
    paths.update(p for p in (ROOT / 'third_party/libvlc/upstream').iterdir() if p.is_file())
    paths.update(ROOT / 'third_party/libvlc/audio' / name for name in ['README.md', 'BUILD.json'])
    paths.update(p for p in (ROOT / 'third_party/libvlc/audio/dependency-notices').rglob('*') if p.is_file())
    paths.add(ROOT / 'third_party/libvlc/audio/evidence/linked-modules.json')
    paths.add(ROOT / 'third_party/android/LICENSE-2.0.txt')
    paths.add(ROOT / f'release-config/v{version}.json')
    paths.update((ROOT / 'release-config').glob('v*.json'))
    paths.add(ROOT / 'release-config/RELEASE_NOTICES.md')
    paths.add(ROOT / 'release-config/FIRMWARE_README.md')
    paths.add(ROOT / 'release-config/FIRMWARE_LIBVLC_NOTICE.md')
    result = []
    for p in sorted(paths):
        rel = p.relative_to(ROOT)
        if p.name == '.DS_Store' or p.name.startswith('._') or '__pycache__' in rel.parts:
            continue
        if p.is_symlink() or not p.is_file():
            raise SystemExit(f'Invalid source input: {rel}')
        result.append(p)
    return result


def audit(stage):
    forbidden = re.compile(rb'/[U]sers/|/[V]olumes/|192[.]168[.]|10[.](?:[0-9]{1,3}[.]){2}|[0]123456789ABCDEF|(?:[0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----')
    bad_suffix = {'.keystore', '.jks', '.p12', '.apk', '.img', '.so', '.mp3', '.m4a', '.mp4', '.wav', '.log'}
    for p in stage.rglob('*'):
        if not p.is_file():
            continue
        rel = p.relative_to(stage)
        if p.suffix.lower() in bad_suffix or p.name.startswith('._') or p.name == '.DS_Store':
            raise SystemExit(f'Unexpected source content: {rel}')
        # Upstream archives/JARs are preserved verbatim with provenance hashes.
        if p.suffix.lower() in {'.md', '.txt', '.py', '.java', '.json', '.xml', '.sh', '.kl'}:
            if forbidden.search(p.read_bytes()):
                raise SystemExit(f'Private-data pattern: {rel}')
        if p.suffix == '.py':
            ast.parse(p.read_text(encoding='utf-8'))
        if p.suffix.lower() in {'.png', '.jpg', '.jpeg'}:
            from PIL import Image
            with Image.open(p) as im:
                if im.getexif() or any(k.lower() in {'exif', 'xmp', 'xml:com.adobe.xmp'} for k in im.info):
                    raise SystemExit(f'Image metadata needs review: {rel}')
        if p.suffix == '.md':
            s = p.read_text(encoding='utf-8')
            targets = re.findall(r'\]\(([^)]+)\)', s) + re.findall(r'(?:src|href)="([^"]+)"', s)
            for target in targets:
                if '://' in target or target.startswith('#'):
                    continue
                if not (p.parent / target.split('#')[0]).exists():
                    raise SystemExit(f'Broken source link: {rel}: {target}')
    for script in stage.rglob('*.sh'):
        subprocess.run(['sh', '-n', str(script)], check=True)


def replace_generated_tree(stage, target):
    if target.exists():
        manifest_path = target / 'SOURCE_FILES.json'
        if not manifest_path.is_file():
            raise SystemExit('Existing public tree is not a known generated tree; leaving it untouched')
        old = json.loads(manifest_path.read_text())['files']
        actual = {str(p.relative_to(target)) for p in target.rglob('*') if p.is_file()}
        if actual != set(old) | {'SOURCE_FILES.json'} or any(digest(target / name) != h for name, h in old.items()):
            raise SystemExit('Existing public tree has edits or extra files; leaving it untouched')
        shutil.rmtree(target)
    stage.rename(target)


def main():
    version = ET.parse(ROOT / 'AndroidManifest.xml').getroot().attrib[ANDROID + 'versionName']
    config = json.loads((ROOT / f'release-config/v{version}.json').read_text())
    manifest_attrs = ET.parse(ROOT / 'AndroidManifest.xml').getroot().attrib
    if config['version'] != version or int(manifest_attrs[ANDROID + 'versionCode']) != config['version_code']:
        raise SystemExit('Source version does not match release-config')
    out = ROOT / 'release' / f'v{version}'
    for name, expected in config['tested_binary_sha256'].items():
        if digest(out / name) != expected:
            raise SystemExit(f'Tested binary changed: {name}; validate and version it before preparation')
    sdk = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk')))
    verifier = sdk / 'build-tools/35.0.1/apksigner'
    result = subprocess.check_output([str(verifier), 'verify', '--print-certs',
                                     str(out / f'plumOS-Y1-v{version}-player.apk')], text=True)
    cert = re.search(r'certificate SHA-256 digest: ([0-9a-f]+)', result)
    if not cert or cert[1] != config['signing_certificate_sha256']:
        raise SystemExit('Release signing identity does not match')
    badging = subprocess.check_output([str(sdk / 'build-tools/35.0.1/aapt2'), 'dump', 'badging', str(out / f'plumOS-Y1-v{version}-player.apk')], text=True)
    if f"name='com.kroot.y1themes'" not in badging or f"versionName='{version}'" not in badging or f"versionCode='{config['version_code']}'" not in badging:
        raise SystemExit('APK package/version does not match source/config')
    public = ROOT / 'public'
    public.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='.plumos-source-', dir=public) as tmp:
        stage = Path(tmp) / 'plumOS-Y1'
        stage.mkdir()
        for source in selected_files(version):
            dest = stage / source.relative_to(ROOT)
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, dest)
            dest.chmod(0o755 if source.stat().st_mode & 0o111 else 0o644)
        (stage / '.gitignore').write_text('''# Generated outputs and private local material
.DS_Store
._*
__pycache__/
/build/
/dist/
/output/
/release/
/public/
/reports/
/previews/
/converted-bgm/
/third_party/libvlc/extracted/
/third_party/libvlc/audio/extracted/
/third_party/libvlc/*.aar
/firmware/keys/*.debugfs
/firmware/keys/build.prop.usb
/firmware/branding/*.debugfs
/firmware/branding/boot_logo
/firmware/branding/bootanimation.zip
/firmware/branding/plumos-logo-480x360.png
/firmware/branding/plumos-logo-rgb565-preview.png
*.keystore
*.jks
*.p12
.env
.env.*
''')
        (stage / '.gitattributes').write_text('* text=auto\n*.sh text eol=lf\n*.py text eol=lf\n*.java text eol=lf\n*.md text eol=lf\n*.png binary\n*.jpg binary\n*.ttf binary\n*.otf binary\n*.bin binary\n*.jar binary\n*.gz binary\n')
        audit(stage)
        hashes = {str(p.relative_to(stage)): digest(p) for p in sorted(stage.rglob('*')) if p.is_file()}
        (stage / 'SOURCE_FILES.json').write_text(json.dumps({'version': version, 'files': hashes,
                   'self_excluded': True}, indent=2, ensure_ascii=False) + '\n')
        target = public / 'plumOS-Y1'
        replace_generated_tree(stage, target)
    archive = out / f'plumOS-Y1-v{version}-source.tar.gz'
    epoch = int(datetime.datetime.fromisoformat(config['release_date']).replace(tzinfo=datetime.timezone.utc).timestamp())
    with archive.open('wb') as raw, gzip.GzipFile(filename='', fileobj=raw, mode='wb', mtime=0) as gz:
        with tarfile.open(fileobj=gz, mode='w') as tar:
            for p in [target] + sorted(target.rglob('*')):
                info = tar.gettarinfo(str(p), str(Path('plumOS-Y1') / p.relative_to(target)))
                info.uid = info.gid = 0; info.uname = info.gname = ''; info.mtime = epoch
                if p.is_file():
                    with p.open('rb') as f: tar.addfile(info, f)
                else:
                    tar.addfile(info)
    for suffix, note in [('', f'v{version}.md'), ('.en', f'v{version}.en.md'), ('.ja', f'v{version}.ja.md')]:
        shutil.copyfile(ROOT / 'docs/releases' / note, out / f'RELEASE_NOTES{suffix}.md')
    shutil.copyfile(ROOT / 'LICENSE', out / 'LICENSE')
    shutil.copyfile(ROOT / 'release-config/RELEASE_NOTICES.md', out / 'RELEASE_NOTICES.md')
    shutil.copyfile(ROOT / 'third_party/libvlc/licenses/NDK-NOTICE.txt', out / 'RUNTIME_NOTICES.txt')
    extra = {'RELEASE_MANIFEST.json', 'SHA256SUMS'}
    allowed = set(config['tested_binary_sha256']) | {archive.name, 'RELEASE_NOTES.md', 'RELEASE_NOTES.en.md',
               'RELEASE_NOTES.ja.md', 'LICENSE', 'RELEASE_NOTICES.md', 'RUNTIME_NOTICES.txt'} | extra
    for language in ['en', 'ja']:
        name = f'VALIDATION.{language}.md'
        validation_doc = config.get('validation_document', 'libvlc-audio-build.md')
        shutil.copyfile(ROOT / 'docs' / language / validation_doc, out / name)
        # Standalone release assets have no repository-relative directory tree.
        text = (out / name).read_text()
        text = text.replace('../ja/libvlc-audio-build.md', 'VALIDATION.ja.md').replace('../en/libvlc-audio-build.md', 'VALIDATION.en.md')
        text = text.replace('../ja/release-validation.md', 'VALIDATION.ja.md').replace('../en/release-validation.md', 'VALIDATION.en.md')
        text = text.replace('](validation-v0.10.23.md)', f'](https://github.com/game-de-it/plumOS-Y1/blob/main/docs/{language}/validation-v0.10.23.md)')
        text = text.replace('../../third_party/', 'https://github.com/game-de-it/plumOS-Y1/blob/main/third_party/').replace('../licensing/', 'https://github.com/game-de-it/plumOS-Y1/blob/main/docs/licensing/')
        (out / name).write_text(text)
        allowed.add(name)
    unexpected = {p.name for p in out.iterdir()} - allowed
    if unexpected:
        raise SystemExit(f'Unexpected release files: {sorted(unexpected)}')
    assets = {p.name: {'sha256': digest(p), 'bytes': p.stat().st_size}
              for p in sorted(out.iterdir()) if p.is_file() and p.name not in extra}
    with zipfile.ZipFile(out / f'plumOS-Y1-v{version}-TypeA.zip') as z:
        firmware_meta = configparser.ConfigParser()
        firmware_meta.read_string(z.read('fw_package.ini').decode())
        if firmware_meta['Firmware'].get('version') != version or firmware_meta['Firmware'].get('target_type') != 'Type A':
            raise SystemExit('Firmware version/hardware metadata mismatch')
        if any(Path(n).name in {'.state', 'hwparam.json', '.DS_Store'} or Path(n).name.startswith('._') for n in z.namelist()):
            raise SystemExit('Flash-session metadata must not be shipped')
        for name, expected in config.get('tested_payload_sha256', {}).items():
            h = hashlib.sha256()
            with z.open(name) as f:
                for block in iter(lambda: f.read(1024 * 1024), b''): h.update(block)
            if h.hexdigest() != expected: raise SystemExit(f'Tested partition changed: {name}')
    manifest = {'project': 'plumOS-Y1', 'version': version, 'version_code': config['version_code'], 'release_date': config['release_date'],
                'github_release': {'repository': 'game-de-it/plumOS-Y1', 'tag': 'v' + version, 'title': 'plumOS-Y1 v' + version + ' — Initial release / 初回リリース', 'body_file': 'RELEASE_NOTES.md', 'url': config.get('release_url', 'https://github.com/game-de-it/plumOS-Y1/releases/tag/v' + version)},
                'target': config['hardware'], 'base_firmware': config['base_firmware'],
                'android_version': config['android_version'], 'firmware_package_metadata': dict(firmware_meta['Firmware']), 'firmware_partitions': ['LOGO', 'ANDROID'],
                'signing_certificate_sha256': cert[1], 'source_file_count': len(hashes),
                'validation': config['validation'],
                'publication_status': config.get('publication_status', 'prepared locally; not published; distribution review remains open'),
                'open_publication_checks': config['open_publication_checks'], 'recommended_release_checks': config.get('recommended_release_checks', []), 'packaging_provenance': config.get('packaging_provenance'), 'native_license_audit': config['native_license_audit'], 'assets': assets}
    (out / 'RELEASE_MANIFEST.json').write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + '\n')
    (out / 'SHA256SUMS').write_text(''.join(f'{digest(p)}  {p.name}\n' for p in sorted(out.iterdir())
                                                   if p.is_file() and p.name != 'SHA256SUMS'))
    print(f'Prepared {len(hashes)} source files, {len(assets)+1} release assets, and SHA256SUMS')
    print(f'Public source: public/plumOS-Y1; release assets: release/v{version}')
    print('Publication checks still open: ' + '; '.join(config['open_publication_checks']))


if __name__ == '__main__':
    main()
