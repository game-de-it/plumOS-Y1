#!/usr/bin/env python3
"""Pack fixed-cell U-M+ BDF glyphs for the Y1 Canvas bitmap renderer."""
from pathlib import Path
import struct
import sys


def pack(source: Path, destination: Path):
    lines = source.read_text(encoding="ascii").splitlines()
    cell_width = int(next(x for x in lines if x.startswith("FONTBOUNDINGBOX ")).split()[1])
    cell_height = 13
    ascent = int(next(x for x in lines if x.startswith("FONT_ASCENT ")).split()[1])
    glyphs = []
    i = 0
    while i < len(lines):
        if not lines[i].startswith("STARTCHAR "):
            i += 1
            continue
        codepoint = -1
        advance = cell_width
        box = None
        bits = []
        i += 1
        while i < len(lines) and lines[i] != "ENDCHAR":
            line = lines[i]
            if line.startswith("ENCODING "):
                codepoint = int(line.split()[1])
            elif line.startswith("DWIDTH "):
                advance = int(line.split()[1])
            elif line.startswith("BBX "):
                box = tuple(map(int, line.split()[1:]))
            elif line == "BITMAP":
                i += 1
                while i < len(lines) and lines[i] != "ENDCHAR":
                    bits.append(int(lines[i], 16))
                    i += 1
                continue
            i += 1
        i += 1
        if codepoint < 0 or box is None:
            continue
        width, height, xoff, yoff = box
        top = ascent - (yoff + height)
        rows = [0] * cell_height
        for y, bitrow in enumerate(bits):
            dy = top + y
            if not 0 <= dy < cell_height:
                continue
            for x in range(width):
                dx = xoff + x
                if 0 <= dx < cell_width and bitrow & (1 << (len(bits[y].to_bytes((width + 7) // 8, "big")) * 8 - 1 - x)):
                    rows[dy] |= 1 << (15 - dx)
        glyphs.append((codepoint, advance, rows))
    with destination.open("wb") as out:
        out.write(b"BDF1")
        out.write(struct.pack(">BBI", cell_width, cell_height, len(glyphs)))
        for codepoint, advance, rows in glyphs:
            out.write(struct.pack(">IB", codepoint, advance))
            for row in rows:
                out.write(struct.pack(">H", row))
    print(f"{destination}: {len(glyphs)} glyphs, {destination.stat().st_size} bytes")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("usage: pack_bdf.py SOURCE.bdf DESTINATION.bin")
    pack(Path(sys.argv[1]), Path(sys.argv[2]))
