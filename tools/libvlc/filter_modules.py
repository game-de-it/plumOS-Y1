#!/usr/bin/env python3
"""Keep only modules carrying the pinned VLC LGPL metadata, record exclusions."""
from pathlib import Path
import json
import re
import sys
root = Path(sys.argv[1])
blacklist = re.compile(r'lib(' + sys.argv[2] + r')_plugin\.a')
# Keep audio filters/mixers/outputs and the stream buffering path, plus
# explicit local-file/audio codec/container modules. JNI/core APIs remain intact.
modules_source = Path(__file__).resolve().parents[1] / 'vlc/modules'
allowed = set()
for folder in ['audio_filter', 'audio_mixer', 'audio_output', 'stream_filter']:
    allowed.update(re.findall(r'lib([A-Za-z0-9_]+)_plugin', (modules_source/folder/'Makefile.am').read_text()))
allowed.update("android_display android_window volume_neon filesystem access_mmap access_imem avcodec adpcm aes3 g711 uleaddvaudio araw flac vorbis opus mpg123 lpcm spdif mpegaudio aiff asf au avformat es gme mkv mp4 mod mpc ogg rawaud smf tta voc wav xa playlist packetizer_copy packetizer_mpeg4audio packetizer_mpegaudio packetizer_flac packetizer_mlp packetizer_a52 packetizer_dts taglib android_logger logger_android float_mixer integer_mixer".split())
kept, excluded = [], []
for p in sorted(root.rglob('lib*plugin.a')):
    reason = None
    data = p.read_bytes()
    module_name = p.name[3:-len('_plugin.a')]
    if blacklist.fullmatch(p.name):
        reason = 'upstream module blacklist'
    elif module_name not in allowed:
        reason = 'not required by the local audio profile'
    elif b'Licensed under the terms of the GNU General Public License' in data:
        reason = 'GPL module metadata'
    elif b'Licensed under the terms of the GNU Lesser General Public License, version 2.1 or later.' not in data:
        raise SystemExit('Missing recognized license metadata: ' + p.name)
    if reason:
        excluded.append({'module': p.name, 'reason': reason})
    else:
        kept.append(p)
(root.parent / 'y1-module-selection.json').write_text(json.dumps({'kept': [p.name for p in kept], 'excluded': excluded}, indent=2) + '\n')
print(' '.join(str(p) for p in kept))
