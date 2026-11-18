#!/usr/bin/env python3
"""Compose only the Y1-used skin regions; retain no cursor/unused sprite sheets."""
import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'assets/skins/winamp'
SOURCE = ART / 'source'
NEAREST = Image.Resampling.NEAREST

# A new, editable pixel drawing. Dots are transparent, not sampled artwork.
PEACH = [
    '................',
    '.........oo.....',
    '.......oggo.....',
    '......oggo......',
    '...ooooboooo....',
    '..oppppospppo...',
    '.opphpppospppo..',
    '.opphpppospppo..',
    'opphpppposppppo.',
    'oppppppposppppo.',
    'oppppppposppppo.',
    '.oppppppospppo..',
    '.osppppposppso..',
    '..ospppppppso...',
    '...oosppssoo....',
    '.....ooooo......',
]
PALETTE = {'o': '#502C43', 'p': '#FFB298', 's': '#E77D91',
           'h': '#FFE0B6', 'g': '#86C67C', 'b': '#A47850'}


def peach():
    im = Image.new('RGBA', (16, 16))
    for y, row in enumerate(PEACH):
        for x, color in enumerate(row):
            if color != '.':
                im.putpixel((x, y), tuple(bytes.fromhex(PALETTE[color][1:])) + (255,))
    return im


def import_regions(folder):
    SOURCE.mkdir(parents=True, exist_ok=True)
    # Crop rectangles use half-open Pillow coordinates. Only these pixels ship.
    regions = {
        'main': ('MAIN.BMP', (0, 0, 275, 116)),
        'title': ('TITLEBAR.BMP', (27, 0, 302, 14)),
        'playlist-left': ('PLEDIT.BMP', (0, 0, 25, 20)),
        'playlist-label': ('PLEDIT.BMP', (26, 0, 126, 20)),
        'playlist-stripe': ('PLEDIT.BMP', (128, 0, 152, 20)),
        'playlist-buttons': ('PLEDIT.BMP', (154, 0, 178, 20)),
        'playlist-footer': ('PLEDIT.BMP', (0, 72, 275, 110)),
        'transport': ('CBUTTONS.BMP', (0, 0, 136, 18)),
    }
    provenance = {'input': 'PLUMOSAMP-pixel-redesign', 'regions': {},
                  'rights_status': 'Creator-provided PLUMOSAMP artwork, with original Winamp as visual reference. Referencing alone is not treated as copying, and changed-pixel counts are not a rights test. No third-party permission is inferred from the logo replacement.',
                  'excluded': ['original lightning logo', 'unused title rows and slogan',
                               'cursors', 'region documentation', 'unused sprite sheets']}
    for name, (filename, box) in regions.items():
        original = folder / filename
        region = Image.open(original).convert('RGBA').crop(box)
        # Bright cyan sprite-sheet guides are layout marks, not visible UI.
        pixels = region.load()
        for y in range(region.height):
            for x in range(region.width):
                r, g, b, a = pixels[x, y]
                if r < 30 and g > 150 and b > 200:
                    replacement = (22, 26, 45, a)
                    for distance in range(1, 9):
                        found = False
                        for nx in (x-distance, x+distance):
                            if 0 <= nx < region.width:
                                nr, ng, nb, na = pixels[nx, y]
                                if not (nr < 30 and ng > 150 and nb > 200):
                                    replacement = (nr, ng, nb, na)
                                    found = True
                                    break
                        if found:
                            break
                    pixels[x, y] = replacement
        if name == 'main':
            # Remove the old logo and its entire surrounding patch from the input.
            ImageDraw.Draw(region).rectangle((246, 86, 274, 115), fill=(0, 0, 0, 0))
        elif name == 'title':
            # Remove the original small lightning mark in the title's left corner.
            ImageDraw.Draw(region).rectangle((0, 0, 16, 13), fill=(0, 0, 0, 0))
        region.save(SOURCE / (name + '.png'))
        provenance['regions'][name] = {'file': filename, 'crop': box,
                                     'input_sha256': hashlib.sha256(original.read_bytes()).hexdigest()}
    (SOURCE / 'provenance.json').write_text(json.dumps(provenance, indent=2) + '\n')


def load(name):
    return Image.open(SOURCE / (name + '.png')).convert('RGBA')


def paste(im, region, box):
    x, y, w, h = box
    tile = region.resize((w, h), NEAREST)
    im.paste(tile, (x, y), tile)


def playlist(im, top=0):
    d = ImageDraw.Draw(im)
    d.rectangle((0, top, 479, 359), fill='#161A2D', outline='#353B56', width=2)
    paste(im, load('playlist-left'), (2, top + 2, 151, 22))
    paste(im, load('playlist-label'), (153, top + 2, 176, 22))
    paste(im, load('playlist-stripe'), (329, top + 2, 105, 22))
    paste(im, load('playlist-buttons'), (434, top + 2, 44, 22))
    d.rectangle((9, top + 24, 470, 295), fill='#22263E', outline='#555B75', width=2)
    d.rectangle((18, top + 33, 462, 294), fill='#000000')
    paste(im, load('playlist-footer'), (0, 294, 480, 66))
    # Explicit LCD openings match the runtime text clipping coordinates.
    d.rectangle((229, 308, 389, 324), fill='#000000')
    d.rectangle((329, 333, 389, 348), fill='#000000')


def build():
    icon = peach()
    icon.save(SOURCE / 'peach.png')
    menu = Image.new('RGB', (480, 360), '#161A2D')
    playlist(menu)
    # The first portion of the gold stripe becomes a peach badge.
    ImageDraw.Draw(menu).rectangle((4, 3, 27, 20), fill='#161A2D')
    menu.paste(icon, (8, 3), icon)
    menu.save(ART / 'menu.png')

    player = Image.new('RGB', (480, 360), '#161A2D')
    paste(player, load('main'), (0, 0, 480, 202))
    paste(player, load('title'), (2, 2, 476, 24))
    player.paste(icon, (10, 6), icon)
    large = icon.resize((26, 26), NEAREST)
    player.paste(large, (441, 164), large)
    paste(player, load('transport'), (28, 153, 237, 31))
    playlist(player, 204)
    player.save(ART / 'player.png')
    print('Generated menu.png, player.png and peach.png (480x360 skins; 16x16 icon).')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source-folder', type=Path, help='Import selected rectangles from the supplied skin once.')
    args = parser.parse_args()
    if args.source_folder:
        import_regions(args.source_folder)
    build()
