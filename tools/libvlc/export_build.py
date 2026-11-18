#!/usr/bin/env python3
"""Export rebuilt native binaries and auditable source/build materials."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import subprocess
import tarfile
import tempfile
import gzip
import difflib
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
PIN = '2fca0d2b5eb1d94e2c41e23408e8087358cb983a'
CORE = '4c608da16c33560821be7e5c1ad4e34bf5a65398'

def sha(path):
    digest = hashlib.sha256()
    with path.open('rb') as f:
        for block in iter(lambda: f.read(1024*1024), b''): digest.update(block)
    return digest.hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--container', default='plumos-y1-libvlc-audio')
    parser.add_argument('--binaries-only', action='store_true')
    args = parser.parse_args()
    c = args.container
    base = f'/work/libvlcjni-{PIN}'
    core = base+'/vlc'
    build = core+'/build-android-arm-linux-androideabi'
    output = ROOT/'third_party/libvlc/audio'
    (output/'extracted/jni/armeabi-v7a').mkdir(parents=True, exist_ok=True)
    def cp(src, dst):
        dst.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(['docker', 'cp', c+':'+src, str(dst)], check=True)
    for name in ['libvlc.so', 'libvlcjni.so', 'libc++_shared.so']:
        cp(base+'/libvlc/jni/libs/armeabi-v7a/'+name, output/'extracted/jni/armeabi-v7a'/name)
    shutil.copyfile(ROOT/'third_party/libvlc/extracted/classes.jar', output/'extracted/classes.jar')
    evidence = output/'evidence'; evidence.mkdir(exist_ok=True)
    for src, name in [(build+'/config.h', 'vlc-config.h'), (build+'/config.log', 'vlc-config.log'), (build+'/y1-module-selection.json', 'linked-modules.json'), (core+'/contrib/contrib-android-arm-linux-androideabi/Makefile', 'contrib-Makefile'), (core+'/contrib/contrib-android-arm-linux-androideabi/config.mak', 'contrib-config.mak'), (core+'/contrib/contrib-android-arm-linux-androideabi/ffmpeg/vlc_build/config.h', 'ffmpeg-config.h'), (core+'/contrib/contrib-android-arm-linux-androideabi/game-music-emu/vlc_build/CMakeCache.txt', 'gme-CMakeCache.txt')]:
        cp(src, evidence/name)
    packages = ['ffmpeg', 'flac', 'gme', 'gsm', 'iconv', 'jpeg', 'libxml2', 'modplug', 'mpg123', 'ogg', 'opus', 'soxr', 'speex', 'taglib', 'vorbis', 'zlib', 'matroska', 'ebml']
    records = {}
    for p in (output/'extracted/jni/armeabi-v7a').iterdir(): records[p.name] = {'sha256': sha(p), 'bytes': p.stat().st_size}
    ndk = ROOT/'build/libvlc-lgpl/android-ndk-r21e-linux-x86_64.zip'
    metadata = {'profile': 'plumOS-Y1 audio LGPL-2.1 profile', 'jni_commit': PIN, 'vlc_base_commit': CORE,
        'abi': 'armeabi-v7a', 'api': 17, 'ndk': '21.4.7075529 (r21e)', 'ndk_download_sha256': sha(ndk),
        'native_build': '--release --license a; no prebuilt contribs', 'contrib_packages': packages,
        'excluded_features': ['DVD/CSS/navigation', 'Blu-ray', 'video decoding in FFmpeg', 'stream output/encoding', 'SMB/NFS/UPnP discovery', 'Lua', 'subtitle rendering', 'GPL-labeled VLC modules', 'GPL/GNUv3/nonfree FFmpeg dependencies'],
        'compatibility_modules': ['android_display', 'android_window (option definitions required by the unmodified Java bindings)'],
        'retained_features': ['audio file demux/decode', 'Android AudioTrack/OpenSL ES output', 'equalizer', 'software volume', 'soxr resampler'],
        'java_bindings': 'Unmodified classes.jar from libvlc-all 3.7.1; matching Java sources included in source material',
        'libraries': records, 'validation': 'See the accompanying device regression report; build success alone does not prove audible output.'}
    (output/'BUILD.json').write_text(json.dumps(metadata, indent=2)+'\n')
    if args.binaries_only: return
    # Build a source bundle without host paths, private media, or signing material.
    version = ET.parse(ROOT/'AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName']
    release = ROOT/'release'/f'v{version}'; release.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='y1-native-source-') as temp:
        stage = Path(temp)/'libvlc-audio-source'; stage.mkdir()
        for relative in ['tools/libvlc', 'third_party/libvlc/upstream', 'third_party/libvlc/licenses']:
            shutil.copytree(ROOT/relative, stage/relative, ignore=shutil.ignore_patterns('__pycache__', 'test'))
        shutil.copyfile(ROOT/'third_party/libvlc/COPYING.LIB', stage/'COPYING.LIB')
        shutil.copyfile(output/'BUILD.json', stage/'BUILD.json')
        shutil.copytree(evidence, stage/'evidence')
        cp(core+'/contrib/tarballs', stage/'contrib-tarballs')
        allowed_tarballs = set()
        for package in packages:
            checksums = stage/'evidence/contrib-checksums'/(package+'.sha512')
            cp(core+'/contrib/src/'+package+'/SHA512SUMS', checksums)
            for line in checksums.read_text().splitlines():
                parts = line.split()
                if len(parts) >= 2: allowed_tarballs.add(parts[-1].lstrip('*'))
        for entry in (stage/'contrib-tarballs').iterdir():
            if entry.name not in allowed_tarballs:
                if entry.is_dir(): shutil.rmtree(entry)
                else: entry.unlink()

        # Include tools sources used to bootstrap the native build.
        tools = stage/'extras-tools-tarballs'; tools.mkdir()
        for name in ['protobuf-3.4.1.tar.gz', 'ninja-1.11.1.tar.gz']:
            cp(core+'/extras/tools/'+name, tools/name)
        # Preserve dependency notices as separate files; GPL build tools may coexist in sources
        # while only the audited library portions enter the APK.
        notices = stage/'dependency-notices'; notices.mkdir()
        base_contrib = core+'/contrib/contrib-android-arm-linux-androideabi'
        for package in packages:
            source_dir = {'gme': 'game-music-emu', 'modplug': 'libmodplug', 'ogg': 'libogg', 'vorbis': 'libvorbis'}.get(package, package)
            process = subprocess.Popen(['docker', 'cp', c+':'+base_contrib+'/'+source_dir, '-'], stdout=subprocess.PIPE)
            with tarfile.open(fileobj=process.stdout, mode='r|') as contents:
                for member in contents:
                    rel = Path(member.name)
                    name = rel.name.upper()
                    if member.isfile() and len(rel.parts) <= 4 and name.startswith(('COPYING', 'LICENSE', 'LICENCE', 'COPYRIGHT', 'AUTHORS', 'NOTICE')):
                        if '..' in rel.parts or rel.is_absolute(): raise SystemExit('Unsafe archive notice path')
                        destination = notices/rel
                        destination.parent.mkdir(parents=True, exist_ok=True)
                        destination.write_bytes(contents.extractfile(member).read())
            if process.wait() != 0: raise SystemExit('Cannot collect dependency notices: '+package)
        shutil.copytree(notices, output/'dependency-notices', dirs_exist_ok=True)
        # Every contrib patch is in the pinned VLC archive; this diff records local changes.
        final_recipe = stage/'evidence/ffmpeg-rules.mak'
        cp(core+'/contrib/src/ffmpeg/rules.mak', final_recipe)
        with tarfile.open(ROOT/f'third_party/libvlc/upstream/vlc-{CORE}.tar.gz') as original:
            baseline = original.extractfile(f'vlc-{CORE}/contrib/src/ffmpeg/rules.mak').read().decode()
        diff = ''.join(difflib.unified_diff(baseline.splitlines(True), final_recipe.read_text().splitlines(True), fromfile='a/contrib/src/ffmpeg/rules.mak', tofile='b/contrib/src/ffmpeg/rules.mak'))
        (stage/'audio-ffmpeg.patch').write_text(diff)
        (stage/'README.md').write_text('''# LibVLC audio source / ソース\n\nPinned upstream archives, their contrib patches, downloaded dependency sources, local profile scripts, license texts, and configuration evidence are included. Native code was built for ARMv7 Android API 17 with NDK r21e. Java bindings are the unmodified Maven 3.7.1 classes; their sources JAR is included.\n\nUse tools/libvlc/Dockerfile and build-in-container.sh in the plumOS-Y1 source tree. Download the official android-ndk-r21e-linux-x86_64.zip into build/libvlc-lgpl/ and verify its SHA-256 against BUILD.json. The build scripts use a dedicated /work volume and /workspace source mount. The first build applies the pinned JNI patch series to the pinned VLC archive, then applies configure_audio_build.py. Do not use --with-prebuilt-contribs. The source tarballs here can populate vlc/contrib/tarballs; extras-tools-tarballs contains the bootstrap tools.\n\nソース・依存物・パッチ・構成を同梱。NDKはBUILD.jsonのハッシュと照合。専用ビルド領域で--license aを使用し、既成contribを使わず再構築します。独自署名APKとFWを一貫した鍵で作成すれば改造版を導入できます。配布者の秘密鍵は含みません。\n''')
        manifest = {str(p.relative_to(stage)): sha(p) for p in sorted(stage.rglob('*')) if p.is_file()}
        (stage/'SOURCE_SHA256.json').write_text(json.dumps(manifest, indent=2)+'\n')
        target = release/f'plumOS-Y1-v{version}-libvlc-source.tar.gz'
        with target.open('wb') as f, gzip.GzipFile(filename='', fileobj=f, mode='wb', mtime=0) as gz, tarfile.open(fileobj=gz, mode='w') as tar:
            for p in sorted(stage.rglob('*')):
                if p.is_file():
                    info = tar.gettarinfo(str(p), 'libvlc-audio-source/'+str(p.relative_to(stage)))
                    info.uid = info.gid = 0; info.uname = info.gname = ''; info.mtime = 0
                    with p.open('rb') as inp: tar.addfile(info, inp)
        print(target)

if __name__ == '__main__': main()
