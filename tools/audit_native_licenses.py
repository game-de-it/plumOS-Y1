#!/usr/bin/env python3
"""Record native dependency evidence, not a legal-compliance certification."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import subprocess


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--nm', required=True, type=Path)
    p.add_argument('--readelf', required=True, type=Path)
    p.add_argument('--libraries', type=Path, default=Path('third_party/libvlc/extracted/jni/armeabi-v7a'))
    p.add_argument('--output', type=Path, default=Path('docs/licensing/native-binaries.json'))
    p.add_argument('--artifact', default='org.videolan.android:libvlc-all:3.7.1')
    p.add_argument('--module-selection', type=Path)
    args = p.parse_args()
    records = {}
    for name in ['libvlc.so', 'libvlcjni.so', 'libc++_shared.so']:
        file = args.libraries / name
        data = file.read_bytes()
        dynamic = subprocess.check_output([str(args.readelf), '-d', str(file)], text=True)
        symbols = subprocess.check_output([str(args.nm), '-D', str(file)], text=True)
        defined = set(re.findall(r'^\S+\s+[TW]\s+(\S+)$', symbols, re.M))
        records[name] = {
            'sha256': hashlib.sha256(data).hexdigest(), 'bytes': len(data),
            'needed_shared_libraries': re.findall(r'Shared library: \[([^\]]+)\]', dynamic),
            'defined_symbol_evidence': {
                key: sorted(n for n in defined if n.startswith(prefix))[:8]
                for key, prefix in [('libdvdcss', 'dvdcss_'), ('libdvdread', 'DVDOpen'), ('libdvdnav', 'dvdnav_'), ('libbluray', 'bd_open'), ('x264', 'x264_encoder_'), ('x265', 'x265_encoder_'), ('zvbi', 'vbi_')]},
            'embedded_configure_flags': [f for f in ['--enable-dvdread', '--enable-live555', '--enable-zvbi', '--disable-gpl', '--disable-gnuv3'] if f.encode() in data]
        }
    dvd = records['libvlc.so']['defined_symbol_evidence']
    result = {
        'artifact': args.artifact, 'abi': 'armeabi-v7a',
        'finding': ('GPL-covered implementation code is present; this binary is not cleared as LGPL-only' if dvd['libdvdcss'] or dvd['libdvdread'] else 'No dvdcss/dvdread definitions found by this limited probe; this alone does not clear all licensing conditions'),
        'confirmed_gpl_component_evidence': {'libdvdcss': bool(dvd['libdvdcss']), 'libdvdread': bool(dvd['libdvdread'])},
        'upstream_recipe_license_modes': {'g': 'default; GPL v3 recipe', 'l': 'LGPL v3 plus ad clauses recipe', 'a': 'LGPL v2.1 plus ad clauses recipe'},
        'limitations': 'Symbols prove implementation presence. They do not establish every component version, complete source correspondence, or the final combined-work license version.',
        'libraries': records
    }
    if args.module_selection:
        result['module_selection'] = json.loads(args.module_selection.read_text())
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print('Recorded native implementation evidence and shared-library inventory.')


if __name__ == '__main__':
    main()
