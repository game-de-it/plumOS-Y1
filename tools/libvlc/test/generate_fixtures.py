#!/usr/bin/env python3
"""Create low-level synthetic fixtures without redistributing music."""
from pathlib import Path
import argparse
import subprocess
p = argparse.ArgumentParser()
p.add_argument('output', type=Path)
a = p.parse_args(); a.output.mkdir(parents=True, exist_ok=True)
def ffmpeg(args): subprocess.run(['ffmpeg', '-hide_banner', '-loglevel', 'error', '-y'] + args, check=True)
wave = a.output/'tone.wav'
ffmpeg(['-f', 'lavfi', '-i', 'sine=frequency=440:duration=9:sample_rate=44100', '-af', 'volume=0.08', '-ac', '2', '-c:a', 'pcm_s16le', str(wave)])
for ext, codec in [('mp3', 'libmp3lame'), ('m4a', 'aac'), ('flac', 'flac'), ('ogg', 'libvorbis'), ('opus', 'libopus'), ('aiff', 'pcm_s16be'), ('wma', 'wmav2'), ('wv', 'wavpack'), ('mka', 'flac'), ('caf', 'pcm_s24le')]:
    ffmpeg(['-i', str(wave), '-c:a', codec, str(a.output/('tone.'+ext))])
ffmpeg(['-i', str(wave), '-c:a', 'alac', str(a.output/'tone-alac.m4a')])
ffmpeg(['-i', str(wave), '-c:a', 'aac', '-f', 'adts', str(a.output/'tone.aac')])
