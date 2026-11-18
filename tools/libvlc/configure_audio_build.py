#!/usr/bin/env python3
"""Apply the Y1 audio profile to the pinned upstream build recipes."""
from pathlib import Path
import sys
import subprocess
import tarfile
root = Path(sys.argv[1])
p = root / 'buildsystem/compile-libvlc.sh'
with tarfile.open(Path(__file__).resolve().parents[2] / 'third_party/libvlc/upstream/libvlcjni-2fca0d2b5eb1d94e2c41e23408e8087358cb983a.tar.gz') as archive:
    s = archive.extractfile('libvlcjni-2fca0d2b5eb1d94e2c41e23408e8087358cb983a/buildsystem/compile-libvlc.sh').read().decode()
start = s.index('VLC_CONTRIB_ARGS="\\\n')
end = s.index('\n"', start) + 2
packages = ['ffmpeg', 'flac', 'ogg', 'vorbis', 'opus', 'mpg123', 'soxr', 'speex', 'taglib', 'iconv', 'libxml2', 'modplug', 'gme', 'jpeg', 'matroska']
s = s[:start] + 'VLC_CONTRIB_ARGS="--disable-all --disable-sout --disable-net --disable-disc --enable-ad-clauses ' + ' '.join('--enable-' + n for n in packages) + '"' + s[end:]
for name in ['lua', 'bluray', 'zvbi', 'fluidlite', 'libass', 'gles2', 'sout', 'smb2', 'realrtsp', 'dvbpsi', 'swscale']:
    s = s.replace('--enable-' + name, '--disable-' + name)
s = s.replace('    --disable-smb2 \\\n', '    --disable-smb2 \\\n    --disable-dvdread --disable-dvdnav --disable-live555 \\\n    --disable-upnp --disable-nfs --disable-sftp --disable-vnc \\\n    --disable-libplacebo --disable-vpx --disable-dav1d --disable-aom \\\n    --disable-notify --disable-chromecast --disable-microdns \\\n')
start = s.index('avlc_find_modules()')
end = s.index('avlc_get_symbol()', start)
s = s[:start] + 'avlc_find_modules()\n{\n    python3 "$LIBVLCJNI_SRC_DIR/buildsystem/y1-filter-modules.py" "$1" "$blacklist_regexp"\n}\n\n' + s[end:]
s = s.replace('VLC_CONTRIB_ARGS="$VLC_CONTRIB_ARGS --enable-gettext"', ': # Native NLS is disabled; host gettext is sufficient')
s = s.replace('VLC_CONTRIB_ARGS="$VLC_CONTRIB_ARGS --disable-gettext"', ': # No gettext contrib in the audio profile')
s = s.replace('sh ../configure --host=', 'sh ../configure --cache-file=config.cache --host=')
s = s.replace('export PATH="$VLC_SRC_DIR/extras/tools/build/bin:$PATH"', 'VLC_CONTRIB="$VLC_SRC_DIR/contrib/$TARGET_TUPLE"\nif [ "$ANDROID_API" -lt 21 ]; then VLC_LDFLAGS="-landroid_support"; fi\nif [ "${Y1_RELINK_ONLY:-0}" != 1 ]; then\nexport PATH="$VLC_SRC_DIR/extras/tools/build/bin:$PATH"')
s = s.replace('REDEFINED_VLC_MODULES_DIR=${VLC_BUILD_DIR}/install/lib/vlc/plugins', 'fi # Full source build; below is also usable for incremental module relinking.\nREDEFINED_VLC_MODULES_DIR=${VLC_BUILD_DIR}/install/lib/vlc/plugins')
p.write_text(s)
import shutil
shutil.copyfile(Path(__file__).with_name('filter_modules.py'), root / 'buildsystem/y1-filter-modules.py')
p = root / 'vlc/contrib/src/ffmpeg/rules.mak'
s = subprocess.check_output(['git', '-C', str(root / 'vlc'), 'show', 'HEAD:contrib/src/ffmpeg/rules.mak'], text=True).replace('--enable-libopenjpeg', '--disable-libopenjpeg').replace('DEPS_ffmpeg += openjpeg', '# Y1 audio build: no JPEG2000 dependency')
# Preserve general audio formats and MP4/M4A demuxing; exclude video decoding and encoding.
decoders = ['aac', 'aac_fixed', 'aac_latm', 'ac3', 'ac3_fixed', 'eac3', 'alac', 'als', 'ape', 'atrac1', 'atrac3', 'atrac3al', 'atrac3p', 'atrac3pal', 'atrac9', 'cook', 'dca', 'dolby_e', 'dsd*', 'dst', 'flac', 'g723_1', 'g729', 'gsm', 'gsm_ms', 'ilbc', 'imc', 'interplayacm', 'mace3', 'mace6', 'metasound', 'mlp', 'mp1*', 'mp2*', 'mp3*', 'mpc7', 'mpc8', 'nellymoser', 'opus', 'pcm*', 'adpcm*', 'qcelp', 'qdm2', 'qdmc', 'ra_144', 'ra_288', 'ralf', 's302m', 'shorten', 'sipr', 'smackaud', 'sonic', 'speex', 'tak', 'truehd', 'truespeech', 'tta', 'twinvq', 'vmdaudio', 'vorbis', 'wavpack', 'wmalossless', 'wmapro', 'wmav1', 'wmav2', 'wmavoice', 'xma1', 'xma2']
s = s.replace('# Optional dependencies', 'FFMPEGCONF += --disable-gpl --disable-version3 --disable-nonfree --disable-decoders --enable-decoder=' + ','.join(decoders) + '\n\n# Optional dependencies')
p.write_text(s)

# libiconv has no pkg-config file; keep the charset converter in the final static link.
p = root / 'libvlc/jni/Android.mk'
s = p.read_text()
if '-liconv' not in s: p.write_text(s.replace('-ljpeg', '-ljpeg -liconv'))
